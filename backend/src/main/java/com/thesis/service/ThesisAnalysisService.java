package com.thesis.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.thesis.dto.ThesisAnalysisResult;
import com.thesis.dto.ThesisAnalysisResult.*;
import com.thesis.entity.Thesis;
import com.thesis.entity.ThesisVersion;
import com.thesis.exception.BadRequestException;
import com.thesis.exception.ForbiddenException;
import com.thesis.exception.NotFoundException;
import com.thesis.mapper.ThesisMapper;
import com.thesis.mapper.ThesisVersionMapper;
import com.thesis.util.ThesisAnalysisUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * 论文分析服务
 * 协调 ThesisAnalysisUtil（文档预处理）和 ReferenceVerifyService（文献验证），
 * 对指定论文的最新版本执行完整分析。
 */
@Service
public class ThesisAnalysisService {

    private static final Logger log = LoggerFactory.getLogger(ThesisAnalysisService.class);

    @Autowired
    private ThesisMapper thesisMapper;

    @Autowired
    private ThesisVersionMapper thesisVersionMapper;

    @Autowired
    private ThesisAnalysisUtil analysisUtil;

    @Autowired
    private ReferenceVerifyService referenceVerifyService;

    /**
     * 分析指定论文的最新版本
     *
     * @param thesisId      论文ID
     * @param currentUserId 当前用户ID
     * @param role          当前用户角色
     * @return 完整分析结果
     */
    public ThesisAnalysisResult analyzeThesis(Long thesisId, Long currentUserId, String role) {
        // 1. 权限校验
        if (currentUserId == null || role == null) {
            throw new ForbiddenException("用户未认证");
        }

        Thesis thesis = thesisMapper.selectById(thesisId);
        if (thesis == null) {
            throw new NotFoundException("论文不存在");
        }

        // STUDENT 只能分析自己的论文
        if ("STUDENT".equals(role)) {
            if (!thesis.getStudentId().equals(currentUserId)) {
                throw new ForbiddenException("无权分析他人论文");
            }
        } else if (!"TEACHER".equals(role) && !"ADMIN".equals(role)) {
            throw new ForbiddenException("角色无权限执行此操作");
        }

        // 2. 获取最新版本
        ThesisVersion latestVersion = getLatestVersion(thesisId);
        if (latestVersion == null) {
            throw new BadRequestException("该论文尚无版本，无法分析");
        }

        // 3. 检查文件存在
        File file = new File(latestVersion.getFilePath());
        if (!file.exists()) {
            throw new BadRequestException("版本文件不存在: " + latestVersion.getFilePath());
        }

        if (!file.getName().toLowerCase().endsWith(".docx")) {
            throw new BadRequestException("仅支持 .docx 格式的论文分析");
        }

        // 4. 执行分析
        String filePath = file.getAbsolutePath();
        return executeAnalysis(filePath, latestVersion);
    }

    /**
     * 执行完整的论文分析流程
     */
    private ThesisAnalysisResult executeAnalysis(String filePath, ThesisVersion version) {
        ThesisAnalysisResult result = new ThesisAnalysisResult();
        result.setVersionId(version.getId());
        result.setVersionNum(version.getVersionNum());

        try {
            // --- 摘要分析 ---
            log.info("开始摘要分析...");
            String abstractText = analysisUtil.extractAbstract(filePath);
            result.setAbstractText(abstractText);
            result.setAbstractAnalysis(analysisUtil.analyzeAbstract(abstractText));
            // llmAnalysis 预留为 null

            // --- 目录分析 ---
            log.info("开始目录分析...");
            List<ChapterInfo> chapters = analysisUtil.extractTableOfContents(filePath);
            result.setChapters(chapters);
            result.setProportionAnalysis(
                    analysisUtil.analyzeChapterProportions(chapters, filePath));

            // --- 参考文献提取 ---
            log.info("开始参考文献提取...");
            List<ReferenceInfo> refs = analysisUtil.extractReferences(filePath);
            result.setReferences(refs);

            // --- 引用检测 ---
            log.info("开始引用检测...");
            result.setCitationIssues(
                    analysisUtil.detectCitationIssues(filePath, refs));

            // --- 参考文献验证（百度学术，耗时较长） ---
            log.info("开始参考文献验证（共{}条）...", refs.size());
            referenceVerifyService.verifyReferences(refs);

            log.info("论文分析完成");
        } catch (IOException e) {
            throw new RuntimeException("论文分析失败: " + e.getMessage(), e);
        }

        return result;
    }

    /**
     * 获取论文的最新版本
     */
    private ThesisVersion getLatestVersion(Long thesisId) {
        LambdaQueryWrapper<ThesisVersion> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ThesisVersion::getThesisId, thesisId)
                .orderByDesc(ThesisVersion::getVersionNum)
                .last("LIMIT 1");
        return thesisVersionMapper.selectOne(wrapper);
    }
}

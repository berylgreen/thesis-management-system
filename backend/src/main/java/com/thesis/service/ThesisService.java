package com.thesis.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.thesis.dto.ThesisDTO;
import com.thesis.entity.Thesis;
import com.thesis.entity.ThesisVersion;
import com.thesis.entity.User;
import com.thesis.exception.BadRequestException;
import com.thesis.exception.ForbiddenException;
import com.thesis.exception.NotFoundException;
import com.thesis.mapper.ThesisMapper;
import com.thesis.mapper.ThesisVersionMapper;
import com.thesis.mapper.UserMapper;
import com.thesis.util.DiffUtil;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ThesisService {

    @Autowired
    private ThesisMapper thesisMapper;

    @Autowired
    private ThesisVersionMapper thesisVersionMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private DiffUtil diffUtil;

    @Value("${file.upload-path}")
    private String uploadPath;

    public Thesis createThesis(Long studentId, String title) {
        Thesis thesis = new Thesis();
        thesis.setStudentId(studentId);
        thesis.setTitle(title);
        thesis.setStatus("DRAFT");
        thesis.setCurrentVersion(0);
        thesisMapper.insert(thesis);
        return thesis;
    }

    @Transactional
    public ThesisVersion uploadVersion(Long thesisId, MultipartFile file, String remark) throws IOException {
        Thesis thesis = thesisMapper.selectById(thesisId);
        if (thesis == null) {
            throw new RuntimeException("论文不存在");
        }

        String fileName = UUID.randomUUID() + "_" + file.getOriginalFilename();
        Path filePath = Paths.get(uploadPath, thesisId.toString(), fileName);
        Files.createDirectories(filePath.getParent());
        file.transferTo(filePath.toFile());

        String contentHash = calculateHash(file.getBytes());

        int newVersionNum = thesis.getCurrentVersion() + 1;
        ThesisVersion version = new ThesisVersion();
        version.setThesisId(thesisId);
        version.setVersionNum(newVersionNum);
        version.setFilePath(filePath.toString());
        version.setContentHash(contentHash);
        version.setFileSize(file.getSize());
        version.setRemark(remark);
        thesisVersionMapper.insert(version);

        thesis.setCurrentVersion(newVersionNum);
        thesis.setStatus("SUBMITTED");
        thesisMapper.updateById(thesis);

        return version;
    }

    public List<Thesis> getStudentTheses(Long studentId) {
        LambdaQueryWrapper<Thesis> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Thesis::getStudentId, studentId);
        return thesisMapper.selectList(wrapper);
    }

    public List<Thesis> getAllTheses() {
        LambdaQueryWrapper<Thesis> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(Thesis::getUpdatedAt);
        return thesisMapper.selectList(wrapper);
    }

    public List<ThesisDTO> getAllThesesWithStudent() {
        List<Thesis> theses = getAllTheses();
        return theses.stream().map(thesis -> {
            ThesisDTO dto = new ThesisDTO();
            BeanUtils.copyProperties(thesis, dto);

            User student = userMapper.selectById(thesis.getStudentId());
            if (student != null) {
                dto.setStudentName(student.getRealName());
                dto.setStudentUsername(student.getUsername());
            }
            return dto;
        }).collect(Collectors.toList());
    }

    public List<ThesisVersion> getThesisVersions(Long thesisId) {
        LambdaQueryWrapper<ThesisVersion> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ThesisVersion::getThesisId, thesisId).orderByDesc(ThesisVersion::getVersionNum);
        return thesisVersionMapper.selectList(wrapper);
    }

    public File getVersionFile(Long versionId) {
        ThesisVersion version = thesisVersionMapper.selectById(versionId);
        if (version == null) {
            throw new NotFoundException("版本不存在");
        }
        return new File(version.getFilePath());
    }

    /**
     * 带权限校验的版本对比
     * @param version1Id 版本1 ID
     * @param version2Id 版本2 ID
     * @param currentUserId 当前用户ID
     * @param role 当前用户角色
     * @return 完整对比结果（包含原始文档和修订文档内容）
     */
    public DiffUtil.FullDiffResult compareVersionsWithAuth(
            Long version1Id, Long version2Id, Long currentUserId, String role) {

        // 0. 认证校验
        if (currentUserId == null || role == null) {
            throw new ForbiddenException("用户未认证");
        }

        // 1. 参数校验
        if (version1Id == null || version2Id == null) {
            throw new BadRequestException("版本ID不能为空");
        }
        if (version1Id.equals(version2Id)) {
            throw new BadRequestException("不能对比同一版本");
        }

        // 2. 加载版本
        ThesisVersion v1 = thesisVersionMapper.selectById(version1Id);
        ThesisVersion v2 = thesisVersionMapper.selectById(version2Id);
        if (v1 == null || v2 == null) {
            throw new NotFoundException("版本不存在");
        }

        // 3. 跨论文校验
        if (!v1.getThesisId().equals(v2.getThesisId())) {
            throw new BadRequestException("不能对比不同论文的版本");
        }

        // 4. 加载论文
        Thesis thesis = thesisMapper.selectById(v1.getThesisId());
        if (thesis == null) {
            throw new NotFoundException("论文不存在");
        }

        // 5. 权限校验（仅允许STUDENT和TEACHER角色）
        if ("STUDENT".equals(role)) {
            if (!thesis.getStudentId().equals(currentUserId)) {
                throw new ForbiddenException("无权访问他人论文");
            }
        } else if ("TEACHER".equals(role)) {
            // 教师可以访问所有论文
        } else {
            // 其他角色一律拒绝
            throw new ForbiddenException("角色无权限执行此操作");
        }

        // 6. contentHash 短路优化
        if (v1.getContentHash() != null && v1.getContentHash().equals(v2.getContentHash())) {
            // 无差异，返回空结果
            DiffUtil.FullDiffResult emptyResult = new DiffUtil.FullDiffResult();
            emptyResult.setDiffs(Collections.emptyList());
            emptyResult.setOriginalLines(Collections.emptyList());
            emptyResult.setRevisedLines(Collections.emptyList());
            return emptyResult;
        }

        // 7. 执行完整文件对比（返回包含完整文档内容的结果）
        try {
            File f1 = new File(v1.getFilePath());
            File f2 = new File(v2.getFilePath());
            return diffUtil.compareFilesWithFullContent(f1.getAbsolutePath(), f2.getAbsolutePath());
        } catch (IOException e) {
            throw new RuntimeException("文件对比失败", e);
        }
    }

    private String calculateHash(byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data);
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            throw new RuntimeException("计算哈希失败", e);
        }
    }
}

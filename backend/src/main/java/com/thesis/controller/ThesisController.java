package com.thesis.controller;

import com.thesis.dto.ThesisAnalysisResult;
import com.thesis.dto.ThesisDTO;
import com.thesis.entity.Thesis;
import com.thesis.entity.ThesisVersion;
import com.thesis.entity.User;
import com.thesis.mapper.UserMapper;
import com.thesis.service.ThesisAnalysisService;
import com.thesis.service.ThesisService;
import com.thesis.util.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.List;

@RestController
@RequestMapping("/api/thesis")
public class ThesisController {

    @Autowired
    private ThesisService thesisService;

    @Autowired
    private ThesisAnalysisService thesisAnalysisService;

    @Autowired
    private UserMapper userMapper;

    @PostMapping("/create")
    public Result<Thesis> createThesis(@RequestParam String title, Authentication auth) {
        try {
            Long studentId = (Long) auth.getPrincipal();
            Thesis thesis = thesisService.createThesis(studentId, title);
            return Result.success("论文创建成功", thesis);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/{thesisId}")
    public Result<Thesis> getThesis(@PathVariable Long thesisId) {
        try {
            Thesis thesis = thesisService.getThesisById(thesisId);
            if (thesis == null) {
                return Result.error("论文不存在");
            }
            return Result.success(thesis);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/{thesisId}/upload")
    public Result<ThesisVersion> uploadVersion(
            @PathVariable Long thesisId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) String remark,
            Authentication auth) {
        try {
            ThesisVersion version = thesisService.uploadVersion(thesisId, file, remark);
            return Result.success("版本上传成功", version);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/my")
    public Result<List<?>> getMyTheses(Authentication auth) {
        try {
            Long userId = (Long) auth.getPrincipal();
            User user = userMapper.selectById(userId);

            if (user == null) {
                return Result.error("用户不存在");
            }

            // 教师和管理员可以看到所有论文(包含学生信息)，学生只能看到自己的论文
            if ("TEACHER".equals(user.getRole()) || "ADMIN".equals(user.getRole())) {
                List<ThesisDTO> theses = thesisService.getAllThesesWithStudent();
                return Result.success(theses);
            } else {
                List<Thesis> theses = thesisService.getStudentTheses(userId);
                return Result.success(theses);
            }
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/{thesisId}/versions")
    public Result<List<ThesisVersion>> getVersions(@PathVariable Long thesisId) {
        try {
            List<ThesisVersion> versions = thesisService.getThesisVersions(thesisId);
            return Result.success(versions);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/version/{versionId}/download")
    public ResponseEntity<Resource> downloadVersion(@PathVariable Long versionId) {
        try {
            File file = thesisService.getVersionFile(versionId);
            Resource resource = new FileSystemResource(file);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file.getName() + "\"")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(resource);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @Autowired
    private com.thesis.service.FileInitService fileInitService;

    /**
     * 强制同步文件系统到数据库（仅教师/管理员可用）
     */
    @PostMapping("/admin/force-sync")
    public Result<java.util.Map<String, Integer>> forceSyncFiles(Authentication auth) {
        try {
            Long userId = (Long) auth.getPrincipal();
            User user = userMapper.selectById(userId);

            if (user == null) {
                return Result.error("用户不存在");
            }

            // 权限检查
            if (!"TEACHER".equals(user.getRole()) && !"ADMIN".equals(user.getRole())) {
                return Result.error("无权限执行此操作");
            }

            java.util.Map<String, Integer> result = fileInitService.forceSyncFromFileSystem();
            return Result.success("强制同步完成", result);
        } catch (Exception e) {
            return Result.error("同步失败: " + e.getMessage());
        }
    }

    /**
     * 分析论文（自动取最新版本）
     * 执行摘要提取、目录分析、参考文献验证、引用检测
     */
    @GetMapping("/{thesisId}/analyze")
    public Result<ThesisAnalysisResult> analyzeThesis(
            @PathVariable Long thesisId, Authentication auth) {
        try {
            Long userId = (Long) auth.getPrincipal();
            User user = userMapper.selectById(userId);
            if (user == null) {
                return Result.error("用户不存在");
            }

            ThesisAnalysisResult result = thesisAnalysisService.analyzeThesis(
                    thesisId, userId, user.getRole());
            return Result.success("分析完成", result);
        } catch (Exception e) {
            return Result.error("分析失败: " + e.getMessage());
        }
    }
}


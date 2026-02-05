package com.thesis.controller;

import com.thesis.service.ThesisService;
import com.thesis.util.DiffUtil;
import com.thesis.util.Result;
import com.thesis.util.SecurityUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/diff")
public class DiffController {

    @Autowired
    private ThesisService thesisService;

    @GetMapping("/compare")
    public Result<DiffUtil.FullDiffResult> compareVersions(
            @RequestParam Long version1Id,
            @RequestParam Long version2Id) {

        // 获取当前用户信息
        Long currentUserId = SecurityUtil.getCurrentUserId();
        String role = SecurityUtil.getCurrentUserRole();

        // 调用服务层（包含权限校验）
        DiffUtil.FullDiffResult diff = thesisService.compareVersionsWithAuth(
                version1Id, version2Id, currentUserId, role);

        return Result.success(diff);
    }

    /**
     * 获取单个版本的文档内容（用于单版本查看模式）
     */
    @GetMapping("/content")
    public Result<ThesisService.VersionContentResult> getVersionContent(
            @RequestParam Long versionId) {

        // 获取当前用户信息
        Long currentUserId = SecurityUtil.getCurrentUserId();
        String role = SecurityUtil.getCurrentUserRole();

        // 调用服务层（包含权限校验）
        ThesisService.VersionContentResult result = thesisService.getVersionContentWithAuth(
                versionId, currentUserId, role);

        return Result.success(result);
    }
}


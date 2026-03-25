package com.thesis.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.thesis.dto.StudentDTO;
import com.thesis.entity.User;
import com.thesis.mapper.UserMapper;
import com.thesis.service.StudentService;
import com.thesis.util.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * 学生管理控制器（仅 ADMIN / TEACHER 可访问）
 */
@RestController
@RequestMapping("/api/students")
public class StudentController {

    @Autowired
    private StudentService studentService;

    @Autowired
    private UserMapper userMapper;

    /**
     * 权限校验：当前用户须为 ADMIN 或 TEACHER
     */
    private void checkPermission(Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        User currentUser = userMapper.selectById(userId);
        if (currentUser == null) {
            throw new RuntimeException("用户不存在");
        }
        if (!"ADMIN".equals(currentUser.getRole()) && !"TEACHER".equals(currentUser.getRole())) {
            throw new RuntimeException("无权限访问");
        }
    }

    /**
     * 分页查询学生列表
     */
    @GetMapping
    public Result<IPage<User>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            Authentication auth) {
        try {
            checkPermission(auth);
            IPage<User> result = studentService.listStudents(page, size, keyword);
            return Result.success(result);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 获取单个学生
     */
    @GetMapping("/{id}")
    public Result<User> get(@PathVariable Long id, Authentication auth) {
        try {
            checkPermission(auth);
            User student = studentService.getStudent(id);
            return Result.success(student);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 新增学生
     */
    @PostMapping
    public Result<User> create(@RequestBody StudentDTO.CreateRequest request, Authentication auth) {
        try {
            checkPermission(auth);
            User student = studentService.createStudent(request);
            return Result.success("学生创建成功", student);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 编辑学生信息
     */
    @PutMapping("/{id}")
    public Result<User> update(@PathVariable Long id, @RequestBody StudentDTO.UpdateRequest request, Authentication auth) {
        try {
            checkPermission(auth);
            User student = studentService.updateStudent(id, request);
            return Result.success("学生信息更新成功", student);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 删除学生
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id, Authentication auth) {
        try {
            checkPermission(auth);
            studentService.deleteStudent(id);
            return Result.success("学生删除成功", null);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 重置密码
     */
    @PutMapping("/{id}/reset-password")
    public Result<Void> resetPassword(@PathVariable Long id, @RequestBody StudentDTO.ResetPasswordRequest request, Authentication auth) {
        try {
            checkPermission(auth);
            studentService.resetPassword(id, request.getNewPassword());
            return Result.success("密码重置成功", null);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 获取学生的所有论文及版本
     */
    @GetMapping("/{id}/theses")
    public Result<java.util.List<java.util.Map<String, Object>>> getStudentTheses(@PathVariable Long id, Authentication auth) {
        try {
            checkPermission(auth);
            return Result.success(studentService.getStudentThesesWithVersions(id));
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 一键统一所有学生论文题目：遍历所有学生，将预设的论文题目同步到论文记录
     */
    @PutMapping("/sync-all-titles")
    public Result<java.util.Map<String, Object>> syncAllTitles(Authentication auth) {
        try {
            checkPermission(auth);
            return Result.success("论文题目统一完成", studentService.syncAllThesisTitles());
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 批量重命名论文版本文件为规范格式
     * 请求体: { studentId, versionIds: [1,2,3] }
     */
    @PostMapping("/batch-rename")
    public Result<java.util.Map<String, Object>> batchRename(@RequestBody StudentDTO.BatchRenameRequest request, Authentication auth) {
        try {
            checkPermission(auth);
            return Result.success("批量重命名完成", studentService.batchRenameFiles(request));
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
}

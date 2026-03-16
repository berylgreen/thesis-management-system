package com.thesis.dto;

import lombok.Data;

/**
 * 学生管理 DTO
 */
@Data
public class StudentDTO {
    private Long id;
    private String username;    // 学号
    private String realName;    // 姓名
    private String email;

    /**
     * 新增学生请求
     */
    @Data
    public static class CreateRequest {
        private String username;    // 学号
        private String password;
        private String realName;
        private String email;
    }

    /**
     * 编辑学生请求
     */
    @Data
    public static class UpdateRequest {
        private String realName;
        private String email;
        private String thesisTitle;
    }

    /**
     * 重置密码请求
     */
    @Data
    public static class ResetPasswordRequest {
        private String newPassword;
    }

    /**
     * 批量重命名请求
     */
    @Data
    public static class BatchRenameRequest {
        private Long studentId;
        private java.util.List<Long> versionIds;
    }
}

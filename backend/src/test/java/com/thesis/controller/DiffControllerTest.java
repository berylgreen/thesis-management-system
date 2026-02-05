package com.thesis.controller;

import com.thesis.exception.BadRequestException;
import com.thesis.exception.ForbiddenException;
import com.thesis.exception.GlobalExceptionHandler;
import com.thesis.exception.NotFoundException;
import com.thesis.service.ThesisService;
import com.thesis.util.DiffUtil;
import com.thesis.util.SecurityUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 集成测试: DiffController
 * 覆盖场景: HTTP 状态码、权限校验、异常处理、响应体格式
 */
@WebMvcTest(
    controllers = DiffController.class,
    excludeAutoConfiguration = {
        com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration.class
    }
)
@AutoConfigureMockMvc
@Import({GlobalExceptionHandler.class, TestSecurityConfig.class})
@DisplayName("DiffController - 版本对比 API 集成测试")
class DiffControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ThesisService thesisService;

    private MockedStatic<SecurityUtil> securityUtilMock;

    @BeforeEach
    void setUp() {
        securityUtilMock = mockStatic(SecurityUtil.class);
    }

    @AfterEach
    void tearDown() {
        securityUtilMock.close();
        reset(thesisService);
    }

    // ==================== 成功场景测试 ====================

    @Test
    @WithMockUser
    @DisplayName("✅ 学生对比自己论文 应返回 200 和差异列表")
    void test_should_return_200_when_student_compare_own_thesis() throws Exception {
        // Arrange
        securityUtilMock.when(SecurityUtil::getCurrentUserId).thenReturn(10L);
        securityUtilMock.when(SecurityUtil::getCurrentUserRole).thenReturn("STUDENT");

        DiffUtil.DiffResult insertDiff = new DiffUtil.DiffResult();
        insertDiff.setType("INSERT");
        insertDiff.setOriginalPosition(0);
        insertDiff.setRevisedPosition(0);
        insertDiff.setOriginalLines(Collections.emptyList());
        insertDiff.setRevisedLines(List.of("新行"));

        DiffUtil.DiffResult deleteDiff = new DiffUtil.DiffResult();
        deleteDiff.setType("DELETE");
        deleteDiff.setOriginalPosition(5);
        deleteDiff.setRevisedPosition(5);
        deleteDiff.setOriginalLines(List.of("旧行"));
        deleteDiff.setRevisedLines(Collections.emptyList());

        DiffUtil.FullDiffResult fullResult = new DiffUtil.FullDiffResult();
        fullResult.setDiffs(List.of(insertDiff, deleteDiff));
        fullResult.setOriginalLines(List.of("旧行"));
        fullResult.setRevisedLines(List.of("新行"));
        when(thesisService.compareVersionsWithAuth(1L, 2L, 10L, "STUDENT"))
                .thenReturn(fullResult);

        // Act & Assert
        mockMvc.perform(get("/api/diff/compare")
                        .param("version1Id", "1")
                        .param("version2Id", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.diffs").isArray())
                .andExpect(jsonPath("$.data.diffs.length()").value(2))
                .andExpect(jsonPath("$.data.diffs[0].type").value("INSERT"))
                .andExpect(jsonPath("$.data.diffs[1].type").value("DELETE"));

        verify(thesisService).compareVersionsWithAuth(1L, 2L, 10L, "STUDENT");
    }

    @Test
    @WithMockUser
    @DisplayName("✅ 教师对比任意论文 应返回 200")
    void test_should_return_200_when_teacher_compare_any_thesis() throws Exception {
        // Arrange
        securityUtilMock.when(SecurityUtil::getCurrentUserId).thenReturn(20L);
        securityUtilMock.when(SecurityUtil::getCurrentUserRole).thenReturn("TEACHER");

        DiffUtil.DiffResult changeDiff = new DiffUtil.DiffResult();
        changeDiff.setType("CHANGE");
        changeDiff.setOriginalPosition(3);
        changeDiff.setRevisedPosition(3);
        changeDiff.setOriginalLines(List.of("旧内容"));
        changeDiff.setRevisedLines(List.of("新内容"));

        DiffUtil.FullDiffResult fullResult = new DiffUtil.FullDiffResult();
        fullResult.setDiffs(List.of(changeDiff));
        fullResult.setOriginalLines(List.of("旧内容"));
        fullResult.setRevisedLines(List.of("新内容"));
        when(thesisService.compareVersionsWithAuth(anyLong(), anyLong(), anyLong(), eq("TEACHER")))
                .thenReturn(fullResult);

        // Act & Assert
        mockMvc.perform(get("/api/diff/compare")
                        .param("version1Id", "1")
                        .param("version2Id", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.diffs").isArray());

        verify(thesisService).compareVersionsWithAuth(1L, 2L, 20L, "TEACHER");
    }

    @Test
    @WithMockUser
    @DisplayName("✅ ContentHash相同无差异 应返回 200 和空结果")
    void test_should_return_200_with_empty_array_when_no_diff() throws Exception {
        // Arrange
        securityUtilMock.when(SecurityUtil::getCurrentUserId).thenReturn(10L);
        securityUtilMock.when(SecurityUtil::getCurrentUserRole).thenReturn("STUDENT");

        DiffUtil.FullDiffResult emptyResult = new DiffUtil.FullDiffResult();
        emptyResult.setDiffs(Collections.emptyList());
        emptyResult.setOriginalLines(Collections.emptyList());
        emptyResult.setRevisedLines(Collections.emptyList());
        when(thesisService.compareVersionsWithAuth(anyLong(), anyLong(), anyLong(), anyString()))
                .thenReturn(emptyResult);

        // Act & Assert
        mockMvc.perform(get("/api/diff/compare")
                        .param("version1Id", "1")
                        .param("version2Id", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.diffs").isArray())
                .andExpect(jsonPath("$.data.diffs").isEmpty());
    }

    // ==================== 权限拒绝测试 (403) ====================

    @Test
    @WithMockUser
    @DisplayName("❌ 学生对比他人论文 应返回 403")
    void test_should_return_403_when_student_compare_others_thesis() throws Exception {
        // Arrange
        securityUtilMock.when(SecurityUtil::getCurrentUserId).thenReturn(99L);
        securityUtilMock.when(SecurityUtil::getCurrentUserRole).thenReturn("STUDENT");

        when(thesisService.compareVersionsWithAuth(anyLong(), anyLong(), anyLong(), anyString()))
                .thenThrow(new ForbiddenException("无权访问他人论文"));

        // Act & Assert
        mockMvc.perform(get("/api/diff/compare")
                        .param("version1Id", "1")
                        .param("version2Id", "2"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403))
                .andExpect(jsonPath("$.message").value("无权访问他人论文"));
    }

    @Test
    @WithMockUser
    @DisplayName("❌ 用户未认证 应返回 403")
    void test_should_return_403_when_user_not_authenticated() throws Exception {
        // Arrange
        securityUtilMock.when(SecurityUtil::getCurrentUserId).thenReturn(null);
        securityUtilMock.when(SecurityUtil::getCurrentUserRole).thenReturn(null);

        when(thesisService.compareVersionsWithAuth(anyLong(), anyLong(), eq(null), eq(null)))
                .thenThrow(new ForbiddenException("用户未认证"));

        // Act & Assert
        mockMvc.perform(get("/api/diff/compare")
                        .param("version1Id", "1")
                        .param("version2Id", "2"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403))
                .andExpect(jsonPath("$.message").value("用户未认证"));
    }

    @Test
    @WithMockUser
    @DisplayName("❌ ADMIN角色访问 应返回 403")
    void test_should_return_403_when_admin_role() throws Exception {
        // Arrange
        securityUtilMock.when(SecurityUtil::getCurrentUserId).thenReturn(1L);
        securityUtilMock.when(SecurityUtil::getCurrentUserRole).thenReturn("ADMIN");

        when(thesisService.compareVersionsWithAuth(anyLong(), anyLong(), anyLong(), eq("ADMIN")))
                .thenThrow(new ForbiddenException("角色无权限执行此操作"));

        // Act & Assert
        mockMvc.perform(get("/api/diff/compare")
                        .param("version1Id", "1")
                        .param("version2Id", "2"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403))
                .andExpect(jsonPath("$.message").value("角色无权限执行此操作"));
    }

    // ==================== 资源不存在测试 (404) ====================

    @Test
    @WithMockUser
    @DisplayName("❌ 版本不存在 应返回 404")
    void test_should_return_404_when_version_not_exists() throws Exception {
        // Arrange
        securityUtilMock.when(SecurityUtil::getCurrentUserId).thenReturn(10L);
        securityUtilMock.when(SecurityUtil::getCurrentUserRole).thenReturn("STUDENT");

        when(thesisService.compareVersionsWithAuth(anyLong(), anyLong(), anyLong(), anyString()))
                .thenThrow(new NotFoundException("版本不存在"));

        // Act & Assert
        mockMvc.perform(get("/api/diff/compare")
                        .param("version1Id", "999")
                        .param("version2Id", "2"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.message").value("版本不存在"));
    }

    @Test
    @WithMockUser
    @DisplayName("❌ 论文不存在 应返回 404")
    void test_should_return_404_when_thesis_not_exists() throws Exception {
        // Arrange
        securityUtilMock.when(SecurityUtil::getCurrentUserId).thenReturn(10L);
        securityUtilMock.when(SecurityUtil::getCurrentUserRole).thenReturn("STUDENT");

        when(thesisService.compareVersionsWithAuth(anyLong(), anyLong(), anyLong(), anyString()))
                .thenThrow(new NotFoundException("论文不存在"));

        // Act & Assert
        mockMvc.perform(get("/api/diff/compare")
                        .param("version1Id", "1")
                        .param("version2Id", "2"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.message").value("论文不存在"));
    }

    // ==================== 非法请求测试 (400) ====================

    @Test
    @WithMockUser
    @DisplayName("❌ 缺少 version1Id 参数 应返回 400")
    void test_should_return_400_when_missing_version1Id() throws Exception {
        mockMvc.perform(get("/api/diff/compare")
                        .param("version2Id", "2"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(thesisService);
    }

    @Test
    @WithMockUser
    @DisplayName("❌ 缺少 version2Id 参数 应返回 400")
    void test_should_return_400_when_missing_version2Id() throws Exception {
        mockMvc.perform(get("/api/diff/compare")
                        .param("version1Id", "1"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(thesisService);
    }

    @Test
    @WithMockUser
    @DisplayName("❌ 对比同一版本 应返回 400")
    void test_should_return_400_when_comparing_same_version() throws Exception {
        // Arrange
        securityUtilMock.when(SecurityUtil::getCurrentUserId).thenReturn(10L);
        securityUtilMock.when(SecurityUtil::getCurrentUserRole).thenReturn("STUDENT");

        when(thesisService.compareVersionsWithAuth(anyLong(), anyLong(), anyLong(), anyString()))
                .thenThrow(new BadRequestException("不能对比同一版本"));

        // Act & Assert
        mockMvc.perform(get("/api/diff/compare")
                        .param("version1Id", "1")
                        .param("version2Id", "1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("不能对比同一版本"));
    }

    @Test
    @WithMockUser
    @DisplayName("❌ 跨论文对比 应返回 400")
    void test_should_return_400_when_cross_thesis_comparison() throws Exception {
        // Arrange
        securityUtilMock.when(SecurityUtil::getCurrentUserId).thenReturn(10L);
        securityUtilMock.when(SecurityUtil::getCurrentUserRole).thenReturn("STUDENT");

        when(thesisService.compareVersionsWithAuth(anyLong(), anyLong(), anyLong(), anyString()))
                .thenThrow(new BadRequestException("不能对比不同论文的版本"));

        // Act & Assert
        mockMvc.perform(get("/api/diff/compare")
                        .param("version1Id", "1")
                        .param("version2Id", "2"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("不能对比不同论文的版本"));
    }

    // ==================== 服务器错误测试 (500) ====================

    @Test
    @WithMockUser
    @DisplayName("❌ 文件读取失败 应返回 500")
    void test_should_return_500_when_file_read_fails() throws Exception {
        // Arrange
        securityUtilMock.when(SecurityUtil::getCurrentUserId).thenReturn(10L);
        securityUtilMock.when(SecurityUtil::getCurrentUserRole).thenReturn("STUDENT");

        when(thesisService.compareVersionsWithAuth(anyLong(), anyLong(), anyLong(), anyString()))
                .thenThrow(new RuntimeException("文件对比失败"));

        // Act & Assert
        mockMvc.perform(get("/api/diff/compare")
                        .param("version1Id", "1")
                        .param("version2Id", "2"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.message").value("服务器内部错误，请稍后重试"));
    }
}

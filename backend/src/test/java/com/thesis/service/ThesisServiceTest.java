package com.thesis.service;

import com.thesis.entity.Thesis;
import com.thesis.entity.ThesisVersion;
import com.thesis.exception.BadRequestException;
import com.thesis.exception.ForbiddenException;
import com.thesis.exception.NotFoundException;
import com.thesis.mapper.ThesisMapper;
import com.thesis.mapper.ThesisVersionMapper;
import com.thesis.util.DiffUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * 单元测试: ThesisService.compareVersionsWithAuth()
 * 覆盖场景: 权限校验、参数验证、跨论文阻断、ContentHash优化
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ThesisService - 版本对比权限校验")
class ThesisServiceTest {

    @Mock
    private ThesisMapper thesisMapper;

    @Mock
    private ThesisVersionMapper thesisVersionMapper;

    @Mock
    private DiffUtil diffUtil;

    @InjectMocks
    private ThesisService thesisService;

    private ThesisVersion version1;
    private ThesisVersion version2;
    private Thesis thesis;

    @BeforeEach
    void setUp() {
        // 准备测试数据
        version1 = new ThesisVersion();
        version1.setId(1L);
        version1.setThesisId(100L);
        version1.setFilePath("/uploads/v1.txt");
        version1.setContentHash("hash123");

        version2 = new ThesisVersion();
        version2.setId(2L);
        version2.setThesisId(100L);
        version2.setFilePath("/uploads/v2.txt");
        version2.setContentHash("hash456");

        thesis = new Thesis();
        thesis.setId(100L);
        thesis.setStudentId(10L); // 学生ID
        thesis.setTitle("测试论文");
    }

    @AfterEach
    void tearDown() {
        reset(thesisMapper, thesisVersionMapper, diffUtil);
    }

    // ==================== 认证校验测试 ====================

    @Test
    @DisplayName("❌ 用户未认证 (userId=null) 应抛出 ForbiddenException")
    void test_should_throw_forbidden_when_userId_is_null() {
        assertThatThrownBy(() ->
                thesisService.compareVersionsWithAuth(1L, 2L, null, "STUDENT"))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("用户未认证");

        verifyNoInteractions(thesisVersionMapper, thesisMapper, diffUtil);
    }

    @Test
    @DisplayName("❌ 角色未认证 (role=null) 应抛出 ForbiddenException")
    void test_should_throw_forbidden_when_role_is_null() {
        assertThatThrownBy(() ->
                thesisService.compareVersionsWithAuth(1L, 2L, 10L, null))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("用户未认证");

        verifyNoInteractions(thesisVersionMapper, thesisMapper, diffUtil);
    }

    // ==================== 参数校验测试 ====================

    @Test
    @DisplayName("❌ 版本1 ID为null 应抛出 BadRequestException")
    void test_should_throw_bad_request_when_version1Id_is_null() {
        assertThatThrownBy(() ->
                thesisService.compareVersionsWithAuth(null, 2L, 10L, "STUDENT"))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("版本ID不能为空");
    }

    @Test
    @DisplayName("❌ 版本2 ID为null 应抛出 BadRequestException")
    void test_should_throw_bad_request_when_version2Id_is_null() {
        assertThatThrownBy(() ->
                thesisService.compareVersionsWithAuth(1L, null, 10L, "STUDENT"))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("版本ID不能为空");
    }

    @Test
    @DisplayName("❌ 对比同一版本 应抛出 BadRequestException")
    void test_should_throw_bad_request_when_comparing_same_version() {
        assertThatThrownBy(() ->
                thesisService.compareVersionsWithAuth(1L, 1L, 10L, "STUDENT"))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("不能对比同一版本");
    }

    // ==================== 资源存在性测试 ====================

    @Test
    @DisplayName("❌ 版本1不存在 应抛出 NotFoundException")
    void test_should_throw_not_found_when_version1_not_exists() {
        when(thesisVersionMapper.selectById(1L)).thenReturn(null);

        assertThatThrownBy(() ->
                thesisService.compareVersionsWithAuth(1L, 2L, 10L, "STUDENT"))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("版本不存在");
    }

    @Test
    @DisplayName("❌ 版本2不存在 应抛出 NotFoundException")
    void test_should_throw_not_found_when_version2_not_exists() {
        when(thesisVersionMapper.selectById(1L)).thenReturn(version1);
        when(thesisVersionMapper.selectById(2L)).thenReturn(null);

        assertThatThrownBy(() ->
                thesisService.compareVersionsWithAuth(1L, 2L, 10L, "STUDENT"))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("版本不存在");
    }

    @Test
    @DisplayName("❌ 论文不存在 应抛出 NotFoundException")
    void test_should_throw_not_found_when_thesis_not_exists() {
        when(thesisVersionMapper.selectById(1L)).thenReturn(version1);
        when(thesisVersionMapper.selectById(2L)).thenReturn(version2);
        when(thesisMapper.selectById(100L)).thenReturn(null);

        assertThatThrownBy(() ->
                thesisService.compareVersionsWithAuth(1L, 2L, 10L, "STUDENT"))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("论文不存在");
    }

    // ==================== 跨论文阻断测试 ====================

    @Test
    @DisplayName("❌ 跨论文对比 应抛出 BadRequestException")
    void test_should_throw_bad_request_when_cross_thesis_comparison() {
        version2.setThesisId(200L); // 不同论文

        when(thesisVersionMapper.selectById(1L)).thenReturn(version1);
        when(thesisVersionMapper.selectById(2L)).thenReturn(version2);

        assertThatThrownBy(() ->
                thesisService.compareVersionsWithAuth(1L, 2L, 10L, "STUDENT"))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("不能对比不同论文的版本");
    }

    // ==================== 权限校验测试 (STUDENT) ====================

    @Test
    @DisplayName("✅ 学生对比自己的论文 应成功返回差异")
    void test_should_succeed_when_student_compare_own_thesis() throws IOException {
        // Arrange
        when(thesisVersionMapper.selectById(1L)).thenReturn(version1);
        when(thesisVersionMapper.selectById(2L)).thenReturn(version2);
        when(thesisMapper.selectById(100L)).thenReturn(thesis);

        DiffUtil.DiffResult diffResult = new DiffUtil.DiffResult();
        diffResult.setType("INSERT");
        diffResult.setOriginalPosition(0);
        diffResult.setRevisedPosition(0);
        diffResult.setOriginalLines(Collections.emptyList());
        diffResult.setRevisedLines(List.of("新行"));
        
        DiffUtil.FullDiffResult fullResult = new DiffUtil.FullDiffResult();
        fullResult.setDiffs(List.of(diffResult));
        fullResult.setOriginalLines(List.of("旧内容"));
        fullResult.setRevisedLines(List.of("新行"));
        when(diffUtil.compareFilesWithFullContent(anyString(), anyString())).thenReturn(fullResult);

        // Act
        DiffUtil.FullDiffResult result = thesisService.compareVersionsWithAuth(
                1L, 2L, 10L, "STUDENT"); // studentId=10L 匹配 thesis.studentId

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getDiffs()).hasSize(1);
        verify(diffUtil).compareFilesWithFullContent(anyString(), anyString());
    }

    @Test
    @DisplayName("❌ 学生对比他人论文 应抛出 ForbiddenException")
    void test_should_throw_forbidden_when_student_compare_others_thesis() {
        when(thesisVersionMapper.selectById(1L)).thenReturn(version1);
        when(thesisVersionMapper.selectById(2L)).thenReturn(version2);
        when(thesisMapper.selectById(100L)).thenReturn(thesis);

        assertThatThrownBy(() ->
                thesisService.compareVersionsWithAuth(1L, 2L, 99L, "STUDENT")) // studentId=99L != 10L
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("无权访问他人论文");

        verifyNoInteractions(diffUtil);
    }

    // ==================== 权限校验测试 (TEACHER) ====================

    @Test
    @DisplayName("✅ 教师对比任意论文 应成功返回差异")
    void test_should_succeed_when_teacher_compare_any_thesis() throws IOException {
        when(thesisVersionMapper.selectById(1L)).thenReturn(version1);
        when(thesisVersionMapper.selectById(2L)).thenReturn(version2);
        when(thesisMapper.selectById(100L)).thenReturn(thesis);

        DiffUtil.DiffResult diffResult = new DiffUtil.DiffResult();
        diffResult.setType("DELETE");
        diffResult.setOriginalPosition(0);
        diffResult.setRevisedPosition(0);
        diffResult.setOriginalLines(List.of("旧行"));
        diffResult.setRevisedLines(Collections.emptyList());
        
        DiffUtil.FullDiffResult fullResult = new DiffUtil.FullDiffResult();
        fullResult.setDiffs(List.of(diffResult));
        fullResult.setOriginalLines(List.of("旧行"));
        fullResult.setRevisedLines(Collections.emptyList());
        when(diffUtil.compareFilesWithFullContent(anyString(), anyString())).thenReturn(fullResult);

        // Act
        DiffUtil.FullDiffResult result = thesisService.compareVersionsWithAuth(
                1L, 2L, 20L, "TEACHER"); // teacherId=20L (不是论文所有者)

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getDiffs()).hasSize(1);
        verify(diffUtil).compareFilesWithFullContent(anyString(), anyString());
    }

    // ==================== 角色白名单测试 ====================

    @Test
    @DisplayName("❌ ADMIN 角色访问 应抛出 ForbiddenException")
    void test_should_throw_forbidden_when_admin_role() {
        when(thesisVersionMapper.selectById(1L)).thenReturn(version1);
        when(thesisVersionMapper.selectById(2L)).thenReturn(version2);
        when(thesisMapper.selectById(100L)).thenReturn(thesis);

        assertThatThrownBy(() ->
                thesisService.compareVersionsWithAuth(1L, 2L, 1L, "ADMIN"))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("角色无权限执行此操作");

        verifyNoInteractions(diffUtil);
    }

    @Test
    @DisplayName("❌ 未知角色访问 应抛出 ForbiddenException")
    void test_should_throw_forbidden_when_unknown_role() {
        when(thesisVersionMapper.selectById(1L)).thenReturn(version1);
        when(thesisVersionMapper.selectById(2L)).thenReturn(version2);
        when(thesisMapper.selectById(100L)).thenReturn(thesis);

        assertThatThrownBy(() ->
                thesisService.compareVersionsWithAuth(1L, 2L, 1L, "GUEST"))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("角色无权限执行此操作");
    }

    // ==================== ContentHash 短路优化测试 ====================

    @Test
    @DisplayName("✅ ContentHash 相同 应返回空结果 (短路优化)")
    void test_should_return_empty_when_content_hash_same() {
        version2.setContentHash("hash123"); // 与 version1 相同

        when(thesisVersionMapper.selectById(1L)).thenReturn(version1);
        when(thesisVersionMapper.selectById(2L)).thenReturn(version2);
        when(thesisMapper.selectById(100L)).thenReturn(thesis);

        // Act
        DiffUtil.FullDiffResult result = thesisService.compareVersionsWithAuth(
                1L, 2L, 10L, "STUDENT");

        // Assert
        assertThat(result.getDiffs()).isEmpty();
        verifyNoInteractions(diffUtil); // 重要: 不应调用文件对比
    }

    // ==================== 异常传播测试 ====================

    @Test
    @DisplayName("❌ 文件对比失败 应抛出 RuntimeException")
    void test_should_throw_runtime_exception_when_diff_fails() throws IOException {
        when(thesisVersionMapper.selectById(1L)).thenReturn(version1);
        when(thesisVersionMapper.selectById(2L)).thenReturn(version2);
        when(thesisMapper.selectById(100L)).thenReturn(thesis);
        when(diffUtil.compareFilesWithFullContent(anyString(), anyString()))
                .thenThrow(new IOException("文件读取失败"));

        assertThatThrownBy(() ->
                thesisService.compareVersionsWithAuth(1L, 2L, 10L, "STUDENT"))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("文件对比失败")
                .hasCauseInstanceOf(IOException.class);
    }
}

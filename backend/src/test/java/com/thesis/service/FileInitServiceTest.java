package com.thesis.service;

import com.thesis.mapper.ThesisMapper;
import com.thesis.mapper.ThesisVersionMapper;
import com.thesis.mapper.UserMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("FileInitService - 强制同步测试")
class FileInitServiceTest {

    @Mock
    private ThesisMapper thesisMapper;

    @Mock
    private ThesisVersionMapper thesisVersionMapper;

    @Mock
    private UserMapper userMapper;

    @Spy
    @InjectMocks
    private FileInitService fileInitService;

    @Test
    @DisplayName("强制同步应先清空表再重建，返回正确统计")
    void test_force_sync_should_purge_then_rebuild() throws Exception {
        // Arrange: 模拟清空操作返回删除数
        when(thesisVersionMapper.physicalDeleteAll()).thenReturn(5);
        when(thesisMapper.physicalDeleteAll()).thenReturn(3);

        // 模拟重建后的记录数
        when(thesisMapper.selectCount(null)).thenReturn(10L);
        when(thesisVersionMapper.selectCount(null)).thenReturn(10L);

        // 跳过实际扫描
        doNothing().when(fileInitService).scanAndInitialize();

        // Act
        Map<String, Integer> result = fileInitService.forceSyncFromFileSystem();

        // Assert: 验证返回值
        assertEquals(5, result.get("purgedVersions"));
        assertEquals(3, result.get("purgedTheses"));
        assertEquals(10, result.get("newTheses"));
        assertEquals(10, result.get("newVersions"));

        // Assert: 验证调用顺序——先删版本，再删论文，再重建
        InOrder inOrder = inOrder(thesisVersionMapper, thesisMapper, fileInitService);
        inOrder.verify(thesisVersionMapper).physicalDeleteAll();
        inOrder.verify(thesisMapper).physicalDeleteAll();
        inOrder.verify(fileInitService).scanAndInitialize();
    }

    @Test
    @DisplayName("空数据库强制同步应正常执行")
    void test_force_sync_on_empty_database() throws Exception {
        when(thesisVersionMapper.physicalDeleteAll()).thenReturn(0);
        when(thesisMapper.physicalDeleteAll()).thenReturn(0);
        when(thesisMapper.selectCount(null)).thenReturn(0L);
        when(thesisVersionMapper.selectCount(null)).thenReturn(0L);
        doNothing().when(fileInitService).scanAndInitialize();

        Map<String, Integer> result = fileInitService.forceSyncFromFileSystem();

        assertEquals(0, result.get("purgedVersions"));
        assertEquals(0, result.get("purgedTheses"));
        assertEquals(0, result.get("newTheses"));
        assertEquals(0, result.get("newVersions"));
    }
}

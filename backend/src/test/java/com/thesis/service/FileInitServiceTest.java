package com.thesis.service;

import com.thesis.entity.Thesis;
import com.thesis.entity.ThesisVersion;
import com.thesis.mapper.ThesisMapper;
import com.thesis.mapper.ThesisVersionMapper;
import com.thesis.mapper.UserMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
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
    void test_should_delete_missing_versions_and_empty_theses_and_return_counts() throws Exception {
        String existingPath = new File("pom.xml").getAbsolutePath();
        String missingPath = Paths.get("target", "force-sync-missing-" + System.nanoTime() + ".docx")
                .toAbsolutePath()
                .toString();

        ThesisVersion missingVersion = new ThesisVersion();
        missingVersion.setId(1L);
        missingVersion.setFilePath(missingPath);

        ThesisVersion existingVersion = new ThesisVersion();
        existingVersion.setId(2L);
        existingVersion.setFilePath(existingPath);

        when(thesisVersionMapper.selectList(null))
                .thenReturn(List.of(missingVersion, existingVersion));

        Thesis thesisToDelete = new Thesis();
        thesisToDelete.setId(10L);
        thesisToDelete.setTitle("T1");
        thesisToDelete.setCurrentVersion(1);

        Thesis thesisToUpdate = new Thesis();
        thesisToUpdate.setId(11L);
        thesisToUpdate.setTitle("T2");
        thesisToUpdate.setCurrentVersion(1);

        when(thesisMapper.selectList(null))
                .thenReturn(List.of(thesisToDelete, thesisToUpdate));

        when(thesisVersionMapper.selectCount(any()))
                .thenReturn(0L, 2L);

        ThesisVersion latestVersion = new ThesisVersion();
        latestVersion.setVersionNum(3);
        when(thesisVersionMapper.selectOne(any()))
                .thenReturn(latestVersion);

        doNothing().when(fileInitService).scanAndInitialize();

        Map<String, Integer> result = fileInitService.forceSyncFromFileSystem();

        assertEquals(1, result.get("deletedVersions"));
        assertEquals(1, result.get("deletedTheses"));
        verify(thesisVersionMapper).deleteById(1L);
        verify(thesisMapper).deleteById(10L);
        verify(thesisMapper).updateById(argThat(
                thesis -> thesis.getId().equals(11L) && thesis.getCurrentVersion().equals(3)
        ));
        verify(fileInitService).scanAndInitialize();
    }
}

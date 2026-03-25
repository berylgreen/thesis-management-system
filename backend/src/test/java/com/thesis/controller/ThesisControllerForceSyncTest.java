package com.thesis.controller;

import com.thesis.entity.User;
import com.thesis.mapper.ReviewMapper;
import com.thesis.mapper.ThesisMapper;
import com.thesis.mapper.ThesisVersionMapper;
import com.thesis.mapper.UserMapper;
import com.thesis.service.FileInitService;
import com.thesis.util.JwtUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration," +
                "com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration",
        "file.upload-path=uploads"
})
@AutoConfigureMockMvc
class ThesisControllerForceSyncTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserMapper userMapper;

    @MockBean
    private ThesisMapper thesisMapper;

    @MockBean
    private ThesisVersionMapper thesisVersionMapper;

    @MockBean
    private ReviewMapper reviewMapper;

    @MockBean
    private FileInitService fileInitService;

    @MockBean
    private JwtUtil jwtUtil;

    private UsernamePasswordAuthenticationToken authWithUserId(Long userId, String role) {
        return new UsernamePasswordAuthenticationToken(
                userId,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_" + role))
        );
    }

    @Test
    void test_should_force_sync_when_teacher() throws Exception {
        User user = new User();
        user.setId(1L);
        user.setRole("TEACHER");
        when(userMapper.selectById(1L)).thenReturn(user);

        Map<String, Integer> result = new HashMap<>();
        result.put("deletedVersions", 2);
        result.put("deletedTheses", 1);
        when(fileInitService.forceSyncFromFileSystem()).thenReturn(result);

        mockMvc.perform(post("/api/thesis/admin/force-sync")
                        .with(authentication(authWithUserId(1L, "TEACHER"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.deletedVersions").value(2))
                .andExpect(jsonPath("$.data.deletedTheses").value(1));

        verify(fileInitService).forceSyncFromFileSystem();
    }

    @Test
    void test_should_force_sync_when_admin() throws Exception {
        User user = new User();
        user.setId(2L);
        user.setRole("ADMIN");
        when(userMapper.selectById(2L)).thenReturn(user);

        Map<String, Integer> result = new HashMap<>();
        result.put("deletedVersions", 0);
        result.put("deletedTheses", 0);
        when(fileInitService.forceSyncFromFileSystem()).thenReturn(result);

        mockMvc.perform(post("/api/thesis/admin/force-sync")
                        .with(authentication(authWithUserId(2L, "ADMIN"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.deletedVersions").value(0))
                .andExpect(jsonPath("$.data.deletedTheses").value(0));

        verify(fileInitService).forceSyncFromFileSystem();
    }

    @Test
    void test_should_reject_when_student_role() throws Exception {
        User user = new User();
        user.setId(3L);
        user.setRole("STUDENT");
        when(userMapper.selectById(3L)).thenReturn(user);

        mockMvc.perform(post("/api/thesis/admin/force-sync")
                        .with(authentication(authWithUserId(3L, "STUDENT"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.message").value("无权限执行此操作"));

        verifyNoInteractions(fileInitService);
    }

    @Test
    void test_should_return_403_when_unauthenticated() throws Exception {
        mockMvc.perform(post("/api/thesis/admin/force-sync"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(userMapper);
    }

    @Test
    void test_should_return_error_when_service_throws() throws Exception {
        User user = new User();
        user.setId(4L);
        user.setRole("TEACHER");
        when(userMapper.selectById(4L)).thenReturn(user);
        when(fileInitService.forceSyncFromFileSystem())
                .thenThrow(new RuntimeException("IO error"));

        mockMvc.perform(post("/api/thesis/admin/force-sync")
                        .with(authentication(authWithUserId(4L, "TEACHER"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.message", containsString("同步失败")));
    }
}

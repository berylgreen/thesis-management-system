package com.thesis.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.thesis.dto.LoginRequest;
import com.thesis.dto.LoginResponse;
import com.thesis.dto.RegisterRequest;
import com.thesis.exception.GlobalExceptionHandler;
import com.thesis.mapper.ReviewMapper;
import com.thesis.mapper.ThesisMapper;
import com.thesis.mapper.ThesisVersionMapper;
import com.thesis.mapper.UserMapper;
import com.thesis.service.UserService;
import com.thesis.util.JwtUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Controller tests for authentication endpoints (register/login).
 * Tests normal flows, validation errors, duplicate users, and invalid credentials.
 */
@SpringBootTest(properties = {
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration," +
                "com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration"
})
@AutoConfigureMockMvc
@Import(GlobalExceptionHandler.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserService userService;

    @MockBean
    private JwtUtil jwtUtil;

    @MockBean
    private UserMapper userMapper;

    @MockBean
    private ThesisMapper thesisMapper;

    @MockBean
    private ThesisVersionMapper thesisVersionMapper;

    @MockBean
    private ReviewMapper reviewMapper;

    private RegisterRequest registerRequest;
    private LoginRequest loginRequest;

    @BeforeEach
    void setUp() {
        // Arrange: prepare test data
        registerRequest = new RegisterRequest();
        registerRequest.setUsername("alice");
        registerRequest.setPassword("secret123");
        registerRequest.setRole("STUDENT");
        registerRequest.setRealName("Alice Smith");
        registerRequest.setEmail("alice@example.com");

        loginRequest = new LoginRequest();
        loginRequest.setUsername("alice");
        loginRequest.setPassword("secret123");
    }

    @AfterEach
    void tearDown() {
        reset(userService);
    }

    @Test
    void test_should_register_successfully() throws Exception {
        // Arrange
        LoginResponse response = new LoginResponse("jwt-token", 1L, "alice", "STUDENT");
        when(userService.register(any(RegisterRequest.class))).thenReturn(response);

        // Act + Assert
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.token").value("jwt-token"))
                .andExpect(jsonPath("$.data.username").value("alice"));
    }

    @Test
    void test_should_fail_register_when_duplicate_username() throws Exception {
        // Arrange
        when(userService.register(any(RegisterRequest.class)))
                .thenThrow(new RuntimeException("Username already exists"));

        // Act + Assert
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500));
    }

    @Test
    void test_should_fail_register_with_validation_error() throws Exception {
        // Arrange
        registerRequest.setUsername(""); // Invalid: empty username

        // Act + Assert
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(userService);
    }

    @Test
    void test_should_login_successfully() throws Exception {
        // Arrange
        LoginResponse response = new LoginResponse("jwt-token", 1L, "alice", "STUDENT");
        when(userService.login(any(LoginRequest.class))).thenReturn(response);

        // Act + Assert
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.token").value("jwt-token"));
    }

    @Test
    void test_should_fail_login_when_invalid_credentials() throws Exception {
        // Arrange
        when(userService.login(any(LoginRequest.class)))
                .thenThrow(new RuntimeException("Invalid username or password"));

        // Act + Assert
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500));
    }

    @Test
    void test_should_fail_login_with_missing_password() throws Exception {
        // Arrange
        loginRequest.setPassword(null);

        // Act + Assert
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(userService);
    }
}

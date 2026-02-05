package com.thesis.controller;

import com.thesis.mapper.ReviewMapper;
import com.thesis.mapper.ThesisMapper;
import com.thesis.mapper.ThesisVersionMapper;
import com.thesis.mapper.UserMapper;
import com.thesis.util.JwtUtil;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import static org.mockito.Mockito.mock;

/**
 * 测试安全配置
 * 为集成测试提供 Mock 的 Security 相关 Bean 和 MyBatis Mapper
 */
@TestConfiguration
public class TestSecurityConfig {

    @Bean
    @Primary
    public JwtUtil jwtUtil() {
        return mock(JwtUtil.class);
    }

    // Mock all MyBatis Mappers to prevent sqlSessionFactory initialization
    @MockBean
    private UserMapper userMapper;

    @MockBean
    private ThesisMapper thesisMapper;

    @MockBean
    private ThesisVersionMapper thesisVersionMapper;

    @MockBean
    private ReviewMapper reviewMapper;
}

package com.example.oauth2.server;

import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import javax.annotation.Resource;

/**
 * @AutoConfigureMockMvc是一个注解，用于自动配置MockMvc实例。MockMvc是Spring MVC提供的一个核心类，
 * 用于在测试环境下模拟发送HTTP请求，然后检查响应是否符合预期。这个注解确保了在测试开始之前，
 * MockMvc实例会被自动注入到测试类中，使得开发者可以直接使用它来编写测试用例。
 * 使用MockMvc可以进行精细的Web层测试，包括请求路径、HTTP方法、请求参数、响应状态码和响应内容等。
 */
@SpringBootTest
@AutoConfigureMockMvc
public class OAuth2ServerApplicationTests {

    @Resource
    protected MockMvc mockMvc;
}

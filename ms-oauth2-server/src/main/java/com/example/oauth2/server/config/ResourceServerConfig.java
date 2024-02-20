package com.example.oauth2.server.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableResourceServer;
import org.springframework.security.oauth2.config.annotation.web.configuration.ResourceServerConfigurerAdapter;
import org.springframework.security.oauth2.config.annotation.web.configurers.ResourceServerSecurityConfigurer;

import javax.annotation.Resource;

/**
 * 资源服务
 */
@Configuration
@EnableResourceServer
public class ResourceServerConfig extends ResourceServerConfigurerAdapter {
    @Resource
    private MyAuthenticationEntryPoint authenticationEntryPoint;

    /**
     * 这个方法用于配置HTTP安全规则。通过参数HttpSecurity，可以定义哪些HTTP请求需要被保护以及如何保护。
     * http.authorizeRequests().anyRequest().authenticated()：这一行配置表示所有请求都需要认证后才能访问。
     * 它确保了资源服务器上的所有资源都不会被未经认证的用户访问。
     * .and().requestMatchers().antMatchers("/user/**")：这一行指定了哪些路径模式的请求将被这个资源服务器的安全配置所管理。
     * 在这个例子中，只有以/user/开头的请求会被这个资源服务器的安全配置所管理。
     * @param http
     * @throws Exception
     */
    @Override
    public void configure(HttpSecurity http) throws Exception {
        // 配置放行的资源
        http.authorizeRequests()
                .anyRequest()
                .authenticated()
                .and()
                .requestMatchers()
                .antMatchers("/user/**");
    }

    /**
     * 这个方法用于配置资源服务器本身的一些属性。
     * resources.authenticationEntryPoint(authenticationEntryPoint)：这一行配置了一个自定义的认证入口点authenticationEntryPoint。
     * 当资源访问被拒绝且用户需要开始认证流程时，这个入口点将被调用。通过自定义认证入口点，可以控制如何响应认证错误，例如返回特定的错误码或错误信息。
     * @param resources
     * @throws Exception
     */
    @Override
    public void configure(ResourceServerSecurityConfigurer resources) throws Exception {
        resources.authenticationEntryPoint(authenticationEntryPoint);
    }
}

package com.example.oauth2.server.config;

import com.example.commons.model.domain.SignInIdentity;
import com.example.oauth2.server.service.UserService;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.common.DefaultOAuth2AccessToken;
import org.springframework.security.oauth2.config.annotation.configurers.ClientDetailsServiceConfigurer;
import org.springframework.security.oauth2.config.annotation.web.configuration.AuthorizationServerConfigurerAdapter;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableAuthorizationServer;
import org.springframework.security.oauth2.config.annotation.web.configurers.AuthorizationServerEndpointsConfigurer;
import org.springframework.security.oauth2.config.annotation.web.configurers.AuthorizationServerSecurityConfigurer;
import org.springframework.security.oauth2.provider.token.store.redis.RedisTokenStore;

import javax.annotation.Resource;
import java.util.LinkedHashMap;

/**
 * 授权服务
 */
@Configuration
@EnableAuthorizationServer
public class AuthorizationServerConfiguration extends AuthorizationServerConfigurerAdapter {
    // RedisTokenStore
    @Resource
    private RedisTokenStore redisTokenStore;

    // 认证管理对象
    @Resource
    private AuthenticationManager authenticationManager;

    // 密码编码器
    @Resource
    private PasswordEncoder passwordEncoder;

    // 客户端配置类
    @Resource
    private ClientOAuth2DataConfiguration clientOAuth2DataConfiguration;

    // 登录校验
    @Resource
    private UserService userService;

    /**
     * 配置令牌端点安全约束  该方法用于配置 OAuth2 授权服务器的安全相关信息
     * @param security
     * @throws Exception
     */
    @Override
    public void configure(AuthorizationServerSecurityConfigurer security) throws Exception {
        // 允许访问token的公钥，默认/oauth/token_key是受保护的
        security.tokenKeyAccess("permitAll()")
                // 允许检查token的状态，默认/oauth/check_token是受保护的
                .checkTokenAccess("permitAll()");
    }

    /**
     * 该方法用来配置客户端信息，包括客户端 ID、密钥、授权类型等属性
     * @param clients
     * @throws Exception
     */
    @Override
    public void configure(ClientDetailsServiceConfigurer clients) throws Exception {
        clients.inMemory().withClient(clientOAuth2DataConfiguration.getClientId())  // 客户端标识id
                .secret(passwordEncoder.encode(clientOAuth2DataConfiguration.getSecret()))  // 客户端安全码
                .authorizedGrantTypes(clientOAuth2DataConfiguration.getGrantTypes())        // 授权类型
                .accessTokenValiditySeconds(clientOAuth2DataConfiguration.getTokenValidityTime())   // token的有效时期
                .refreshTokenValiditySeconds(clientOAuth2DataConfiguration.getRefreshTokenValidityTime())  // 刷新token的有效期
                .scopes(clientOAuth2DataConfiguration.getScopes());         // 客户端的访问范围
    }

    /**
     * 该方法用来配置认证和授权的逻辑，包括用户认证器、令牌存储方式、授权类型等属性
     * @param endpoints
     * @throws Exception
     */
    @Override
    public void configure(AuthorizationServerEndpointsConfigurer endpoints) throws Exception {
        endpoints.authenticationManager(authenticationManager)  // 认证器
                // 具体登录的方法
                .userDetailsService(userService)
                // token的存储方式
                .tokenStore(redisTokenStore)
                // 令牌增强对象，增强返回的结果
                .tokenEnhancer(((accessToken, authentication) -> {
                    // 获取登录后的用户信息
                    SignInIdentity signInIdentity = (SignInIdentity) authentication.getPrincipal();
                    DefaultOAuth2AccessToken token = (DefaultOAuth2AccessToken) accessToken;
                    LinkedHashMap<String, Object> map = new LinkedHashMap<>();
                    map.put("nickname", signInIdentity.getNickname());
                    map.put("avatarUrl", signInIdentity.getAvatarUrl());
                    token.setAdditionalInformation(map);
                    return token;
                }));
    }
}

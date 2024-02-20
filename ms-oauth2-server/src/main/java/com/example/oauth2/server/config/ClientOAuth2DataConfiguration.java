package com.example.oauth2.server.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 客户端配置类
 */
@Component
@ConfigurationProperties(prefix = "client.oauth2")
@Data
public class ClientOAuth2DataConfiguration {
    private String clientId;        // 客户端标识id
    private String secret;          // 客户端安全码
    private String[] grantTypes;   // 授权类型
    private int tokenValidityTime;  // token有效期
    private int refreshTokenValidityTime;   // refresh-token有效期
    private String[] scopes;        // 客户端访问范围
}

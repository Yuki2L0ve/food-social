package com.example.gateway.filter;

import com.example.gateway.component.HandleException;
import com.example.gateway.config.IgnoreUrlsConfig;
import org.apache.commons.lang.StringUtils;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.annotation.Resource;

/**
 * 网关全局过滤器
 */
@Component
public class AuthGlobalFilter implements GlobalFilter, Ordered {
    @Resource
    private IgnoreUrlsConfig ignoreUrlsConfig;
    @Resource
    private RestTemplate restTemplate;
    @Resource
    private HandleException handleException;

    /**
     * 身份校验处理
     * @param exchange: 代表当前HTTP请求-响应交换的上下文。可以通过它访问请求和响应。它封装了当前请求和响应的所有信息，提供了访问和修改请求和响应的方法。
     * (1)获取请求信息：可以通过exchange.getRequest()获取到当前的ServerHttpRequest对象，进而获取到请求路径、请求头、查询参数等信息。
     * (2)修改响应：可以通过exchange.getResponse()获取到当前的ServerHttpResponse对象，用于修改响应状态码、设置响应头、写入响应体等。
     * @param chain: 代表过滤器链, 它负责管理网关过滤器的执行流程。调用chain.filter(exchange)将请求传递给下一个过滤器，或者如果当前过滤器是链中的最后一个，则将请求发送到下游服务。
     * @return
     * 这个方法的返回类型是Mono<Void>，这是Project Reactor中的一个响应式编程类型，表示一个异步的操作。
     * 在这个上下文中，它表示过滤器逻辑的执行可能是非阻塞的，并且在完成所有操作后不返回任何值。
     * 返回Mono<Void>允许过滤器以异步的方式执行，提高了处理请求的效率。
     */
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // 判断当前请求是否在白名单中
        // 使用AntPathMatcher（一种路径匹配工具）检查当前请求的路径是否在配置的白名单中。如果是，flag被设置为true，表示请求应该直接放行。
        AntPathMatcher pathMatcher = new AntPathMatcher();
        boolean flag = false;
        String path = exchange.getRequest().getURI().getPath();
        for (String url : ignoreUrlsConfig.getUrls()) {
            if (pathMatcher.match(url, path)) {
                flag = true;
                break;
            }
        }

        // 白名单放行
        if (flag) {
            // 如果请求在白名单中，通过调用chain.filter(exchange)直接放行，不进行后续的身份校验。
            return chain.filter(exchange);
        }

        // 获取access_token
        String access_token = exchange.getRequest().getQueryParams().getFirst("access_token");
        // 判断access_token是否为空
        if (StringUtils.isBlank(access_token)) {
            return handleException.writeError(exchange, "请登录");
        }

        /**
         * 构造一个URL，用于向OAuth2服务器发送请求，以校验access_token的有效性。
         * 使用RestTemplate发送GET请求到校验token的URL。
         * 如果响应状态码不是HttpStatus.OK或响应体为空，表示access_token无效，返回错误响应。
         */
        // 校验token是否有效
        String checkTokenUrl = "http://ms-oauth2-server/oauth/check_token?token=".concat(access_token);
        try {
            // 发送远程请求，验证token
            ResponseEntity<String> entity = restTemplate.getForEntity(checkTokenUrl, String.class);
            // token 无效的业务逻辑处理
            if (entity.getStatusCode() != HttpStatus.OK) {
                return handleException.writeError(exchange, "Token was not recognised, token: ".concat(access_token));
            }
            if (StringUtils.isBlank(entity.getBody())) {
                return handleException.writeError(exchange, "This token is invalid: ".concat(access_token));
            }
        } catch (Exception e) {
            return handleException.writeError(exchange, "Token was not recognised, token: ".concat(access_token));
        }

        // 放行
        return chain.filter(exchange);
    }

    /**
     * 网关过滤器的排序，数字越小，优先级就越高
     * @return
     */
    @Override
    public int getOrder() {
        return 0;
    }
}

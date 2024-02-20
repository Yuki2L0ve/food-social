package com.example.oauth2.server.controller;

import com.example.commons.model.domain.ResultInfo;
import com.example.commons.model.domain.SignInIdentity;
import com.example.commons.model.vo.SignInDinerInfo;
import com.example.commons.utils.ResultInfoUtil;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.common.OAuth2RefreshToken;
import org.springframework.security.oauth2.provider.token.store.redis.RedisTokenStore;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

/**
 * 用户中心
 */
@RestController
public class UserController {
    @Resource
    private HttpServletRequest request;
    @Resource
    private RedisTokenStore redisTokenStore;

    @GetMapping("/user/me")
    public ResultInfo getCurrentUser(Authentication authentication) {
        // 获取登录后的用户信息
        SignInIdentity signInIdentity = (SignInIdentity) authentication.getPrincipal();
        // 转为前端可用的视图对象
        SignInDinerInfo dinerInfo = new SignInDinerInfo();
        BeanUtils.copyProperties(signInIdentity, dinerInfo);
        return ResultInfoUtil.buildSuccess(request.getServletPath(), dinerInfo);
    }

    /**
     * 安全退出
     * @param access_token
     * @param authorization
     * @return
     */
    @GetMapping("/user/logout")
    public ResultInfo logout(String access_token, String authorization) {
        /**
         * 客户端可以通过多种方式向服务器传递访问令牌（access_token），常见有两种：
         * （1）通过请求参数传递：比如http://baidu.com/api/resource?access_token=???，但是这种方法有安全风险
         * （2）通过请求头（Authorization头）传递：更安全的做法是将访问令牌放在HTTP请求的Authorization头中，
         * 通常是以Bearer标记作为前缀，例如Authorization：Bearer xxx 这种方式更安全
         */

        // 这段代码首先检查access_token是否为空通过请求参数传递（即直接作为方法参数提供）
        // 如果没有提供（access_token）为空，则尝试从Authorization请求头中获取令牌（即authorization参数）
        if (StringUtils.isBlank(access_token)) {
            access_token = authorization;
        }

        /**
         * 这里的第二个判断是为了处理一种特殊情况：即使尝试从authorization参数获取访问令牌后，access_token仍然为空。这种情况可能发生在以下几种情形：
         * (1)客户端未提供任何令牌信息：既没有通过请求参数提供access_token，也没有在请求头中提供authorization信息。
         * 这意味着客户端可能根本没有尝试进行认证，或者请求是匿名的。
         * (2)提供的令牌信息无效：即使客户端在authorization请求头中提供了信息，但该信息可能不包含有效的访问令牌，
         * 例如仅仅是一个空字符串或仅包含"Bearer "前缀而没有实际的令牌。
         * 为什么这么做？
         * （1）优雅地处理无令牌情况：在安全退出的上下文中，如果客户端没有提供有效的访问令牌，可能意味着用户已经是未登录状态，或者令牌已经失效。
         * 在这种情况下，继续执行退出操作没有实际意义，因为没有什么可以清理的。因此，直接返回“退出成功”的响应是一种优雅的处理方式，避免了不必要的错误或异常。
         * （2）简化客户端逻辑：这样做允许客户端在不确定自己的登录状态（或令牌状态）时，简单地调用退出接口而不需要担心会收到错误响应。
         * 即使用户已经是未登录状态，调用退出接口也会收到成功的响应，这对于提供良好的用户体验是有帮助的。
         * （3）减少服务器端的错误日志或异常处理：如果因为缺少令牌就抛出异常或记录错误，可能会导致日志中充满不必要的错误记录。通过直接返回成功响应，可以减少这种情况的发生。
         */
        if (StringUtils.isBlank(access_token)) {    // 判断authorization是否为空
            return ResultInfoUtil.buildSuccess(request.getServletPath(), "退出成功");
        }


        /**
         * 在OAuth 2.0和其他认证机制中，"Bearer"令牌是一种常见的方式，用于在HTTP请求的Authorization头中传递访问令牌。
         * 这个前缀用于指示后面跟着的是一个Bearer类型的令牌，这是一种表示访问令牌将被用作HTTP请求认证的方式。
         */
        // 判断 Bearer Token 是否为空
        if (access_token.toLowerCase().contains("bearer ".toLowerCase())) {
            access_token = access_token.toLowerCase().replace("bearer ", "");
        }

        // 清除redis中的token信息
        OAuth2AccessToken token = redisTokenStore.readAccessToken(access_token);
        if (token != null) {
            redisTokenStore.removeAccessToken(token);
            OAuth2RefreshToken refreshToken = token.getRefreshToken();
            redisTokenStore.removeRefreshToken(refreshToken);
        }

        return ResultInfoUtil.buildSuccess(request.getServletPath(), "退出成功");
    }
}

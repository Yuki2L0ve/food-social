package com.example.commons.utils;

import cn.hutool.core.util.StrUtil;
import com.example.commons.constant.ApiConstant;
import com.example.commons.exception.ParameterException;

/**
 * 断言工具类
 */
public class AssertUtil {

    /**
     * 必须登录
     * @param accessToken
     */
    public static void mustLogin(String accessToken) {
        if (StrUtil.isBlank(accessToken)) {
            throw new ParameterException(ApiConstant.NO_LOGIN_CODE, ApiConstant.NO_LOGIN_MESSAGE);
        }
    }

    /**
     * 判断字符串非空
     */
    public static void isNotEmpty(String str, String... message) {
        if (StrUtil.isBlank(str)) {
            execute(message);
        }
    }

    /**
     * 判断对象非空
     */
    public static void isNotNull(Object obj, String... message) {
        if (obj == null) {
            execute(message);
        }
    }

    /**
     * 判断结果是否为真
     */
    public static void isTrue(boolean isTrue, String... message) {
        if (isTrue) {
            execute(message);
        }
    }

    /**
     * 最终执行方法
     */
    private static void execute(String... message) {
        String msg = ApiConstant.ERROR_MESSAGE;
        if (message != null && message.length > 0) {
            msg = message[0];
        }
        throw new ParameterException(msg);
    }
}


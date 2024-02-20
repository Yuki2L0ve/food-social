package com.example.diners.controller;

import com.example.commons.model.domain.ResultInfo;
import com.example.commons.utils.ResultInfoUtil;
import com.example.diners.service.SignService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 * 签到控制层
 */
@RestController
@RequestMapping("/sign")
public class SignController {
    @Resource
    private SignService signService;
    @Resource
    private HttpServletRequest request;

    /**
     * 获取用户的签到情况， 默认是当月
     * @param access_token
     * @param date
     * @return
     */
    @GetMapping("/signInfo")
    public ResultInfo getSignInfo(String access_token, String date) {
        Map<String, Boolean> map = signService.getSignInfo(access_token, date);
        return ResultInfoUtil.buildSuccess(request.getServletPath(), map);
    }

    /**
     * 获取用户的签到次数，并非要连续  默认当月
     * @param access_token
     * @param date
     * @return
     */
    @GetMapping("/count")
    public ResultInfo getSignCount(String access_token, String date) {
        Long count = signService.getSignCount(access_token, date);
        return ResultInfoUtil.buildSuccess(request.getServletPath(), count);
    }

    /**
     * 签到，可以补签
     * @param access_token
     * @param date
     * @return
     */
    @PostMapping
    public ResultInfo sign(String access_token, @RequestParam(required = false) String date) {
        int count = signService.doSign(access_token, date);
        return ResultInfoUtil.buildSuccess(request.getServletPath(), count);
    }
}

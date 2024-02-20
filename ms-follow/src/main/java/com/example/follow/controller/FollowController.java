package com.example.follow.controller;

import com.example.commons.model.domain.ResultInfo;
import com.example.commons.utils.ResultInfoUtil;
import com.example.follow.service.FollowService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Set;

/**
 * 关注/取关控制层
 */
@RestController
public class FollowController {
    @Resource
    private FollowService followService;
    @Resource
    private HttpServletRequest request;

    /**
     * 获取粉丝列表
     * @param dinerId
     * @return
     */
    @GetMapping("/followers/{dinerId}")
    public ResultInfo findFollowers(@PathVariable Integer dinerId) {
        return ResultInfoUtil.buildSuccess(request.getServletPath(), followService.findFollowers(dinerId));
    }

    /**
     * 共同关注列表
     * @param dinerId
     * @param access_token
     * @return
     */
    @GetMapping("/commons/{dinerId}")
    public ResultInfo findCommonsFriends(@PathVariable Integer dinerId, String access_token) {
        return followService.findCommonsFriends(dinerId, access_token, request.getServletPath());
    }

    /**
     * 关注/取关
     * @param followDinerId     所要关注的食客对象id
     * @param isFollowed        是否关注 1=关注 0=取关
     * @param access_token       登录用户的访问token
     * @return
     */
    @PostMapping("/{followDinerId}")
    public ResultInfo follow(@PathVariable Integer followDinerId, @RequestParam int isFollowed, String access_token) {
        ResultInfo resultInfo = followService.follow(followDinerId, isFollowed, access_token, request.getServletPath());
        return resultInfo;
    }
}

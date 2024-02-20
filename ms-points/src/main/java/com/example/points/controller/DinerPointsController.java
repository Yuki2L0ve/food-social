package com.example.points.controller;

import com.example.commons.model.domain.ResultInfo;
import com.example.commons.model.vo.DinerPointsRankVO;
import com.example.commons.utils.ResultInfoUtil;
import com.example.points.service.DinerPointsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * 积分控制层
 */
@RestController
public class DinerPointsController {
    @Resource
    private DinerPointsService dinerPointsService;
    @Resource
    private HttpServletRequest request;

    /**
     * 添加积分
     *
     * @param dinerId
     * @param points
     * @param types   0=签到，1=关注好友，2=添加Feed，3=添加商户评论
     * @return
     */
    @PostMapping
    public ResultInfo<Integer> addPoints(@RequestParam(required = false) Integer dinerId,
                                         @RequestParam(required = false) Integer points,
                                         @RequestParam(required = false) Integer types) {
        dinerPointsService.addPoints(dinerId, points, types);
        return ResultInfoUtil.buildSuccess(request.getServletPath(), points);
    }

    /**
     * 查询积分排行榜的前TOPN，并显示个人排名   Redis实现
     * @return
     */
    @GetMapping("/redis")
    public ResultInfo findDinerPointRankFromRedis(String access_token) {
        List<DinerPointsRankVO> ranks = dinerPointsService.findDinerPointRankFromRedis(access_token);
        return ResultInfoUtil.buildSuccess(request.getServletPath(), ranks);
    }

    /**
     * 查询积分排行榜的前TOPN，并显示个人排名   MySQL实现
     * @return
     */
    @GetMapping
    public ResultInfo findDinerPointRank(String access_token) {
        List<DinerPointsRankVO> ranks = dinerPointsService.findDinerPointRank(access_token);
        return ResultInfoUtil.buildSuccess(request.getServletPath(), ranks);
    }
}

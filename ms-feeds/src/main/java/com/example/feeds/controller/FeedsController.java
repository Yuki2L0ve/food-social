package com.example.feeds.controller;

import com.example.commons.model.domain.ResultInfo;
import com.example.commons.model.pojo.Feeds;
import com.example.commons.model.vo.FeedsVo;
import com.example.commons.utils.ResultInfoUtil;
import com.example.feeds.service.FeedsService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

@RestController
public class FeedsController {
    @Resource
    private FeedsService feedsService;
    @Resource
    private HttpServletRequest request;

    /**
     * 分页获取关注的Feed数据
     * @param page
     * @param access_token
     * @return
     */
    @GetMapping("/{page}")
    public ResultInfo selectForPage(@PathVariable Integer page, String access_token) {
        List<FeedsVo> feedsVos = feedsService.selectForPage(page, access_token);
        return ResultInfoUtil.buildSuccess(request.getServletPath(), feedsVos);
    }

    /**
     * 变更Feed
     * @param followingDinerId
     * @param access_token
     * @param type
     * @return
     */
    @PostMapping("/updateFollowingFeeds/{followingDinerId}")
    public ResultInfo updateFollowingFeeds(@PathVariable Integer followingDinerId, String access_token, @RequestParam int type) {
        feedsService.updateFollowingFeeds(followingDinerId, access_token, type);
        return ResultInfoUtil.buildSuccess(request.getServletPath(), "操作成功");
    }

    /**
     * 删除Feed
     * @param id
     * @param access_token
     * @return
     */
    @DeleteMapping("/{id}")
    public ResultInfo delete(@PathVariable Integer id, String access_token) {
        feedsService.delete(id, access_token);
        return ResultInfoUtil.buildSuccess(request.getServletPath(), "删除成功");
    }

    /**
     * 添加Feed
     * @param feeds
     * @param access_token
     * @return
     */
    @PostMapping
    public ResultInfo<String> create(@RequestBody Feeds feeds, String access_token) {
        feedsService.create(feeds, access_token);
        return ResultInfoUtil.buildSuccess(request.getServletPath(), "添加成功");
    }
}

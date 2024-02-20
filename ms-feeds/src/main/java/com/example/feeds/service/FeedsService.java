package com.example.feeds.service;

import cn.hutool.core.bean.BeanUtil;
import com.example.commons.constant.ApiConstant;
import com.example.commons.constant.RedisKeyConstant;
import com.example.commons.exception.ParameterException;
import com.example.commons.model.domain.ResultInfo;
import com.example.commons.model.pojo.Feeds;
import com.example.commons.model.vo.FeedsVo;
import com.example.commons.model.vo.ShortDinerInfo;
import com.example.commons.model.vo.SignInDinerInfo;
import com.example.commons.utils.AssertUtil;
import com.example.feeds.mapper.FeedsMapper;
import com.google.common.collect.Lists;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.DefaultTypedTuple;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class FeedsService {
    @Value("${service.name.ms-oauth-server}")
    private String oauthServerName;
    @Value("${service.name.ms-follow-server}")
    private String followerServerName;
    @Value("${service.name.ms-diners-server}")
    private String dinersServerName;
    @Resource
    private RedisTemplate redisTemplate;
    @Resource
    private RestTemplate restTemplate;
    @Resource
    private FeedsMapper feedsMapper;

    /**
     * 根据时间由近至远，每次查询20条Feed
     * @param page
     * @param accessToken
     * @return
     */
    public List<FeedsVo> selectForPage(Integer page, String accessToken) {
        if (page == null) {
            page = 1;
        }
        // 获取登录用户
        SignInDinerInfo dinerInfo = loadSignInDinerInfo(accessToken);
        // 我关注的好友的Feed key
        String key = RedisKeyConstant.follwing_feeds.getKey() + dinerInfo.getId();
        // SortedSet中的ZREVRANGE是闭区间  比如[0,19]
        long start = (page - 1) * ApiConstant.PAGE_SIZE;
        long end = page * ApiConstant.PAGE_SIZE - 1;
        Set<Integer> feedIds = redisTemplate.opsForZSet().reverseRange(key, start, end);
        if (feedIds == null || feedIds.isEmpty()) {
            return Lists.newArrayList();
        }
        // 根据多主键查询Feed
        List<Feeds> feeds = feedsMapper.findFeedsByIds(feedIds);
        // 初始化关注好友ID集合
        List<Integer> followingDinerIds = new ArrayList<>();
        // 添加用户ID至集合中，顺带将Feeds转为VO对象
        List<FeedsVo> feedsVos = feeds.stream().map(feed -> {
            FeedsVo feedsVo = new FeedsVo();
            BeanUtil.copyProperties(feed, feedsVo);
            // 添加用户ID
            followingDinerIds.add(feed.getFkDinerId());
            return feedsVo;
        }).collect(Collectors.toList());
        // 远程调用获取Feed中的用户信息
        ResultInfo resultInfo = restTemplate.getForObject(dinersServerName + "/findByIds?access_token={accessToken}&ids={ids}",
                ResultInfo.class, accessToken, followingDinerIds);
        if (resultInfo.getCode() != ApiConstant.SUCCESS_CODE) {
            throw new ParameterException(resultInfo.getCode(), resultInfo.getMessage());
        }
        List<LinkedHashMap> dinerInfoMaps = (List<LinkedHashMap>) resultInfo.getData();
        // 构建一个 Key 为用户 ID，Value 为 ShortDinerInfo 的 Map
        Map<Integer, ShortDinerInfo> dinerInfos = dinerInfoMaps.stream()
                .collect(Collectors.toMap(
                        // key
                        diner -> (Integer) diner.get("id"),
                        // value
                        diner -> BeanUtil.fillBeanWithMap(diner, new ShortDinerInfo(), true)
                ));
        // 循环 vo 集合，根据用户 ID 从 Map 中获取用户信息并设置至 vo 对象
        feedsVos.forEach(feedsVo -> feedsVo.setDinerInfo(dinerInfos.get(feedsVo.getFkDinerId())));

        return feedsVos;
    }

    /**
     * 变更Feed
     * @param followingDinerId  关注的好友ID
     * @param accessToken       登录用户token
     * @param type              0 = 取关， 1 = 关注
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateFollowingFeeds(Integer followingDinerId, String accessToken, int type) {
        // 请选择关注的好友
        AssertUtil.isTrue(followingDinerId == null || followingDinerId < 1, "请选择关注的好友");
        // 获取登录用户信息
        SignInDinerInfo dinerInfo = loadSignInDinerInfo(accessToken);
        // 获取关注/取关的食客的所有Feed
        List<Feeds> feedsList = feedsMapper.findByDinerId(followingDinerId);
        String key = RedisKeyConstant.follwing_feeds.getKey() + dinerInfo.getId();
        if (type == 0) {    // 取关
            List<Integer> feedIds = feedsList.stream().map(feed -> feed.getId()).collect(Collectors.toList());
            redisTemplate.opsForZSet().remove(key, feedIds.toArray(new Integer[]{}));
        } else {            // 关注
            Set<ZSetOperations.TypedTuple> typedTuples = feedsList.stream()
                    .map(feed -> new DefaultTypedTuple<>(feed.getId(), (double) feed.getUpdateDate().getTime()))
                    .collect(Collectors.toSet());
            redisTemplate.opsForZSet().add(key, typedTuples);
        }
    }

    /**
     * 删除Feed
     * @param id  这里的id指的是mysql中t_feeds表中的字段id
     * @param accessToken
     */
    @Transactional(rollbackFor = Exception.class)
    public void delete(Integer id, String accessToken) {
        // 请选择要删除的Feed
        AssertUtil.isTrue(id == null || id < 1, "请选择要删除的Feed");
        // 获取登录用户
        SignInDinerInfo dinerInfo = loadSignInDinerInfo(accessToken);
        // 获取Feed内容
        Feeds feeds = feedsMapper.findById(id);
        // 判断Feed是否已经被删除且只能删除自己的Feed
        AssertUtil.isTrue(feeds == null, "该Feed已经被删除了");
        AssertUtil.isTrue(!feeds.getFkDinerId().equals(dinerInfo.getId()), "只能删除自己的feed");
        // 逻辑删除
        int count = feedsMapper.delete(id);
        if (count == 0) return ;
        // 将内容从粉丝的集合中删除  --> 异步消息队列优化
        // 先获取我的粉丝
        List<Integer> followers = findFollwers(dinerInfo.getId());
        // 然后把redis中的Feed内容清空
        followers.forEach(followerId -> {
            String key = RedisKeyConstant.follwing_feeds.getKey() + followerId;
            redisTemplate.opsForZSet().remove(key, feeds.getId());
        });
    }

    /**
     * 添加Feed
     * @param feeds
     * @param accessToken
     */
    @Transactional(rollbackFor = Exception.class)
    public void create(Feeds feeds, String accessToken) {
        // 校验  Feed内容不能为空，不能太长
        AssertUtil.isNotEmpty(feeds.getContent(), "请输入内容");
        AssertUtil.isTrue(feeds.getContent().length() > 255, "输入内容太多，请重新输入");
        // 获取登录用户信息
        SignInDinerInfo dinerInfo = loadSignInDinerInfo(accessToken);
        // Feed 关联用户信息
        feeds.setFkDinerId(dinerInfo.getId());
        // 添加Feed
        int count = feedsMapper.save(feeds);
        AssertUtil.isTrue(count == 0, "添加失败");
        // 推送到粉丝的列表中 -- 后续这里应该采用异步消息队列解决性能问题
        // 先获取粉丝列表
        List<Integer> followers = findFollwers(dinerInfo.getId());
        // 推送Feed
        long now = System.currentTimeMillis();
        followers.forEach(followerId -> {
            String key = RedisKeyConstant.follwing_feeds.getKey() + followerId;
            redisTemplate.opsForZSet().add(key, feeds.getId(), now);
        });
    }

    /**
     * 获取粉丝列表
     * @param dinerId
     * @return
     */
    private List<Integer> findFollwers(Integer dinerId) {
        String url = followerServerName + "/followers/" + dinerId;
        ResultInfo resultInfo = restTemplate.getForObject(url, ResultInfo.class);
        if (resultInfo.getCode() != ApiConstant.SUCCESS_CODE) {
            throw new ParameterException(resultInfo.getCode(), resultInfo.getMessage());
        }
        List<Integer> followers = (List<Integer>) resultInfo.getData();
        return followers;
    }

    /**
     * 获取登录用户信息
     * @param accessToken
     * @return
     */
    private SignInDinerInfo loadSignInDinerInfo(String accessToken) {
        // 必须登录
        AssertUtil.mustLogin(accessToken);
        String url = oauthServerName + "user/me?access_token={accessToken}";
        ResultInfo resultInfo = restTemplate.getForObject(url, ResultInfo.class, accessToken);
        if (resultInfo.getCode() != ApiConstant.SUCCESS_CODE) {
            throw new ParameterException(resultInfo.getCode(), resultInfo.getMessage());
        }
        SignInDinerInfo dinerInfo = BeanUtil.fillBeanWithMap(((LinkedHashMap) resultInfo.getData()), new SignInDinerInfo(), false);
        return dinerInfo;
    }
}

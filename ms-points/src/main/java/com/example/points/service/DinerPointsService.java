package com.example.points.service;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.example.commons.constant.ApiConstant;
import com.example.commons.constant.RedisKeyConstant;
import com.example.commons.exception.ParameterException;
import com.example.commons.model.domain.ResultInfo;
import com.example.commons.model.pojo.DinerPoints;
import com.example.commons.model.vo.DinerPointsRankVO;
import com.example.commons.model.vo.ShortDinerInfo;
import com.example.commons.model.vo.SignInDinerInfo;
import com.example.commons.utils.AssertUtil;
import com.example.points.mapper.DinerPointsMapper;
import com.google.common.collect.Lists;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import javax.annotation.Resource;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 积分业务逻辑层
 */
@Service
public class DinerPointsService {
    @Value("${service.name.ms-oauth-server}")
    private String oauthServerName;
    @Value("${service.name.ms-diners-server}")
    private String dinerServerName;
    @Resource
    private DinerPointsMapper dinerPointsMapper;
    @Resource
    private RestTemplate restTemplate;
    @Resource
    private RedisTemplate redisTemplate;
    // 排行榜TOPN
    public static final int TOPN = 20;

    /**
     * 添加积分
     *
     * @param dinerId
     * @param points
     * @param types   0=签到，1=关注好友，2=添加Feed，3=添加商户评论
     */
    @Transactional(rollbackFor = Exception.class)
    public void addPoints(Integer dinerId, Integer points, Integer types) {
        // 基本参数校验
        AssertUtil.isTrue(dinerId == null || dinerId < 1, "食客不能为空");
        AssertUtil.isTrue(points == null || points < 1, "积分不能为空");
        AssertUtil.isTrue(types == null, "请选择对应的积分类型");
        // 将积分数据插入数据库
        DinerPoints dinerPoints = new DinerPoints();
        dinerPoints.setFkDinerId(dinerId);
        dinerPoints.setPoints(points);
        dinerPoints.setTypes(types);
        dinerPointsMapper.save(dinerPoints);
        // 将积分数据也插入到redis中
        redisTemplate.opsForZSet().incrementScore(
                RedisKeyConstant.diner_points.getKey(), dinerId, points
        );
    }

    /**
     * 查询积分排行榜的前TOPN，并显示个人排名，实现Redis
     * @param accessToken
     * @return
     */
    public List<DinerPointsRankVO> findDinerPointRankFromRedis(String accessToken) {
        // 获取登录用户信息
        SignInDinerInfo dinerInfo = loadSignInDinerInfo(accessToken);
        // 统计积分排行榜
        Set<ZSetOperations.TypedTuple<Integer>> rangeWithScores = redisTemplate.opsForZSet().reverseRangeWithScores(
                RedisKeyConstant.diner_points.getKey(), 0, 19
        );
        if (rangeWithScores == null || rangeWithScores.isEmpty()) {
            return Lists.newArrayList();
        }
        // 初始化食客ID集合
        List<Integer> rankDinerIds = Lists.newArrayList();
        // 根据 key:食客id  value：积分信息  构建一个map
        Map<Integer, DinerPointsRankVO> ranksMap = new LinkedHashMap<>();
        // 初始化排名
        int rank = 1;
        // 循环处理排行榜，添加排名信息
        for (ZSetOperations.TypedTuple<Integer> rangeWithScore : rangeWithScores) {
            // 食客id
            Integer dinerId = rangeWithScore.getValue();
            // 积分
            int points = rangeWithScore.getScore().intValue();
            // 将食客id添加至食客id集合中
            rankDinerIds.add(dinerId);
            DinerPointsRankVO dinerPointsRankVO = new DinerPointsRankVO();
            dinerPointsRankVO.setId(dinerId);
            dinerPointsRankVO.setRanks(rank);
            dinerPointsRankVO.setTotal(points);
            // 将VO对象添加至Map中
            ranksMap.put(dinerId, dinerPointsRankVO);
            ++ rank;
        }
        // 获取Diners食客用户信息
        ResultInfo resultInfo = restTemplate.getForObject(dinerServerName + "findByIds?access_token=${accessToken}&ids={ids}",
                ResultInfo.class, accessToken, StrUtil.join(",", rankDinerIds));
        if (resultInfo.getCode() != ApiConstant.SUCCESS_CODE) {
            throw new ParameterException(resultInfo.getCode(), resultInfo.getMessage());
        }
        List<LinkedHashMap> dinerInfoMaps = (List<LinkedHashMap>) resultInfo.getData();
        // 完善食客昵称和头像
        for (LinkedHashMap dinerInfoMap : dinerInfoMaps) {
            ShortDinerInfo shortDinerInfo = BeanUtil.fillBeanWithMap(dinerInfoMap, new ShortDinerInfo(), false);
            DinerPointsRankVO rankVO = ranksMap.get(shortDinerInfo.getId());
            rankVO.setNickname(shortDinerInfo.getNickname());
            rankVO.setAvatarUrl(shortDinerInfo.getAvatarUrl());
        }
        // 判断个人是否在ranks中，如果在，则图上标记直接返回（这样在前端就能看到高亮显示自己）
        if (ranksMap.containsKey(dinerInfo.getId())) {
            DinerPointsRankVO myRank = ranksMap.get(dinerInfo.getId());
            myRank.setIsMe(1);
            return Lists.newArrayList(ranksMap.values());
        }
        // 如果不在ranks中，则获取个人排名在最后
        // 获取排名
        Long myRank = redisTemplate.opsForZSet().reverseRank(
                RedisKeyConstant.diner_points.getKey(), dinerInfo.getId());
        if (myRank != null) {
            DinerPointsRankVO me = new DinerPointsRankVO();
            BeanUtil.copyProperties(dinerInfo, me);
            me.setRanks(myRank.intValue() + 1);
            me.setIsMe(1);
            // 获取积分
            Double points = redisTemplate.opsForZSet().score(RedisKeyConstant.diner_points.getKey(), dinerInfo.getId());
            me.setTotal(points.intValue());
            ranksMap.put(dinerInfo.getId(), me);
        }

        return Lists.newArrayList(ranksMap.values());
    }

    /**
     * 查询积分排行榜的前TOPN，并显示个人排名, 使用MySQL
     * @param accessToken
     * @return
     */
    public List<DinerPointsRankVO> findDinerPointRank(String accessToken) {
        // 获取登录用户信息
        SignInDinerInfo dinerInfo = loadSignInDinerInfo(accessToken);
        // 统计积分排行榜
        List<DinerPointsRankVO> ranks = dinerPointsMapper.findTopN(TOPN);
        if (ranks == null || ranks.isEmpty()) {
            return Lists.newArrayList();
        }
        // 根据 key:食客id  value：积分信息  构建一个map
        Map<Integer, DinerPointsRankVO> ranksMap = new LinkedHashMap<>();
        for (int i = 0; i < ranks.size(); i++) {
            ranksMap.put(ranks.get(i).getId(), ranks.get(i));
        }
        // 判断个人是否在ranks中，如果在，则图上标记直接返回（这样在前端就能看到高亮显示自己）
        if (ranksMap.containsKey(dinerInfo.getId())) {
            DinerPointsRankVO myRank = ranksMap.get(dinerInfo.getId());
            myRank.setIsMe(1);
            return Lists.newArrayList(ranksMap.values());
        }
        // 如果不在ranks中，则获取个人排名在最后
        DinerPointsRankVO myRank = dinerPointsMapper.findDinerRank(dinerInfo.getId());
        myRank.setIsMe(1);
        ranks.add(myRank);
        return ranks;
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

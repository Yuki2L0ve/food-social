package com.example.follow.service;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.example.commons.constant.ApiConstant;
import com.example.commons.constant.RedisKeyConstant;
import com.example.commons.exception.ParameterException;
import com.example.commons.model.domain.ResultInfo;
import com.example.commons.model.pojo.Follow;
import com.example.commons.model.vo.ShortDinerInfo;
import com.example.commons.model.vo.SignInDinerInfo;
import com.example.commons.utils.AssertUtil;
import com.example.commons.utils.ResultInfoUtil;
import com.example.follow.mapper.FollowMapper;
import io.swagger.models.auth.In;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;


/**
 * 关注/取关业务逻辑层
 */
@Service
public class FollowService {
    @Value("${service.name.ms-oauth-server}")
    private String oauthServerName;
    @Value("${service.name.ms-diners-server}")
    private String dinersServerName;
    @Value("${service.name.ms-feeds-server}")
    private String feedsServerName;
    @Resource
    private RestTemplate restTemplate;
    @Resource
    private FollowMapper followMapper;
    @Resource
    private RedisTemplate redisTemplate;

    /**
     * 获取粉丝列表
     * @param dinerId 想要查询dinerId他的粉丝
     * @return
     */
    public Set<Integer> findFollowers(Integer dinerId) {
        AssertUtil.isNotNull(dinerId, "请选择要查看的用户");
        Set<Integer> followers = redisTemplate.opsForSet().members(RedisKeyConstant.followers.getKey() + dinerId);
        return followers;
    }

    /**
     * 共同关注列表
     * @param dinerId 想要查看的对象id 比如我想查询张三和我有哪些共同关注好友，那么dinerId就是张三的id
     * @param accessToken
     * @param path
     * @return
     */
    public ResultInfo findCommonsFriends(Integer dinerId, String accessToken, String path) {
        // 是否选择了关注对象
        AssertUtil.isTrue(dinerId == null || dinerId < 1, "请选择要查看的人");
        // 获取登录用户信息
        SignInDinerInfo dinerInfo = loadSignInDinerInfo(accessToken);
        // 获取登录用户的关注信息
        String loginDinerKey = RedisKeyConstant.following.getKey() + dinerInfo.getId();
        // 获取登录用户其查看对象的关注信息 (比如我想查询张三和我有哪些共同关注好友, 这里dinerKey就是张三的key）
        String dinerKey = RedisKeyConstant.following.getKey() + dinerId;
        // 计算交集
        Set<Integer> dinerIds = redisTemplate.opsForSet().intersect(loginDinerKey, dinerKey);
        // 他俩之间并没有共同的关注列表
        if (dinerIds == null || dinerIds.isEmpty()) {
            return ResultInfoUtil.buildSuccess(path, new ArrayList<ShortDinerInfo>());
        }
        // 调用食客服务 根据ids查询食客信息
        ResultInfo resultInfo = restTemplate.getForObject(dinersServerName + "findByIds?access_token={accessToken}&ids={ids}",
                ResultInfo.class, accessToken, StrUtil.join(",", dinerIds));
        if (resultInfo.getCode() != ApiConstant.SUCCESS_CODE) {
            resultInfo.setPath(path);
            return resultInfo;
        }
        // 处理结果集
        List<LinkedHashMap> dinerInfoMaps = (ArrayList) resultInfo.getData();
        List<ShortDinerInfo> dinerInfos = dinerInfoMaps.stream().
                map(diner -> BeanUtil.fillBeanWithMap(diner, new ShortDinerInfo(), true)).collect(Collectors.toList());
        return ResultInfoUtil.buildSuccess(path, dinerInfos);
    }

    /**
     * 关注/取关
     * @param followDinerId     所要关注的食客对象id
     * @param isFollowed        是否关注 1=关注 0=取关
     * @param accessToken       登录用户的访问token
     * @param path              访问地址
     * @return
     */
    public ResultInfo follow(Integer followDinerId, int isFollowed, String accessToken, String path) {
        // 是否选择了关注对象
        AssertUtil.isTrue(followDinerId == null || followDinerId < 1, "请选择要关注的人");
        // 获取登录用户信息（封装成一个方法）
        SignInDinerInfo dinerInfo = loadSignInDinerInfo(accessToken);
        // 获取当前登录用户与需要关注用户的关注信息
        Follow follow = followMapper.selectFollow(dinerInfo.getId(), followDinerId);

        // 如果从来没有关注过他，现在要关注他  --> 添加关注
        if (follow == null && isFollowed == 1) {
            // 添加关注信息
            int count = followMapper.save(dinerInfo.getId(), followDinerId);
            // 添加关注列表到redis中
            if (count == 1) {
                addToRedisSet(dinerInfo.getId(), followDinerId);
                // 添加Feed
                sendSaveOrRemoveFeed(followDinerId, accessToken, 1);
            }
            return ResultInfoUtil.build(ApiConstant.SUCCESS_CODE, "关注成功", path, "关注成功");
        }

        // 如果关注过他，且目前处于关注状态，然后要进行取关他  --> 取关
        if (follow != null && follow.getIsValid() == 1 && isFollowed == 0) {
            // 取关
            int count = followMapper.update(follow.getId(), isFollowed);
            // 从redis中移除关注列表
            if (count == 1) {
                removeFromRedisSet(dinerInfo.getId(), followDinerId);
                // 移除Feed
                sendSaveOrRemoveFeed(followDinerId, accessToken, 0);
            }
            return ResultInfoUtil.build(ApiConstant.SUCCESS_CODE, "关注取关", path, "关注取关");
        }

        // 如果曾经关注过他，且目前处于取关状态，然后又要重新关注他  -> 重新关注
        if (follow != null && follow.getIsValid() == 0 && isFollowed == 1) {
            // 重新关注
            int count = followMapper.update(follow.getId(), isFollowed);
            // 添加关注列表到redis中
            if (count == 1) {
                addToRedisSet(dinerInfo.getId(), followDinerId);
                // 添加Feed
                sendSaveOrRemoveFeed(followDinerId, accessToken, 1);
            }
            return ResultInfoUtil.build(ApiConstant.SUCCESS_CODE, "关注成功", path, "关注成功");
        }

        return ResultInfoUtil.buildSuccess(path, "操作成功");
    }

    /**
     * 添加关注列表到redis中
     * @param dinerId
     * @param followDinerId
     */
    private void addToRedisSet(Integer dinerId, Integer followDinerId) {
        redisTemplate.opsForSet().add(RedisKeyConstant.following.getKey() + dinerId, followDinerId);
        redisTemplate.opsForSet().add(RedisKeyConstant.followers.getKey() + followDinerId, dinerId);
    }

    /**
     * 从redis中移除关注列表
     * @param dinerId
     * @param followDinerId
     */
    private void removeFromRedisSet(Integer dinerId, Integer followDinerId) {
        redisTemplate.opsForSet().remove(RedisKeyConstant.following.getKey() + dinerId, followDinerId);
        redisTemplate.opsForSet().remove(RedisKeyConstant.followers.getKey() + followDinerId, dinerId);
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
            throw new ParameterException(resultInfo.getMessage());
        }
        SignInDinerInfo dinerInfo = BeanUtil.fillBeanWithMap(((LinkedHashMap) resultInfo.getData()), new SignInDinerInfo(), false);
        return dinerInfo;
    }

    /**
     * 发送请求添加或者移除关注人的Feed列表
     *
     * @param followDinerId 		关注好友的ID
     * @param accessToken  		当前登录用户token
     * @param type     			0=取关 1=关注
     */
    private void sendSaveOrRemoveFeed(Integer followDinerId, String accessToken, int type) {
        String feedsUpdateUrl = feedsServerName + "/updateFollowingFeeds/" + followDinerId + "?access_token=" + accessToken;

        // 构建请求头
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        // 构建请求体（请求参数）
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("type", type);
        HttpEntity<MultiValueMap<String, Object>> entity = new HttpEntity<>(body,headers);
        restTemplate.postForEntity(feedsUpdateUrl, entity, ResultInfo.class);
    }

}

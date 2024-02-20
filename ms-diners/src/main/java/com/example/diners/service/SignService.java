package com.example.diners.service;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.date.DateUnit;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.date.LocalDateTimeUtil;
import cn.hutool.core.util.StrUtil;
import com.example.commons.constant.ApiConstant;
import com.example.commons.constant.PointTypesConstant;
import com.example.commons.exception.ParameterException;
import com.example.commons.model.domain.ResultInfo;
import com.example.commons.model.vo.SignInDinerInfo;
import com.example.commons.utils.AssertUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.connection.BitFieldSubCommands;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 签到业务逻辑层
 */
@Service
public class SignService {
    @Value("${service.name.ms-oauth-server}")
    private String oauthServerName;
    @Value("${service.name.ms-points-server}")
    private String pointsServerName;
    @Resource
    private RestTemplate restTemplate;
    @Resource
    private RedisTemplate redisTemplate;

    /**
     * 获取用户当月的签到情况
     * @param accessToken
     * @param dateStr
     * @return
     */
    public Map<String, Boolean> getSignInfo(String accessToken, String dateStr) {
        // 获取登录用户信息
        SignInDinerInfo dinerInfo = loadSignInDinerInfo(accessToken);
        // 获取日期
        Date date = getDate(dateStr);
        // 构建key    user:sign:5:yyyyMM
        String signKey = buildSignKey(dinerInfo.getId(), date);
        // 构建一个自动排序的Map
        Map<String, Boolean> signInfo = new TreeMap<>();
        // 获取某月的总天数（考虑闰年）
        int dayOfMonth = DateUtil.lengthOfMonth(DateUtil.month(date) + 1,
                DateUtil.isLeapYear(DateUtil.dayOfYear(date)));
        // bitfield user:sign:5:202011 u29 0
        BitFieldSubCommands bitFieldSubCommands = BitFieldSubCommands.create()
                .get(BitFieldSubCommands.BitFieldType.unsigned(dayOfMonth))
                .valueAt(0);
        List<Long> list = redisTemplate.opsForValue().bitField(signKey, bitFieldSubCommands);
        if (list == null || list.isEmpty()) {
            return signInfo;
        }
        long v = list.get(0) == null ? 0 : list.get(0);
        // 从低位到高位进行遍历，为0表示未签到，为1表示已签到
        // 签到: yyyy-MM-dd true  未签到：yyyy-MM-dd false
        for (int i = dayOfMonth; i > 0; -- i) {
            // 获取日期时间, 比如i=30 那么localDateTime其实是yyyy-MM-30
            LocalDateTime localDateTime = LocalDateTimeUtil.of(date).withDayOfMonth(i);
            boolean flag = v >> 1 << 1 != v;
            // 构建一个 Key 为日期，Value 为是否签到标记
            signInfo.put(DateUtil.format(localDateTime, "yyyy-MM-dd"), flag);
            v >>= 1;
        }
        return signInfo;
    }

    /**
     * 用户签到，可以补签
     * @param accessToken    登录用户 token
     * @param dateStr       日期，默认当天
     * @return              签到积分
     */
    public int doSign(String accessToken, String dateStr) {
        // 获取登录用户信息
        SignInDinerInfo dinerInfo = loadSignInDinerInfo(accessToken);
        // 获取日期
        Date date = getDate(dateStr);
        // 获取日期对应的天数，具体是哪一天多少号
        int offset = DateUtil.dayOfMonth(date) - 1;     // Bitmaps下标从0开始
        // 构建key    user:sign:5:yyyyMM
        String signKey = buildSignKey(dinerInfo.getId(), date);
        // 查看指定日期是否已签到
        boolean isSigned = redisTemplate.opsForValue().getBit(signKey, offset);
        AssertUtil.isTrue(isSigned, "当前日期已完成签到，无需再签");
        // 进行签到
        redisTemplate.opsForValue().setBit(signKey, offset, true);
        // 统计连续签到的次数
        int count = getContinuousSignCount(dinerInfo.getId(), date);
        // 用户签到后则赠送积分
        int points = addPoints(count, dinerInfo.getId());
        return points;
    }

    /**
     * 获取用户的签到次数，并不一定是连续的
     * @param accessToken
     * @param dateStr
     * @return
     */
    public long getSignCount(String accessToken, String dateStr) {
        // 获取登录用户信息
        SignInDinerInfo dinerInfo = loadSignInDinerInfo(accessToken);
        // 获取日期
        Date date = getDate(dateStr);
        // 构建key
        String signKey = buildSignKey(dinerInfo.getId(), date);
        // 进行统计
        return (Long) redisTemplate.execute(
                (RedisCallback<Long>) con -> con.bitCount(signKey.getBytes())
        );
    }

    /**
     * 统计连续签到的次数
     * @param dinerId
     * @param date
     * @return
     */
    private int getContinuousSignCount(Integer dinerId, Date date) {
        // 获取日期对应的天数，多少号，假设dayOfMonth是31
        int dayOfMonth = DateUtil.dayOfMonth(date);
        // 构建key
        String signKey = buildSignKey(dinerId, date);
        // 命令：bitfield key get [u/i]dayOfMonth offset
        BitFieldSubCommands bitFieldSubCommands = BitFieldSubCommands.create()
                .get(BitFieldSubCommands.BitFieldType.unsigned(dayOfMonth))
                .valueAt(0);
        List<Long> list = redisTemplate.opsForValue().bitField(signKey, bitFieldSubCommands);
        if (list == null || list.isEmpty()) {
            return 0;
        }
        int signCount = 0;
        long v = list.get(0) == null ? 0 : list.get(0);
        // 取低位连续不为 0 的个数即为连续签到次数，需考虑当天尚未签到的情况
        for (int i = dayOfMonth; i > 0; -- i) {     // i表示位移操作次数
            /*
                  原：1111000111
             右移一位：0111100011
           再左移一位：1111000110
           右移一位再左移一位如果不等于自己，表示低位是 1，签到，计数器++
           右移一位再左移一位如果等于自己，表示低位是 0，未签到
           */
            if (v >> 1 << 1 == v) { // 右移再左移，如果等于自己则说明最低位是0，表示未签到
                // 该位是0并且非当天，则说明连续签到中断了
                if (i != dayOfMonth) break;
            } else {
                ++ signCount;   // 签到了，计数器++
            }
            v >>= 1;    // 比如现在v=31, 那么右移一位，就是再看v=30
        }
        return signCount;
    }

    /**
     * 构建key  user:sign:5:yyyyMM
     * @param dinerId
     * @param date
     * @return
     */
    private String buildSignKey(Integer dinerId, Date date) {
        return String.format("user:sign:%d:%s", dinerId, DateUtil.format(date, "yyyyMM"));
    }

    /**
     * 获取日期
     * @param dateStr
     * @return
     */
    public Date getDate(String dateStr) {
        if (StrUtil.isBlank(dateStr)) {
            return new Date();
        }
        try {
            return DateUtil.parseDate(dateStr);
        } catch (Exception e) {
            throw new ParameterException("请传入yyyy-MM-dd的日期格式");
        }
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
        if (dinerInfo == null) {
            throw new ParameterException(ApiConstant.NO_LOGIN_CODE, ApiConstant.NO_LOGIN_MESSAGE);
        }
        return dinerInfo;
    }

    /**
     * 添加用户积分
     * @param count			连续签到次数
     * @param dinerId		登录用户 ID
     * @return				获取的积分
     */
    private int addPoints(int count, Integer dinerId) {
        // 签到 1 天送 10 积分，连续签到 2 天送 20 积分，3 天送 30 积分
        // 4 天以及以上均送 50 积分
        int points = 10;
        if (count == 2) {
            points = 20;
        } else if (count == 3) {
            points = 30;
        } else if (count >= 4) {
            points = 50;
        }
        // 调用积分服务添加积分
        // 构建请求头
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        // 构建请求体（请求参数）
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("dinerId", dinerId);
        body.add("points", points);
        body.add("types", PointTypesConstant.sign.getType());
        HttpEntity<MultiValueMap<String, Object>> entity = new HttpEntity<>(body, headers);
        // 发送请求
        ResponseEntity<ResultInfo> result = restTemplate.postForEntity(pointsServerName, entity, ResultInfo.class);
        AssertUtil.isTrue(result.getStatusCode() != HttpStatus.OK, "登录失败");
        ResultInfo resultInfo = result.getBody();
        if (resultInfo.getCode() != ApiConstant.SUCCESS_CODE) {
            // 失败了, 事物要进行回滚
            throw new ParameterException(resultInfo.getCode(), resultInfo.getMessage());
        }
        return points;
    }

}

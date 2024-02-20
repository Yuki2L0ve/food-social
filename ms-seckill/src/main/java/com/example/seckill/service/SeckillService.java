package com.example.seckill.service;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.IdUtil;
import com.example.commons.constant.ApiConstant;
import com.example.commons.constant.RedisKeyConstant;
import com.example.commons.exception.ParameterException;
import com.example.commons.model.domain.ResultInfo;
import com.example.commons.model.pojo.SeckillVouchers;
import com.example.commons.model.pojo.VoucherOrders;
import com.example.commons.model.vo.SignInDinerInfo;
import com.example.commons.utils.AssertUtil;
import com.example.commons.utils.ResultInfoUtil;
import com.example.seckill.mapper.SeckillVouchersMapper;
import com.example.seckill.mapper.VoucherOrdersMapper;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;
import org.springframework.web.client.RestTemplate;

import javax.annotation.Resource;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * 秒杀业务逻辑层
 */
@Service
public class SeckillService {
    @Resource
    private SeckillVouchersMapper seckillVouchersMapper;
    @Resource
    private VoucherOrdersMapper voucherOrdersMapper;
    @Value("${service.name.ms-oauth-server}")
    private String oauthServerName;
    @Resource
    private RestTemplate restTemplate;
    @Resource
    private RedisTemplate redisTemplate;
    @Resource
    private DefaultRedisScript defaultRedisScript;
    @Resource
    private RedissonClient redissonClient;

//    /**
//     * 添加需要抢购的代金券   即商家发布抢购活动（抢购活动是由商家发布的，跟用户没有关系）
//     * @param seckillVouchers
//     */
//    @Transactional(rollbackFor = Exception.class)
//    public void addSeckillVouchers(SeckillVouchers seckillVouchers) {
//        // 非空校验
//        AssertUtil.isTrue(seckillVouchers.getFkVoucherId() == null, "请选择需要抢购的代金券");
//        AssertUtil.isTrue(seckillVouchers.getAmount() == 0, "请输入抢购总数量");
//        Date now = new Date();
//        AssertUtil.isNotNull(seckillVouchers.getStartTime(), "请输入开始时间");
//        // 生产环境下面一行代码需放行，这里注释方便测试
//        // AssertUtil.isTrue(now.after(seckillVouchers.getStartTime()), "开始时间不能早于当前时间");
//        AssertUtil.isNotNull(seckillVouchers.getEndTime(), "请输入结束时间");
//        AssertUtil.isTrue(now.after(seckillVouchers.getEndTime()), "结束时间不能早于当前时间");
//        AssertUtil.isTrue(seckillVouchers.getStartTime().after(seckillVouchers.getEndTime()), "开始时间不能晚于结束时间");
//
//        // 验证数据库中是否已经存在该券的秒杀活动
//        SeckillVouchers seckillVouchersFromDb = seckillVouchersMapper.selectVoucher(seckillVouchers.getFkVoucherId());
//        AssertUtil.isTrue(seckillVouchersFromDb != null, "该券已经拥有了抢购活动");
//        // 插入数据库
//        seckillVouchersMapper.save(seckillVouchers);
//    }
//
//    /** 原始的走关系型数据库的流程
//     * 抢购代金券   即客户开始秒杀抢购代金券
//     *
//     * @param voucherId   代金券 ID
//     * @param accessToken 登录token
//     * @Para path 访问路径
//     */
//    @Transactional(rollbackFor = Exception.class)
//    public ResultInfo doSeckill(Integer voucherId, String accessToken, String path) {
//        // 基本参数校验
//        AssertUtil.isTrue(voucherId == null || voucherId < 0, "请选择需要抢购的代金券");
//        AssertUtil.isNotEmpty(accessToken, "请登录");
//
//        // 判断此代金券是否加入抢购
//        SeckillVouchers seckillVouchers = seckillVouchersMapper.selectVoucher(voucherId);
//        AssertUtil.isTrue(seckillVouchers == null, "该代金券并未有抢购活动");;
//        // 判断是否有效
//        AssertUtil.isTrue(seckillVouchers.getIsValid() == 0, "该活动已结束");
//        // 判断是否开始、结束
//        Date now = new Date();
//        AssertUtil.isTrue(now.before(seckillVouchers.getStartTime()), "该抢购还未开始");
//        AssertUtil.isTrue(now.after(seckillVouchers.getEndTime()), "该抢购已结束");
//        // 判断是否卖完
//        AssertUtil.isTrue(seckillVouchers.getAmount() < 1, "该券已经卖完啦");
//        // 获取登录用户信息
//        String url = oauthServerName + "user/me?access_token={accessToken}";
//        ResultInfo resultInfo = restTemplate.getForObject(url, ResultInfo.class, accessToken);
//        if (resultInfo.getCode() != ApiConstant.SUCCESS_CODE) {
//            resultInfo.setPath(path);
//            return resultInfo;
//        }
//
//        // 这里的data是一个LinkedHashMap，SignInDinerInfo
//        SignInDinerInfo dinerInfo = BeanUtil.fillBeanWithMap((LinkedHashMap) resultInfo.getData(), new SignInDinerInfo(), false);
//        // 判断登录用户是否已抢到(一个用户针对这次活动只能买一次)
//        VoucherOrders order = voucherOrdersMapper.findDinerOrder(dinerInfo.getId(), seckillVouchers.getFkVoucherId());
//        AssertUtil.isTrue(order != null, "该用户已抢到该代金券，无需再抢");
//
//        // 扣库存
//        int count = seckillVouchersMapper.stockDecrease(seckillVouchers.getId());
//        AssertUtil.isTrue(count == 0, "该券已经卖完啦");
//
//        // 下单
//        VoucherOrders voucherOrders = new VoucherOrders();
//        voucherOrders.setFkDinerId(dinerInfo.getId());
//        voucherOrders.setFkSeckillId(seckillVouchers.getId());
//        voucherOrders.setFkVoucherId(seckillVouchers.getFkVoucherId());
//        String orderNo = IdUtil.getSnowflake(1, 1).nextIdStr();
//        voucherOrders.setOrderNo(orderNo);
//        voucherOrders.setOrderType(1);
//        voucherOrders.setStatus(0);
//        count = voucherOrdersMapper.save(voucherOrders);
//        AssertUtil.isTrue(count == 0, "用户抢购失败");
//
//        return ResultInfoUtil.buildSuccess(path, "抢购成功");
//    }

//    /** 下面的这两个方法 采用redis实现 解决了mysql中t_vouchers_orders订单表中能保持100条记录，而不会超过100条，即不会有订单超卖问题
//     * 但是redis中抢购活动表中库存量amount仍然会小于0，即库存仍然多扣
//     * 添加需要抢购的代金券   即商家发布抢购活动
//     * @param seckillVouchers
//     */
//    @Transactional(rollbackFor = Exception.class)
//    public void addSeckillVouchers(SeckillVouchers seckillVouchers) {
//        // 非空校验
//        AssertUtil.isTrue(seckillVouchers.getFkVoucherId() == null, "请选择需要抢购的代金券");
//        AssertUtil.isTrue(seckillVouchers.getAmount() == 0, "请输入抢购总数量");
//        Date now = new Date();
//        AssertUtil.isNotNull(seckillVouchers.getStartTime(), "请输入开始时间");
//        // 生产环境下面一行代码需放行，这里注释方便测试
//        // AssertUtil.isTrue(now.after(seckillVouchers.getStartTime()), "开始时间不能早于当前时间");
//        AssertUtil.isNotNull(seckillVouchers.getEndTime(), "请输入结束时间");
//        AssertUtil.isTrue(now.after(seckillVouchers.getEndTime()), "结束时间不能早于当前时间");
//        AssertUtil.isTrue(seckillVouchers.getStartTime().after(seckillVouchers.getEndTime()), "开始时间不能晚于结束时间");
//
//        String key = RedisKeyConstant.seckill_vouchers.getKey() + seckillVouchers.getFkVoucherId();
//        // 验证数据库中是否已经存在该券的秒杀活动
//        Map<String, Object> map = redisTemplate.opsForHash().entries(key);
//        AssertUtil.isTrue(!map.isEmpty() && (int) map.get("amount") > 0, "该券已经拥有了抢购活动");
//        // 插入到redis
//        seckillVouchers.setIsValid(1);
//        seckillVouchers.setCreateDate(now);
//        seckillVouchers.setUpdateDate(now);
//        redisTemplate.opsForHash().putAll(key, BeanUtil.beanToMap(seckillVouchers));
//    }
//
//    /**
//     * 抢购代金券   即客户开始秒杀抢购代金券
//     * @param voucherId   代金券 ID
//     * @param accessToken 登录token
//     * @Para path 访问路径
//     */
//    @Transactional(rollbackFor = Exception.class)
//    public ResultInfo doSeckill(Integer voucherId, String accessToken, String path) {
//        // 基本参数校验
//        AssertUtil.isTrue(voucherId == null || voucherId < 0, "请选择需要抢购的代金券");
//        AssertUtil.isNotEmpty(accessToken, "请登录");
//
//        String key = RedisKeyConstant.seckill_vouchers.getKey() + voucherId;
//        Map<String, Object> map = redisTemplate.opsForHash().entries(key);
//        SeckillVouchers seckillVouchers = BeanUtil.mapToBean(map, SeckillVouchers.class, true, null);
//
//        // 判断是否开始、结束
//        Date now = new Date();
//        AssertUtil.isTrue(now.before(seckillVouchers.getStartTime()), "该抢购还未开始");
//        AssertUtil.isTrue(now.after(seckillVouchers.getEndTime()), "该抢购已结束");
//        // 判断是否卖完
//        AssertUtil.isTrue(seckillVouchers.getAmount() < 1, "该券已经卖完啦");
//        // 获取登录用户信息
//        String url = oauthServerName + "user/me?access_token={accessToken}";
//        ResultInfo resultInfo = restTemplate.getForObject(url, ResultInfo.class, accessToken);
//        if (resultInfo.getCode() != ApiConstant.SUCCESS_CODE) {
//            resultInfo.setPath(path);
//            return resultInfo;
//        }
//
//        // 这里的data是一个LinkedHashMap，SignInDinerInfo
//        SignInDinerInfo dinerInfo = BeanUtil.fillBeanWithMap((LinkedHashMap) resultInfo.getData(), new SignInDinerInfo(), false);
//        // 判断登录用户是否已抢到(一个用户针对这次活动只能买一次)
//        VoucherOrders order = voucherOrdersMapper.findDinerOrder(dinerInfo.getId(), seckillVouchers.getFkVoucherId());
//        AssertUtil.isTrue(order != null, "该用户已抢到该代金券，无需再抢");
//
//        // 下单
//        VoucherOrders voucherOrders = new VoucherOrders();
//        voucherOrders.setFkDinerId(dinerInfo.getId());
//        // 在redis中我们并不需要维护这个外键信息
//        //voucherOrders.setFkSeckillId(seckillVouchers.getId());
//        voucherOrders.setFkVoucherId(seckillVouchers.getFkVoucherId());
//        String orderNo = IdUtil.getSnowflake(1, 1).nextIdStr();
//        voucherOrders.setOrderNo(orderNo);
//        voucherOrders.setOrderType(1);
//        voucherOrders.setStatus(0);
//        long count = voucherOrdersMapper.save(voucherOrders);
//        AssertUtil.isTrue(count == 0, "用户抢购失败");
//
//        // 采用redis扣库存
//        count = redisTemplate.opsForHash().increment(key, "amount", -1);
//        AssertUtil.isTrue(count < 0, "该券已经卖完啦");
//
//        return ResultInfoUtil.buildSuccess(path, "抢购成功");
//    }

//    /** 下面的这两个方法 采用redis实现 解决了库存量amout<0，并且保证t_vouchers_orders订单数是100的问题。
//     * 也就是说解决了"多个用户抢购代金券"问题，但是还没有解决"一人一单"问题
//     * 添加需要抢购的代金券   即商家发布抢购活动
//     * @param seckillVouchers
//     */
//    @Transactional(rollbackFor = Exception.class)
//    public void addSeckillVouchers(SeckillVouchers seckillVouchers) {
//        // 非空校验
//        AssertUtil.isTrue(seckillVouchers.getFkVoucherId() == null, "请选择需要抢购的代金券");
//        AssertUtil.isTrue(seckillVouchers.getAmount() == 0, "请输入抢购总数量");
//        Date now = new Date();
//        AssertUtil.isNotNull(seckillVouchers.getStartTime(), "请输入开始时间");
//        // 生产环境下面一行代码需放行，这里注释方便测试
//        // AssertUtil.isTrue(now.after(seckillVouchers.getStartTime()), "开始时间不能早于当前时间");
//        AssertUtil.isNotNull(seckillVouchers.getEndTime(), "请输入结束时间");
//        AssertUtil.isTrue(now.after(seckillVouchers.getEndTime()), "结束时间不能早于当前时间");
//        AssertUtil.isTrue(seckillVouchers.getStartTime().after(seckillVouchers.getEndTime()), "开始时间不能晚于结束时间");
//
//        String key = RedisKeyConstant.seckill_vouchers.getKey() + seckillVouchers.getFkVoucherId();
//        // 验证数据库中是否已经存在该券的秒杀活动
//        Map<String, Object> map = redisTemplate.opsForHash().entries(key);
//        AssertUtil.isTrue(!map.isEmpty() && (int) map.get("amount") > 0, "该券已经拥有了抢购活动");
//        // 插入到redis
//        seckillVouchers.setIsValid(1);
//        seckillVouchers.setCreateDate(now);
//        seckillVouchers.setUpdateDate(now);
//        redisTemplate.opsForHash().putAll(key, BeanUtil.beanToMap(seckillVouchers));
//    }
//
//    /**
//     * 抢购代金券   即客户开始秒杀抢购代金券
//     * @param voucherId   代金券 ID
//     * @param accessToken 登录token
//     * @Para path 访问路径
//     */
//    @Transactional(rollbackFor = Exception.class)
//    public ResultInfo doSeckill(Integer voucherId, String accessToken, String path) {
//        // 基本参数校验
//        AssertUtil.isTrue(voucherId == null || voucherId < 0, "请选择需要抢购的代金券");
//        AssertUtil.isNotEmpty(accessToken, "请登录");
//
//        String key = RedisKeyConstant.seckill_vouchers.getKey() + voucherId;
//        Map<String, Object> map = redisTemplate.opsForHash().entries(key);
//        SeckillVouchers seckillVouchers = BeanUtil.mapToBean(map, SeckillVouchers.class, true, null);
//
//        // 判断是否开始、结束
//        Date now = new Date();
//        AssertUtil.isTrue(now.before(seckillVouchers.getStartTime()), "该抢购还未开始");
//        AssertUtil.isTrue(now.after(seckillVouchers.getEndTime()), "该抢购已结束");
//        // 判断是否卖完
//        AssertUtil.isTrue(seckillVouchers.getAmount() < 1, "该券已经卖完啦");
//        // 获取登录用户信息
//        String url = oauthServerName + "user/me?access_token={accessToken}";
//        ResultInfo resultInfo = restTemplate.getForObject(url, ResultInfo.class, accessToken);
//        if (resultInfo.getCode() != ApiConstant.SUCCESS_CODE) {
//            resultInfo.setPath(path);
//            return resultInfo;
//        }
//
//        // 这里的data是一个LinkedHashMap，SignInDinerInfo
//        SignInDinerInfo dinerInfo = BeanUtil.fillBeanWithMap((LinkedHashMap) resultInfo.getData(), new SignInDinerInfo(), false);
//        // 判断登录用户是否已抢到(一个用户针对这次活动只能买一次)
//        VoucherOrders order = voucherOrdersMapper.findDinerOrder(dinerInfo.getId(), seckillVouchers.getFkVoucherId());
//        AssertUtil.isTrue(order != null, "该用户已抢到该代金券，无需再抢");
//
//        // 下单
//        VoucherOrders voucherOrders = new VoucherOrders();
//        voucherOrders.setFkDinerId(dinerInfo.getId());
//        // 在redis中我们并不需要维护这个外键信息
//        //voucherOrders.setFkSeckillId(seckillVouchers.getId());
//        voucherOrders.setFkVoucherId(seckillVouchers.getFkVoucherId());
//        String orderNo = IdUtil.getSnowflake(1, 1).nextIdStr();
//        voucherOrders.setOrderNo(orderNo);
//        voucherOrders.setOrderType(1);
//        voucherOrders.setStatus(0);
//        long count = voucherOrdersMapper.save(voucherOrders);
//        AssertUtil.isTrue(count == 0, "用户抢购失败");
//
//        // 采用 redis+lua脚本 实现扣库存
//        List<String> keys = new ArrayList<>();
//        keys.add(key);
//        keys.add("amount");
//        Long amount = (Long) redisTemplate.execute(defaultRedisScript, keys);
//        AssertUtil.isTrue(amount == null || amount < 1, "该券已经卖完啦");
//
//        return ResultInfoUtil.buildSuccess(path, "抢购成功");
//    }

    /** 下面的这两个方法 采用redis实现 解决了库存量amount<0，并且保证t_vouchers_orders订单数是100的问题。
     * 也就是说解决了"多个用户抢购代金券"问题，并且也解决"一人一单"问题
     * 添加需要抢购的代金券   即商家发布抢购活动
     *
     * 添加代金券的具体逻辑：
     * （1）进行非空校验
     * （2）验证数据库中是否已经存在该券的秒杀活动
     * （3）插入数据库
     * @param seckillVouchers
     */
    @Transactional(rollbackFor = Exception.class)
    public void addSeckillVouchers(SeckillVouchers seckillVouchers) {
        // 非空校验
        AssertUtil.isTrue(seckillVouchers.getFkVoucherId() == null, "请选择需要抢购的代金券");
        AssertUtil.isTrue(seckillVouchers.getAmount() == 0, "请输入抢购总数量");
        Date now = new Date();
        AssertUtil.isNotNull(seckillVouchers.getStartTime(), "请输入开始时间");
        // 生产环境下面一行代码需放行，这里注释方便测试
        // AssertUtil.isTrue(now.after(seckillVouchers.getStartTime()), "开始时间不能早于当前时间");
        AssertUtil.isNotNull(seckillVouchers.getEndTime(), "请输入结束时间");
        AssertUtil.isTrue(now.after(seckillVouchers.getEndTime()), "结束时间不能早于当前时间");
        AssertUtil.isTrue(seckillVouchers.getStartTime().after(seckillVouchers.getEndTime()), "开始时间不能晚于结束时间");

        String key = RedisKeyConstant.seckill_vouchers.getKey() + seckillVouchers.getFkVoucherId();
        // 验证数据库中是否已经存在该券的秒杀活动
        Map<String, Object> map = redisTemplate.opsForHash().entries(key);
        AssertUtil.isTrue(!map.isEmpty() && (int) map.get("amount") > 0, "该券已经拥有了抢购活动");
        // 插入到redis
        seckillVouchers.setIsValid(1);
        seckillVouchers.setCreateDate(now);
        seckillVouchers.setUpdateDate(now);
        redisTemplate.opsForHash().putAll(key, BeanUtil.beanToMap(seckillVouchers));
    }

    /**
     * 抢购代金券   即客户开始秒杀抢购代金券
     * @param voucherId   代金券 ID
     * @param accessToken 登录token
     * @Para path 访问路径
     *
     * 抢购代金券的具体逻辑：
     * （1）进行非空校验
     * （2）基本参数校验
     * （3）判断此代金券是否加入抢购
     * （4）判断是否有效
     * （5）判断是否开始、结束
     * （6）判断是否卖完
     * （7）判断登录用户信息
     * （8）判断登录用户是否抢到（一个用户针对这次活动只能买一次）
     * （9）扣库存
     * （10）下单
     */
    @Transactional(rollbackFor = Exception.class)
    public ResultInfo doSeckill(Integer voucherId, String accessToken, String path) {
        // 基本参数校验
        AssertUtil.isTrue(voucherId == null || voucherId < 0, "请选择需要抢购的代金券");
        AssertUtil.isNotEmpty(accessToken, "请登录");

        String key = RedisKeyConstant.seckill_vouchers.getKey() + voucherId;
        Map<String, Object> map = redisTemplate.opsForHash().entries(key);
        SeckillVouchers seckillVouchers = BeanUtil.mapToBean(map, SeckillVouchers.class, true, null);

        // 判断是否开始、结束
        Date now = new Date();
        AssertUtil.isTrue(now.before(seckillVouchers.getStartTime()), "该抢购还未开始");
        AssertUtil.isTrue(now.after(seckillVouchers.getEndTime()), "该抢购已结束");
        // 判断是否卖完
        AssertUtil.isTrue(seckillVouchers.getAmount() < 1, "该券已经卖完啦");
        // 获取登录用户信息
        String url = oauthServerName + "user/me?access_token={accessToken}";
        ResultInfo resultInfo = restTemplate.getForObject(url, ResultInfo.class, accessToken);
        if (resultInfo.getCode() != ApiConstant.SUCCESS_CODE) {
            resultInfo.setPath(path);
            return resultInfo;
        }

        // 这里的data是一个LinkedHashMap，SignInDinerInfo
        SignInDinerInfo dinerInfo = BeanUtil.fillBeanWithMap((LinkedHashMap) resultInfo.getData(), new SignInDinerInfo(), false);
        // 判断登录用户是否已抢到(一个用户针对这次活动只能买一次)
        VoucherOrders order = voucherOrdersMapper.findDinerOrder(dinerInfo.getId(), seckillVouchers.getFkVoucherId());
        AssertUtil.isTrue(order != null, "该用户已抢到该代金券，无需再抢");

        // 使用Redisson分布式锁  保证一个用户只能抢购一次
        String lockName = RedisKeyConstant.lock_key.getKey() + dinerInfo.getId() + ":" + voucherId;
        long expireTime = seckillVouchers.getEndTime().getTime() - now.getTime();
        RLock lock = redissonClient.getLock(lockName);

        try {
            boolean isLocked = lock.tryLock(expireTime, TimeUnit.MILLISECONDS);
            if (isLocked) {
                // 下单
                VoucherOrders voucherOrders = new VoucherOrders();
                voucherOrders.setFkDinerId(dinerInfo.getId());
                // redis中不需要维护外键信息
                //voucherOrders.setFkSeckillId(seckillVouchers.getId());
                voucherOrders.setFkVoucherId(seckillVouchers.getFkVoucherId());
                String orderNo = IdUtil.getSnowflake(1, 1).nextIdStr();
                voucherOrders.setOrderNo(orderNo);
                voucherOrders.setOrderType(1);
                voucherOrders.setStatus(0);
                long count = voucherOrdersMapper.save(voucherOrders);
                AssertUtil.isTrue(count == 0, "用户抢购失败");

                // 采用redis+lua脚本 扣库存
                List<String> keys = new ArrayList<>();
                keys.add(key);
                keys.add("amount");
                Long amount = (Long) redisTemplate.execute(defaultRedisScript, keys);
                AssertUtil.isTrue(amount == null || amount < 1, "该券已经卖完了");
            }
        } catch (Exception e) {
            // 手动回滚事务
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            // 释放锁
            lock.unlock();
            if (e instanceof ParameterException) {
                return ResultInfoUtil.buildError(0, "该券已经卖完了", path);
            }
        }

        return ResultInfoUtil.buildSuccess(path, "抢购成功");
    }
}

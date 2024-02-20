package com.example.seckill.controller;

import com.example.commons.model.domain.ResultInfo;
import com.example.commons.model.pojo.SeckillVouchers;
import com.example.commons.utils.ResultInfoUtil;
import com.example.seckill.service.SeckillService;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

/**
 * 秒杀控制层
 */
@RestController
public class SeckillController {
    @Resource
    private SeckillService seckillService;
    @Resource
    private HttpServletRequest request;

    /**
     * 新增秒杀活动   即商家发布抢购活动
     * @param seckillVouchers
     * @return
     */
    @PostMapping("/add")
    public ResultInfo<String> addSeckillVouchers(@RequestBody SeckillVouchers seckillVouchers) {
        seckillService.addSeckillVouchers(seckillVouchers);
        return ResultInfoUtil.buildSuccess(request.getServletPath(), "添加成功");
    }

    /**
     * 秒杀下单
     * @param voucherId  在t_vouchers表中，voucherId就是主键id 在t_seckill_vouchers表中，voucherId就是fk_voucher_id
     * @param access_token
     * @return
     */
    @PostMapping("/{voucherId}")
    public ResultInfo<String> doSeckill(@PathVariable Integer voucherId, String access_token) {
        ResultInfo resultInfo = seckillService.doSeckill(voucherId, access_token, request.getServletPath());
        return resultInfo;
    }
}

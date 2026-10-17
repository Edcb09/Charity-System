package com.example.charity.controller;


import cn.dev33.satoken.stp.StpUtil;
import com.example.charity.common.Result;
import com.example.charity.service.DonationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/donation")
public class DonationController {
    @Autowired
    private DonationService donationService;

    //创建捐款订单
    @PostMapping("/create")
    public Result<String> create(@RequestBody Map<String,Object> params)
    {
        //获取当前登录用户ID
        Long userId= StpUtil.getLoginIdAsLong();
        Long projectId=Long.valueOf(params.get("projectId").toString());
        BigDecimal amount=new BigDecimal(params.get("amount").toString());
        boolean isAnonymous=Boolean.parseBoolean(params.get("isAnonymous").toString());

        String oderNo=donationService.createDonation(userId,projectId,amount,isAnonymous);
        return Result.success(oderNo);
    }

    //模拟支付成功回调
    @PostMapping("/pay")
    public Result<String> pay(@RequestBody Map<String,String> params)
    {
        String orderNo=params.get("orderNo");
        donationService.paySuccess(orderNo);
        return Result.success("支付成功");
    }
}

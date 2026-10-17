package com.example.charity.service;

import com.baomidou.mybatisplus.spring.service.IService;
import com.example.charity.entity.Donation;

public interface DonationService extends IService<Donation> {
    //发起捐款（生成待支付订单）
    String createDonation(Long userId,Long projectId,java.math.BigDecimal amount,boolean isAnonymous);

    //模拟支付
    void paySuccess(String orderNo);
}

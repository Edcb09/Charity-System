package com.example.charity.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.example.charity.entity.Donation;
import com.example.charity.mapper.DonationMapper;
import com.example.charity.mapper.ProjectMapper;
import com.example.charity.service.DonationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.example.charity.entity.Project;
import org.springframework.transaction.annotation.Transactional;


import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class DonationServiceImpl extends ServiceImpl<DonationMapper, Donation> implements DonationService {
    @Autowired
    private ProjectMapper projectMapper;

    @Override
    public String createDonation(Long userId, Long projectId, BigDecimal amount,boolean isAnonymous) {
        Project project = projectMapper.selectById(projectId);
        if (project == null || project.getStatus() != 2) {
            throw new RuntimeException("项目不存在或已结束募捐");
        }

        //生成订单
        Donation donation=new Donation();
        donation.setOrderNo(UUID.randomUUID().toString().replace("-",""));
        donation.setUserId(userId);
        donation.setProjectId(projectId);
        donation.setAmount(amount);
        donation.setIsAnonymous(isAnonymous?1:0);
        donation.setPayStatus(0);//待支付
        this.save(donation);

        return donation.getOrderNo();//返回订单号给前端去支付

    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void paySuccess(String orderNo)
    {
        //1.查订单
        Donation donation=this.getOne(new QueryWrapper<Donation>().eq("order_no",orderNo));
        if(donation==null||donation.getPayStatus()==1)
        {
            throw new RuntimeException("订单无效或已支付");
        }

        //2.更新订单状态
        donation.setPayStatus(1);
        donation.setDonateTime(LocalDateTime.now());
        this.updateById(donation);

        //3.更新项目已筹集金额（原子累加）
        UpdateWrapper<Project> wrapper=new UpdateWrapper<>();
        wrapper.eq("id",donation.getProjectId())
                .setSql("current_amount=current_amount+"+donation.getAmount());
        projectMapper.update(null,wrapper);

    }
}

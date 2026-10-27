package com.example.charity.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.example.charity.entity.Donation;
import com.example.charity.entity.Message;
import com.example.charity.mapper.DonationMapper;
import com.example.charity.mapper.ProjectMapper;
import com.example.charity.service.DonationService;
import com.example.charity.service.MessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.example.charity.entity.Project;
import org.springframework.transaction.annotation.Transactional;
import com.example.charity.entity.Message;


import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class DonationServiceImpl extends ServiceImpl<DonationMapper, Donation> implements DonationService {
    @Autowired
    private ProjectMapper projectMapper;
    @Autowired
    private MessageService messageService;

    @Override
    public String createDonation(Long userId, Long projectId, BigDecimal amount, boolean isAnonymous) {
        Project project = projectMapper.selectById(projectId);
        if (project == null || project.getStatus() != 2) {
            throw new RuntimeException("项目不存在或已结束募捐");
        }

        //生成订单
        Donation donation = new Donation();
        donation.setOrderNo(UUID.randomUUID().toString().replace("-", ""));
        donation.setUserId(userId);
        donation.setProjectId(projectId);
        donation.setAmount(amount);
        donation.setIsAnonymous(isAnonymous ? 1 : 0);
        donation.setPayStatus(0);//待支付
        this.save(donation);

        return donation.getOrderNo();//返回订单号给前端去支付

    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void paySuccess(String orderNo) {
        //-------- Fail-Fast --------
        //1.1 查订单
        Donation donation = this.getOne(new QueryWrapper<Donation>().eq("order_no", orderNo));
        if (donation == null || donation.getPayStatus() == 1) {
            throw new RuntimeException("订单无效");
        }
        if (donation.getPayStatus() == 1) {
            throw new RuntimeException("该订单已支付，请勿重复操作！");
        }

        //1.2 查项目
        Project project = projectMapper.selectById(donation.getProjectId());
        if (project == null) {
            throw new RuntimeException("支付失败：未找到对应项目，项目ID：" + donation.getProjectId());
        }

        //1.3 校验项目状态
        if (project.getStatus() != 2) {
            throw new RuntimeException("支付失败：该项目已结束募捐或不可捐赠");
        }

        //-------- Update --------
        //2.1 更新订单状态
        donation.setPayStatus(1);
        donation.setDonateTime(LocalDateTime.now());
        this.updateById(donation);

        //2.2 更新项目已筹集金额（原子累加）
        LambdaUpdateWrapper<Project> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(Project::getProjectId, donation.getProjectId())
                .setSql("current_amount=current_amount+" + donation.getAmount());
        projectMapper.update(null, wrapper);

        //2.3 重新查询项目
        project = projectMapper.selectById(donation.getProjectId());
        if (project.getCurrentAmount().compareTo(project.getTargetAmount()) >= 0) {
            project.setStatus(3);
            project.setFinishTime(java.time.LocalDateTime.now());
            projectMapper.updateById(project);

            Message msg = new com.example.charity.entity.Message();
            msg.setReceiveUserId(project.getUserId()); // 发给项目申请人
            msg.setContent("好消息！您发起的项目【" + project.getTitle() + "】已达到目标筹款金额，募捐已提前结束。请及时提交资金使用报告。");
            msg.setType(5); // 5=系统通知（达标自动结束，不是人工审核）
            msg.setLinkId(project.getProjectId());
            msg.setIsRead(0);
            msg.setCreateTime(java.time.LocalDateTime.now());
            messageService.save(msg);
        }



    //新增消息通知
    Message msg = new Message();
    msg.setReceiveUserId(donation.getUserId());
    msg.setContent("您已成功向项目【"+project.getTitle()+"】捐款"+donation.getAmount()+"元，感谢您的爱心！");
    msg.setType(2);
    msg.setLinkId(project.getProjectId());
    msg.setIsRead(0);
    msg.setCreateTime(LocalDateTime.now());

    messageService.save(msg);


    }

}

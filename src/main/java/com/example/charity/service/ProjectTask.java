package com.example.charity.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.charity.entity.Donation;
import com.example.charity.entity.Message;
import com.example.charity.entity.Project;
import com.example.charity.mapper.DonationMapper;
import com.example.charity.mapper.ProjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;
import java.util.List;

@Component
public class ProjectTask {

    @Autowired
    private ProjectMapper projectMapper;
    @Autowired
    private DonationMapper donationMapper;
    @Autowired
    private MessageService messageService;

    // 每天凌晨 2 点执行一次 (cron 表达式)
    @Scheduled(fixedRate = 60000)
    public void autoEndExpiredProjects() {
        System.out.println("开始执行定时任务：扫描超时未达标的项目...");

        // 查出所有状态为 2 (募捐中) 且 截止时间小于当前时间 的项目
        QueryWrapper<Project> wrapper = new QueryWrapper<>();
        wrapper.eq("status", 2).lt("end_time", LocalDateTime.now());
        List<Project> expiredProjects = projectMapper.selectList(wrapper);

        for (Project project : expiredProjects) {
            project.setStatus(3); // 改为已结束
            project.setFinishTime(project.getEndTime()); // 记录结束时间为原定截止时间
            if (project.getCurrentAmount().compareTo(java.math.BigDecimal.ZERO) == 0) {//筹集金额为0
                project.setStatus(5); // 直接变成已归档
                project.setRejectReason("该项目未筹集到任何资金，已自动结束并归档。");


                Message msg = new com.example.charity.entity.Message();
                msg.setReceiveUserId(project.getUserId());
                msg.setContent("很遗憾，您发起的项目【" + project.getTitle() + "】因到期未筹集到资金，已自动归档。您可以重新准备材料再次发起。");
                msg.setType(1);
                msg.setLinkId(project.getProjectId());
                msg.setIsRead(0);
                msg.setCreateTime(java.time.LocalDateTime.now());
                messageService.save(msg);
            }else{//筹集金额不为0，公示筹集的金额的用途
                project.setStatus(3);
                com.example.charity.entity.Message msg = new com.example.charity.entity.Message();
                msg.setReceiveUserId(project.getUserId());
                msg.setContent("您的项目【" + project.getTitle() + "】募捐时间已截止，已停止募捐。当前共筹得 ￥" + project.getCurrentAmount() + "。请及时提交资金使用报告。");
                msg.setType(1);
                msg.setLinkId(project.getProjectId());
                msg.setIsRead(0);
                msg.setCreateTime(java.time.LocalDateTime.now());
                messageService.save(msg);

            }
            projectMapper.updateById(project);
            System.out.println("项目 [" + project.getTitle() + "] 已超时，自动结束。");
        }
    }

    // 每天凌晨 3 点执行
    @Scheduled(cron = "0 0 3 * * ?")
    public void cleanUnpaidDonations() {
        System.out.println("开始清理超过24小时未支付的捐款订单...");

        QueryWrapper<Donation> wrapper = new QueryWrapper<>();
        // 状态为 0 (待支付) 且 创建时间小于 1天前
        wrapper.eq("pay_status", 0)
                .lt("create_time", LocalDateTime.now().minusDays(1));

        int deleted = donationMapper.delete(wrapper);
        System.out.println("清理完成，共删除了 " + deleted + " 条废弃订单。");
    }
}
package com.example.charity.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.charity.entity.Project;
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
            projectMapper.updateById(project);
            System.out.println("项目 [" + project.getTitle() + "] 已超时，自动结束。");
        }
    }
}
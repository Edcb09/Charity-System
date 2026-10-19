package com.example.charity.controller;


import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.charity.common.Result;
import com.example.charity.entity.FundReport;
import com.example.charity.entity.Project;
import com.example.charity.entity.Report;
import com.example.charity.entity.User;
import com.example.charity.service.FundReportService;
import com.example.charity.service.ProjectService;
import com.example.charity.service.ReportService;
import com.example.charity.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/admin")
public class AdminController {
    @Autowired private ProjectService projectService;
    @Autowired private FundReportService fundReportService;
    @Autowired private UserService userService;
    @Autowired private ReportService reportService;

    //获取待办事项总数
    @GetMapping("/pendingCount")
    public Result<Map<String,Long>> getPendingCount()
    {
        //1.权限校验
        Long userId= StpUtil.getLoginIdAsLong();
        User currentUser=userService.getById(userId);
        if(currentUser==null||currentUser.getRole()!=2)
        {
            return Result.error("无权限");
        }

        //2.统计待审核项目
        QueryWrapper<Project> projectWrapper=new QueryWrapper<>();
        projectWrapper.eq("status",1);
        long projectCount=projectService.count(projectWrapper);

        //3.统计待审核资金报告
        QueryWrapper<FundReport> reportWrapper=new QueryWrapper<>();
        reportWrapper.eq("status",0);
        long fundReportCount=fundReportService.count(reportWrapper);

        /// 4. 统计待处理举报 (status = 0) 👈 新增
        QueryWrapper<Report> violationWrapper = new QueryWrapper<>();
        violationWrapper.eq("status", 0);
        long violationCount = reportService.count(violationWrapper);

        // 5. 返回总数
        Map<String, Long> map = new HashMap<>();
        map.put("projectCount", projectCount);
        map.put("fundReportCount", fundReportCount); // 改名为了清晰
        map.put("violationCount", violationCount);   // 👈 新增
        map.put("total", projectCount + fundReportCount + violationCount);

        return Result.success(map);
    }
}

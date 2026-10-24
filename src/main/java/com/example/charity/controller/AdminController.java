package com.example.charity.controller;


import cn.dev33.satoken.stp.StpUtil;
import com.alibaba.excel.EasyExcel;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.charity.common.Result;
import com.example.charity.entity.*;
import com.example.charity.service.*;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin")
public class AdminController {
    @Autowired private ProjectService projectService;
    @Autowired private FundReportService fundReportService;
    @Autowired private UserService userService;
    @Autowired private ReportService reportService;
    @Autowired private DonationService donationService;

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

        // 4. 统计待处理举报 (status = 0)
        QueryWrapper<Report> violationWrapper = new QueryWrapper<>();
        violationWrapper.eq("status", 0);
        long violationCount = reportService.count(violationWrapper);

        // 5. 统计待归档审核的项目 (status = 6)
        long archiveCount = projectService.count(new QueryWrapper<Project>().eq("status", 6));

        // 6. 返回总数
        Map<String, Long> map = new HashMap<>();
        map.put("projectCount", projectCount);
        map.put("fundReportCount", fundReportCount); // 改名为了清晰
        map.put("violationCount", violationCount);
        map.put("total", projectCount + fundReportCount + violationCount + archiveCount);

        return Result.success(map);
    }

    @GetMapping("/exportDonations")
    public void exportDonations(HttpServletResponse response) throws IOException {
        // 1. 权限校验（自己加一下，只能管理员访问）
        Long userId = StpUtil.getLoginIdAsLong();
        User currentUser = userService.getById(userId);
        if (currentUser == null || currentUser.getRole() != 2) {
            // 这里简单返回 403 即可，为了演示直接返回空
            response.setStatus(403);
            return;
        }

        // 2. 查询所有已支付记录
        QueryWrapper<Donation> wrapper = new QueryWrapper<>();
        wrapper.eq("pay_status", 1).orderByDesc("donate_time");
        List<Donation> list = donationService.list(wrapper);

        // 3. 转换为导出 DTO
        List<DonationExportExcel> exportList = new ArrayList<>();
        for (Donation d : list) {
            DonationExportExcel e = new DonationExportExcel();
            e.setOrderNo(d.getOrderNo());
            e.setAmount(d.getAmount());
            e.setIsAnonymous(d.getIsAnonymous() == 1 ? "匿名" : "实名");
            e.setDonateTime(d.getDonateTime());

            // 查项目名
            Project p = projectService.getById(d.getProjectId());
            e.setProjectTitle(p != null ? p.getTitle() : "已删除项目");

            // 查用户名
            User u = userService.getById(d.getUserId());
            e.setUsername(u != null ? u.getUsername() : "未知用户");

            exportList.add(e);
        }

        // 4. 设置响应头并写入 Excel
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        // 避免中文文件名乱码
        String fileName = java.net.URLEncoder.encode("爱心捐款流水", "UTF-8").replaceAll("\\+", "%20");
        response.setHeader("Content-disposition", "attachment;filename*=utf-8''" + fileName + ".xlsx");

        EasyExcel.write(response.getOutputStream(), DonationExportExcel.class)
                .sheet("捐款流水")
                .doWrite(exportList);
    }
}

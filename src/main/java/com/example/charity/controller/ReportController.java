package com.example.charity.controller;


import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.charity.common.Result;
import com.example.charity.entity.Message;
import com.example.charity.entity.Project;
import com.example.charity.entity.Report;
import com.example.charity.entity.User;
import com.example.charity.service.MessageService;
import com.example.charity.service.ProjectService;
import com.example.charity.service.ReportService;
import com.example.charity.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/report")
public class ReportController {
    @Autowired
    private ReportService reportService;
    @Autowired private ProjectService projectService;
    @Autowired private UserService userService;
    @Autowired private MessageService messageService;

    // 1. 用户提交举报
    @PostMapping("/add")
    public Result<String> addReport(@RequestBody Report report) {
        Long userId = StpUtil.getLoginIdAsLong();
        Project project = projectService.getById(report.getProjectId());

        if (project == null) return Result.error("项目不存在");
        if (project.getUserId().equals(userId)) return Result.error("不能举报自己发起的项目");

        // 防重复举报校验
        QueryWrapper<Report> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId).eq("project_id", report.getProjectId());
        if (reportService.count(wrapper) > 0) {
            return Result.error("您已经举报过该项目，请等待管理员处理");
        }

        report.setUserId(userId);
        report.setStatus(0); // 0 待处理
        report.setCreateTime(LocalDateTime.now());
        reportService.save(report);

        return Result.success("举报已提交，感谢您的反馈");
    }

    // 2. 管理员获取所有举报列表
    @GetMapping("/admin/list")
    public Result<List<Report>> adminList() {
        Long userId = StpUtil.getLoginIdAsLong();
        User currentUser = userService.getById(userId);
        if (currentUser == null || currentUser.getRole() != 2) return Result.error("无权限");

        List<Report> list = reportService.list(new QueryWrapper<Report>().orderByAsc("status").orderByDesc("create_time"));

        // 填充项目名和举报人信息
        for (Report r : list) {
            Project p = projectService.getById(r.getProjectId());
            if (p != null) r.setProjectTitle(p.getTitle());
            User u = userService.getById(r.getUserId());
            if (u != null) r.setReporterName(u.getUsername());
        }
        return Result.success(list);
    }

    // 3. 管理员处理举报
    @PostMapping("/admin/handle")
    public Result<String> handleReport(@RequestBody Map<String, Object> params) {
        Long adminId = StpUtil.getLoginIdAsLong();
        User currentUser = userService.getById(adminId);
        if (currentUser == null || currentUser.getRole() != 2) return Result.error("无权限");

        Long reportId = Long.valueOf(params.get("reportId").toString());
        Integer status = Integer.valueOf(params.get("status").toString()); // 1属实 2驳回
        String handleResult = params.get("handleResult").toString();

        Report report = reportService.getById(reportId);
        if (report == null) return Result.error("举报记录不存在");

        // 1. 更新举报记录
        report.setStatus(status);
        report.setAdminId(adminId);
        report.setHandleResult(handleResult);
        reportService.updateById(report);

        // 2. 如果举报属实，强制下架项目 (新增逻辑)
        Project project = projectService.getById(report.getProjectId());
        if (status == 1 && project != null) {
            // 将项目状态改为 4 (驳回/违规下架)
            project.setStatus(4);
            // 把违规原因写在项目的驳回理由里
            project.setRejectReason("违反平台规定被强制下架。处理说明：" + handleResult);
            projectService.updateById(project);

            // 给项目申请人发送下架通知
            Message applicantMsg = new Message();
            applicantMsg.setReceiveUserId(project.getUserId());
            applicantMsg.setContent("您发起的项目【" + project.getTitle() + "】因违规被强制下架。原因：" + handleResult);
            applicantMsg.setType(1); // 1 审核结果通知
            applicantMsg.setLinkId(project.getProjectId());
            applicantMsg.setIsRead(0);
            applicantMsg.setCreateTime(LocalDateTime.now());
            messageService.save(applicantMsg);
        }

        // 3. 给举报人发送处理结果通知
        Message msg = new Message();
        msg.setReceiveUserId(report.getUserId());
        msg.setContent("您对项目【" + (project != null ? project.getTitle() : "未知项目") + "】的举报已处理。结果：" + handleResult);
        msg.setType(4); // 4 举报处理结果
        msg.setLinkId(report.getProjectId());
        msg.setIsRead(0);
        msg.setCreateTime(LocalDateTime.now());
        messageService.save(msg);

        return Result.success("处理成功");
    }
}

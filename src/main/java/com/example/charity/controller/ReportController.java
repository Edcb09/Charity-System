package com.example.charity.controller;


import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.charity.common.Result;
import com.example.charity.entity.*;
import com.example.charity.service.*;
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
    @Autowired private CommentService commentService;

    // 1. 用户提交举报
    @PostMapping("/add")
    public Result<String> addReport(@RequestBody Report report) {
        Long userId = StpUtil.getLoginIdAsLong();

        // 如果是举报项目（targetType 默认为1）
        if (report.getTargetType() == null || report.getTargetType() == 1) {
            Project project = projectService.getById(report.getProjectId());
            if (project == null) return Result.error("项目不存在");
            if (project.getUserId().equals(userId)) return Result.error("不能举报自己发起的项目");

            QueryWrapper<Report> wrapper = new QueryWrapper<>();
            wrapper.eq("user_id", userId).eq("project_id", report.getProjectId()).eq("target_type", 1);
            if (reportService.count(wrapper) > 0) return Result.error("您已经举报过该项目了");
        }
        // 如果是举报留言（targetType 为2）
        else if (report.getTargetType() == 2) {
            if (report.getCommentId() == null) return Result.error("留言ID不能为空");

            Comment comment = commentService.getById(report.getCommentId());
            if (comment == null) return Result.error("该留言不存在或已被删除");
            if (comment.getUserId().equals(userId)) {
                return Result.error("不能举报自己发布的留言");
            }

            // 防重复举报留言
            QueryWrapper<Report> wrapper = new QueryWrapper<>();
            wrapper.eq("user_id", userId).eq("comment_id", report.getCommentId()).eq("target_type", 2);
            if (reportService.count(wrapper) > 0) return Result.error("您已经举报过该留言了");
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

            // 如果是举报留言，把留言内容填进去
            if (r.getTargetType() == 2 && r.getCommentId() != null) {
                com.example.charity.entity.Comment c = commentService.getById(r.getCommentId());
                if (c != null) r.setCommentContent(c.getContent());
            }
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
        if (status == 1) {
            // 场景1：处理项目举报（下架项目）
            if (report.getTargetType() == 1) {
                Project project = projectService.getById(report.getProjectId());
                if (project != null) {
                    project.setStatus(7);
                    project.setRejectReason("违反平台规定被强制下架。处理说明：" + handleResult);
                    projectService.updateById(project);
                    // 发通知给项目申请人
                    Message authorMsg = new Message();
                    authorMsg.setReceiveUserId(project.getUserId()); // 项目作者
                    authorMsg.setContent("您的项目因违反平台规定已被管理员下架。处理说明：" + handleResult);
                    authorMsg.setType(5);
                    authorMsg.setLinkId(report.getProjectId());
                    authorMsg.setIsRead(0);
                    authorMsg.setCreateTime(LocalDateTime.now());
                    messageService.save(authorMsg);
                }
            }
            // 场景2：处理留言举报（屏蔽留言）
            else if (report.getTargetType() == 2) {
                Comment comment = commentService.getById(report.getCommentId());
                if (comment != null) {
                    comment.setIsDeleted(1); // 逻辑删除该留言
                    commentService.updateById(comment);

                    Message authorMsg = new Message();
                    authorMsg.setReceiveUserId(comment.getUserId()); // 留言作者
                    authorMsg.setContent("您的留言因违反平台规定已被管理员屏蔽。处理说明：" + handleResult);
                    authorMsg.setType(5); // 可以定义 5 为系统警告/违规通知
                    authorMsg.setLinkId(report.getProjectId());
                    authorMsg.setIsRead(0);
                    authorMsg.setCreateTime(LocalDateTime.now());
                    messageService.save(authorMsg);
                }
            }
        }

        // 3. 给举报人发送处理结果通知
        Message msg = new Message();
        msg.setReceiveUserId(report.getUserId());
        msg.setContent("您对" + (report.getTargetType() == 1 ? "项目" : "留言") + "的举报已处理。结果：" + handleResult);
        msg.setType(4); // 4 举报处理结果
        msg.setLinkId(report.getProjectId());
        msg.setIsRead(0);
        msg.setCreateTime(LocalDateTime.now());
        messageService.save(msg);

        return Result.success("处理成功");
    }
}

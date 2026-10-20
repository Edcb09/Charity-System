package com.example.charity.controller;


import ch.qos.logback.core.pattern.util.RegularEscapeUtil;
import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaIgnore;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.charity.common.Result;
import com.example.charity.entity.Message;
import com.example.charity.entity.Project;
import com.example.charity.entity.User;
import com.example.charity.service.MessageService;
import com.example.charity.service.ProjectService;
import com.example.charity.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/project")
public class ProjectController {

    @Autowired
    private ProjectService projectService;
    @Autowired
    private UserService userService;
    @Autowired
    private MessageService messageService;

    //查询项目列表（可分页、搜索、筛选）
    @GetMapping("/list")
    public Result<Page<Project>> list(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String projectType
    ){
        Page<Project> page=new Page<>(pageNum,pageSize);
        QueryWrapper<Project> queryWrapper=new QueryWrapper<>();

        //普通用户只能看到“2 募捐中”+"3 募捐结束"+"5 已归档"+"6 归档待审核"的项目
        queryWrapper.in("status",2,3,5,6);

        //模糊搜索标题
        if(keyword!=null&&!keyword.isEmpty())
        {
            queryWrapper.like("title",keyword);
        }

        //筛选项目类型
        if (projectType!=null&&!projectType.isEmpty())
        {
            queryWrapper.eq("project_type",projectType);
        }

        //创建时间最新排列
        queryWrapper.orderByDesc("create_time");

        projectService.page(page,queryWrapper);
        return Result.success(page);
    }

    //获取项目详情
    @SaIgnore
    @GetMapping("/{id}")
    public Result<Project> getDetail(@PathVariable Long id)
    {
        Project project=projectService.getById(id);
        if(project==null)
        {
            return Result.error("项目不存在");
        }

        //新增：查询申请人的用户名，返回给前端
        User applicant =userService.getById(project.getUserId());
        if(applicant!=null){
            project.setApplicantName(applicant.getUsername());
        }
        return Result.success(project);
    }

    //1.申请募捐项目，存草稿
    @SaCheckLogin
    @PostMapping("/add")
    public Result<String> addProject(@RequestBody Project project)
    {
        //获取当前登录用户的ID作为申请人
        Long userId = StpUtil.getLoginIdAsLong();
        User currentUser=userService.getById(userId);
        if(currentUser.getRole()!=1)
        {
            return Result.error("普通捐赠人无权发起项目，请联系管理员更改角色");
        }
        project.setUserId(userId);
        project.setCurrentAmount(BigDecimal.ZERO);

        if(project.getStatus()==null){
        project.setStatus(1);//待审核
        }

        project.setCreateTime(LocalDateTime.now());
        projectService.save(project);
        return Result.success(project.getStatus()==0?"草稿保存成功":"项目提交成功，等待管理员审核");
    }

    // 2. 修改项目（用于编辑草稿或驳回后重新提交）
    @PutMapping("/update")
    public Result<String> updateProject(@RequestBody Project project) {
        Long userId = StpUtil.getLoginIdAsLong();
        Project oldProject = projectService.getById(project.getProjectId());

        if (oldProject == null) return Result.error("项目不存在");
        if (!oldProject.getUserId().equals(userId)) return Result.error("无权修改该项目");

        // 只有草稿(0) 或 被驳回(4) 的项目才能修改
        if (oldProject.getStatus() != 0 && oldProject.getStatus() != 4) {
            return Result.error("只有草稿或被驳回的项目才能修改");
        }

        // 防止前端恶意篡改用户ID、已筹金额和时间
        project.setUserId(userId);
        project.setCurrentAmount(oldProject.getCurrentAmount());
        project.setCreateTime(oldProject.getCreateTime());

        // 如果修改后提交审核，状态会变成1；如果还是存草稿，状态就是0
        projectService.updateById(project);

        return Result.success(project.getStatus() == 0 ? "草稿更新成功" : "项目重新提交成功，等待审核");
    }

    //获取审核项目列表
    @GetMapping("/admin/pending")
    public Result<List<Project>> getPendingProjects()
    {
        //通过role进行权限校验
        Long userId=StpUtil.getLoginIdAsLong();
        User currentUser=userService.getById(userId);
        if(currentUser==null||currentUser.getRole()!=2)
        {
            return Result.error("无权限操作，仅管理员可用");
        }

        QueryWrapper<Project> wrapper=new QueryWrapper<>();
        wrapper.eq("status",1).orderByDesc("create_time");
        List<Project> list=projectService.list(wrapper);

        //填入申请人的名字
        for(Project p:list)
        {
            User applicant=userService.getById(p.getUserId());
            if(applicant!=null) p.setApplicantName(applicant.getUsername());
        }
        return Result.success(list);
    }

    //审核项目
    @PostMapping("/admin/audit")
    public Result<String> auditProject(@RequestBody Map<String,Object> params)
    {
        Long userId=StpUtil.getLoginIdAsLong();
        User currentUser=userService.getById(userId);
        if(currentUser==null||currentUser.getRole()!=2)
        {
            return Result.error("无权限操作");
        }

        Long projectId=Long.valueOf(params.get("projectId").toString());
        Integer status=Integer.valueOf(params.get("status").toString());
        String rejectReason=params.get("rejectReason")!=null?params.get("rejectReason").toString():"";

        Project project=projectService.getById(projectId);
        if (project==null)return Result.error("项目不存在");

        project.setStatus(status);
        project.setAuditTime(java.time.LocalDateTime.now());
        if(status==4)
        {
            project.setRejectReason(rejectReason);
        }

        projectService.updateById(project);

        //发送审核结果通知
        Message msg=new Message();
        msg.setReceiveUserId(project.getUserId());
        if(status==2)
        {
            msg.setContent("您发起的项目【"+project.getTitle()+"】已通过审核，募捐项目已启动。");
        }else{
            msg.setContent("很遗憾，您发起的项目【"+project.getTitle()+"】未通过审核。理由："+rejectReason);
        }
        msg.setType(1);
        msg.setLinkId(projectId);
        msg.setIsRead(0);
        msg.setCreateTime(LocalDateTime.now());
        messageService.save(msg);

        return Result.success("审核操作成功");
    }

    @GetMapping("/my")
    public Result<List<Project>> myProjects()
    {
        Long userId=StpUtil.getLoginIdAsLong();
        QueryWrapper<Project> wrapper=new QueryWrapper<>();
        wrapper.eq("user_id",userId).orderByDesc("create_time");
        return Result.success(projectService.list(wrapper));
    }


    // ================= 项目归档相关接口 =================

    // 1. 申请人申请结项 -> 状态变为 6
    @PostMapping("/applyFinish/{id}")
    public Result<String> applyFinish(@PathVariable Long id) {
        Long userId = StpUtil.getLoginIdAsLong();
        Project project = projectService.getById(id);

        if (project == null) return Result.error("项目不存在");
        if (!project.getUserId().equals(userId)) return Result.error("您无权操作该项目");
        if (project.getStatus() != 3) return Result.error("当前状态无法申请结项");

        // 状态从 3 变为 6
        project.setStatus(6);
        projectService.updateById(project);

        // 发消息给管理员...
        List<User> admins = userService.list(new QueryWrapper<User>().eq("role", 2));
        for (User admin : admins) {
            Message msg = new Message();
            msg.setReceiveUserId(admin.getUserId());
            msg.setContent("申请人申请将项目【" + project.getTitle() + "】结项归档，请前往后台审核。");
            msg.setType(1);
            msg.setLinkId(project.getProjectId());
            msg.setIsRead(0);
            msg.setCreateTime(java.time.LocalDateTime.now());
            messageService.save(msg);
        }
        return Result.success("结项申请已提交，等待管理员审核");
    }

    // 2. 管理员获取待归档(状态6)的项目列表
    @GetMapping("/admin/finished")
    public Result<List<Project>> getFinishedProjects() {
        Long userId = StpUtil.getLoginIdAsLong();
        User currentUser = userService.getById(userId);
        if (currentUser == null || currentUser.getRole() != 2) return Result.error("无权限");

        QueryWrapper<Project> wrapper = new QueryWrapper<>();
        wrapper.eq("status", 6).orderByDesc("audit_time"); // 👈 改为查 6
        return Result.success(projectService.list(wrapper));
    }

    // 3. 管理员同意归档 -> 状态变5
    @PostMapping("/admin/archive/{id}")
    public Result<String> archiveProject(@PathVariable Long id) {
        // ... 权限校验 ...
        Project project = projectService.getById(id);
        if (project == null || project.getStatus() != 6) {
            return Result.error("项目状态错误，无法归档");
        }
        project.setStatus(5);
        projectService.updateById(project);
        // 发通知给申请人...
        return Result.success("项目已成功归档");
    }

    // 4. 管理员驳回归档 -> 状态变回 3
    @PostMapping("/admin/rejectArchive")
    public Result<String> rejectArchive(@RequestBody Map<String, Object> params) {
        Long userId = StpUtil.getLoginIdAsLong();
        User currentUser = userService.getById(userId);
        if (currentUser == null || currentUser.getRole() != 2) return Result.error("无权限");

        Long projectId = Long.valueOf(params.get("projectId").toString());
        String rejectReason = params.get("rejectReason").toString();

        Project project = projectService.getById(projectId);
        if (project == null || project.getStatus() != 6) return Result.error("状态错误");

        project.setStatus(3); // 退回已结束
        project.setRejectReason("归档申请被驳回：" + rejectReason); // 借用这个字段存驳回理由
        projectService.updateById(project);

        // 发通知给申请人
        Message msg = new Message();
        msg.setReceiveUserId(project.getUserId());
        msg.setContent("您的项目【" + project.getTitle() + "】归档申请被驳回。理由：" + rejectReason + "，请继续补充资金报告后重新申请。");
        msg.setType(1);
        msg.setLinkId(project.getProjectId());
        msg.setIsRead(0);
        msg.setCreateTime(java.time.LocalDateTime.now());
        messageService.save(msg);

        return Result.success("已驳回结项申请");
    }

}

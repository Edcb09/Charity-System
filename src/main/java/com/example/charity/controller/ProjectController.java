package com.example.charity.controller;


import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaIgnore;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.charity.common.Result;
import com.example.charity.entity.Project;
import com.example.charity.entity.User;
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

        //普通用户只能看到“2 募捐中”的项目
        queryWrapper.eq("status",2);

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
        return Result.success(project);
    }

    //申请募捐项目
    @SaCheckLogin
    @PostMapping("/add")
    public Result<String> addProject(@RequestBody Project project)
    {
        //获取当前登录用户的ID作为申请人
        Long userId = StpUtil.getLoginIdAsLong();
        project.setUserId(userId);
        project.setCurrentAmount(BigDecimal.ZERO);
        project.setStatus(1);//待审核
        project.setCreateTime(LocalDateTime.now());

        projectService.save(project);
        return Result.success("项目提交成功，等待管理员审核");
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
        return Result.success("审核操作成功");
    }

}

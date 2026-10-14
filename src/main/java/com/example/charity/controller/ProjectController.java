package com.example.charity.controller;


import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.charity.common.Result;
import com.example.charity.entity.Project;
import com.example.charity.service.ProjectService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/project")
public class ProjectController {

    @Autowired
    private ProjectService projectService;

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

}

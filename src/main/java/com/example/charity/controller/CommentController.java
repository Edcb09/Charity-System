package com.example.charity.controller;


import cn.dev33.satoken.annotation.SaIgnore;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.charity.common.Result;
import com.example.charity.entity.Comment;
import com.example.charity.entity.User;
import com.example.charity.service.CommentService;
import com.example.charity.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/comment")
public class CommentController {
    @Autowired
    private CommentService commentService;
    @Autowired
    private UserService userService;

    //获取某个项目的留言列表（游客可见，加@SaIgnore）
    @SaIgnore
    @GetMapping("/list/{projectId}")
    public Result<List<Comment>> List(@PathVariable Long projectId){
        QueryWrapper<Comment> wrapper=new QueryWrapper<>();
        wrapper.eq("project_id",projectId)
                .eq("is_deleted",0)
                .orderByDesc("create_time");

       List<Comment> list=commentService.list(wrapper);

       //遍历补充用户名信息
        for(Comment comment:list)
        {
            User user=userService.getById(comment.getUserId());
            if(user!=null)
            {
                comment.setUsername(user.getUsername());
            }else{
                comment.setUsername("未知用户");
            }
        }
        return Result.success(list);
    }

    //发布留言
    @PostMapping("/add")
    public Result<String> add(@RequestBody Comment comment)
    {
        if(comment.getContent()==null||comment.getContent().trim().isEmpty())
        {
            return Result.error("留言内容不能为空");
        }

        if(comment.getContent().length()>500)
        {
            return Result.error("留言内容不能超过500字");
        }

        //获取当前登录用户
        Long userId= StpUtil.getLoginIdAsLong();
        comment.setUserId(userId);
        comment.setCreateTime(LocalDateTime.now());
        comment.setIsDeleted(0);

        commentService.save(comment);
        return Result.success("留言成功");
    }
}

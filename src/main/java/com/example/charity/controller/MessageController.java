package com.example.charity.controller;


import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.example.charity.common.Result;
import com.example.charity.entity.Message;
import com.example.charity.service.MessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.concurrent.ScheduledThreadPoolExecutor;

@RestController
@RequestMapping("/message")
public class MessageController {
    @Autowired
    private MessageService messageService;

    //1.获取消息列表
    @GetMapping("/my")
    public Result<List<Message>> myMessages()
    {
        Long userId= StpUtil.getLoginIdAsLong();
        QueryWrapper<Message> wrapper=new QueryWrapper<>();
        wrapper.eq("receive_user_id",userId).orderByDesc("create_time");
        return Result.success(messageService.list(wrapper));
    }

    //2.获取未读消息数量
    @GetMapping("/unreadCount")
    public Result<Long> unreadCount(){
        Long userId=StpUtil.getLoginIdAsLong();
        QueryWrapper<Message> wrapper=new QueryWrapper<>();
        wrapper.eq("receive_user_id",userId).eq("is_read",0);
        return Result.success(messageService.count(wrapper));
    }

    //3.将消息标记为已读
    @PostMapping("/read/{id}")
    public Result<String> markAsRead(@PathVariable Long id)
    {
        Message msg=new Message();
        msg.setMsgId(id);
        msg.setIsRead(1);
        messageService.updateById(msg);
        return Result.success("已读");
    }

    //4.一键全部已读
    @PostMapping("/readAll")
    public Result<String> readAll()
    {
        Long userId=StpUtil.getLoginIdAsLong();
        UpdateWrapper<Message> wrapper=new UpdateWrapper<>();
        wrapper.eq("receive_user_id",userId).eq("is_read",0).set("is_read",1);
        messageService.update(null,wrapper);
        return Result.success("全部已读");
    }
}

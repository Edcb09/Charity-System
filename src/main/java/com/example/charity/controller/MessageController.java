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

    // 1. 获取我的消息列表（分页）
    @GetMapping("/my")
    public Result<com.baomidou.mybatisplus.extension.plugins.pagination.Page<Message>> myMessages(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        Long userId = StpUtil.getLoginIdAsLong();

        // 创建分页对象
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<Message> page =
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(pageNum, pageSize);

        QueryWrapper<Message> wrapper = new QueryWrapper<>();
        wrapper.eq("receive_user_id", userId).orderByDesc("create_time");

        // 执行分页查询
        messageService.page(page, wrapper);

        return Result.success(page);
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

    // 删除单条消息
    @DeleteMapping("/{id}")
    public Result<String> deleteMessage(@PathVariable Long id) {
        Long userId = StpUtil.getLoginIdAsLong();
        Message msg = messageService.getById(id);

        if (msg == null) return Result.error("消息不存在");
        // 安全校验：只能删自己的消息
        if (!msg.getReceiveUserId().equals(userId)) {
            return Result.error("无权操作该消息");
        }

        messageService.removeById(id);
        return Result.success("删除成功");
    }

    // 清空我的所有消息
    @DeleteMapping("/clear")
    public Result<String> clearMessages() {
        Long userId = StpUtil.getLoginIdAsLong();
        QueryWrapper<Message> wrapper = new QueryWrapper<>();
        wrapper.eq("receive_user_id", userId);

        messageService.remove(wrapper);
        return Result.success("消息已清空");
    }
}

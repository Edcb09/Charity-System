package com.example.charity.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("message")
public class Message {
    @TableId(type= IdType.AUTO)
    private Long msgId;
    private Long receiveUserId;
    private String content;
    private Integer type;//1项目审核 2捐款成功 3资金报告 4举报处理
    private Long linkId;//关联业务ID
    private Integer isRead;//0未读 1已读
    private LocalDateTime createTime;
}

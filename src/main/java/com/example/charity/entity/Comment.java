package com.example.charity.entity;


import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("comments")
public class Comment {
    @TableId(type= IdType.AUTO)
    private Long commentId;
    private Long projectId;
    private Long userId;
    private String content;
    private LocalDateTime createTime;
    private Integer isDeleted;

    //非数据库字段
    @TableField(exist = false)
    private String username;
    @TableField(exist = false)
    private String avatar;
}

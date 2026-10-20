package com.example.charity.entity;


import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("report")
public class Report {
    @TableId(type= IdType.AUTO)
    private Long reportId;
    private Long projectId;
    private Long commentId;
    private Integer targetType;
    private Long userId;
    private String reason;
    private Integer status; // 0待处理 1已处理 2已驳回
    private LocalDateTime createTime;
    private String handleResult;
    private Long adminId;

    // 非数据库字段，用于前端展示
    @TableField(exist = false)
    private String projectTitle;
    @TableField(exist = false)
    private String reporterName;
    @TableField(exist = false)
    private String commentContent;
}

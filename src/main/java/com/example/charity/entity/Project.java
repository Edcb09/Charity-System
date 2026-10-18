package com.example.charity.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("project")
public class Project {
    @TableId(value = "project_id", type = IdType.AUTO)//加上vlaue="project_id"防止id不匹配
    private Long projectId;
    private Long userId;
    private String title;
    private String description;
    private String recipientInfo;
    private String proofImg;
    private BigDecimal targetAmount;
    private BigDecimal currentAmount;
    private LocalDateTime endTime;
    private String projectType;
    private Integer status;//0草稿 1待审核 2募捐中 3已结束 4驳回 5已归档
    private String rejectReason;
    private String coverImg;
    private LocalDateTime createTime;
    private LocalDateTime auditTime;

    //非数据库字段，用于前端展示申请人昵称
    @TableField(exist=false)
    private String applicantName;

}

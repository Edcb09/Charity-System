package com.example.charity.entity;


import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.awt.image.Kernel;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("fund_report")
public class FundReport {
    @TableId(type= IdType.AUTO)
    private Long reportId;
    private Long projectId;
    private String content;
    private String reportImg;
    private BigDecimal reportAmount;
    private Integer status;
    private String rejectReason;
    private LocalDateTime createTime;
    private LocalDateTime auditTime;
}

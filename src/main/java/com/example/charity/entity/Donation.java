package com.example.charity.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("donation")
public class Donation {
    @TableId(type= IdType.AUTO)
    private Long donationId;
    @TableField(exist = false)
    private String projectTitle;
    private String orderNo;
    private Long userId;
    private Long projectId;
    private BigDecimal amount;
    private Integer isAnonymous;
    private Integer payStatus;//0待支付 1已支付
    private LocalDateTime donateTime;
    private String remark;

}

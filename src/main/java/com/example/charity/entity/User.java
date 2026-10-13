package com.example.charity.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("user")//对应数据库的表名
public class User {
    @TableId(type=IdType.AUTO)//自增主键
    private Long userId;
    private String username;
    private String password;
    private String realName;
    private String phone;
    private Integer role;
    private LocalDateTime createTime;
    private Integer status;

}

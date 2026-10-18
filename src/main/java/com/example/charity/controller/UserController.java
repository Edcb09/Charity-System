package com.example.charity.controller;

import com.example.charity.common.Result;
import com.example.charity.entity.User;
import com.example.charity.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import cn.dev33.satoken.stp.StpUtil;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import java.time.LocalDateTime;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/user")
public class UserController {
    @Autowired
    private UserService userService;

    @GetMapping("/list")
    public Result<List<User>> list()
    {
        return Result.success(userService.list());
    }

    private final BCryptPasswordEncoder passwordEncoder=new BCryptPasswordEncoder();

    //1.注册接口
    @PostMapping("/register")
    public Result<String> register(@RequestBody User user)
    {
        //检查账号是否已存在
        long count =userService.count(new QueryWrapper<User>().eq("username",user.getUsername()));
        if(count>0)
        {
            return Result.error("账号已存在");
        }
        //密码加密
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setRole(0);//默认捐赠人（普通用户）
        user.setStatus(0);//默认账号状态正常
        user.setCreateTime(LocalDateTime.now());
        userService.save(user);
        return Result.success("注册成功");
    }

    //2.登录接口
//    @PostMapping("/login")
//    public Result<String> login(@RequestBody User user)
//    {
//        User dbUser=userService.getOne(new QueryWrapper<User>().eq("username",user.getUsername()));
//        if(dbUser==null)
//        {
//            return Result.error("账号不存在");
//        }
//        //校验密码
//        if(!passwordEncoder.matches(user.getPassword(),dbUser.getPassword()))
//        {
//            return Result.error("密码错误");
//        }
//        if (dbUser.getStatus()==1)
//        {
//            return Result.error("账号已被禁用");
//        }
//        //Sa-Token 登录，生成Token返回给前端
//        StpUtil.login(dbUser.getUserId());
//        return Result.success(StpUtil.getTokenValue());
//    }

    //修改登录接口返回类型,把role一起返回给前端
    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestBody User user)
    {
        User dbUser=userService.getOne(new QueryWrapper<User>().eq("username",user.getUsername()));
        if(dbUser==null)
        {
            return Result.error("账号不存在");
        }
        //校验密码
        if(!passwordEncoder.matches(user.getPassword(),dbUser.getPassword()))
        {
            return Result.error("密码错误");
        }
        if (dbUser.getStatus()==1)
        {
            return Result.error("账号已被禁用");
        }
        //Sa-Token 登录，生成Token返回给前端
        StpUtil.login(dbUser.getUserId());

        //把Token和用户信息一起返回
        Map<String,Object> map=new HashMap<>();
        map.put("token",StpUtil.getTokenValue());
        map.put("role",dbUser.getRole());
        map.put("username",dbUser.getUsername());

        return Result.success(map);
    }
}

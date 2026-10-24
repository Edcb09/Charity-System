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
        //基础非空校验
        if (user.getUsername() == null || user.getUsername().trim().isEmpty()) {
            return Result.error("账号不能为空");
        }
        if (user.getPassword() == null || user.getPassword().trim().isEmpty()) {
            return Result.error("密码不能为空");
        }

        //长度校验（防止超长导致数据库报错）
        if (user.getUsername().length() < 3 || user.getUsername().length() > 20) {
            return Result.error("账号长度必须在 3-20 个字符之间");
        }
        if (user.getPassword().length() < 6 || user.getPassword().length() > 20) {
            return Result.error("密码长度必须在 6-20 个字符之间");
        }

        //手机号正则校验（如果填了手机号才校验）
        if (user.getPhone() != null && !user.getPhone().trim().isEmpty()) {
            String phoneRegex = "^1[3-9]\\d{9}$";
            if (!user.getPhone().matches(phoneRegex)) {
                return Result.error("手机号格式不正确");
            }
        }

        //检查账号是否已存在
        long count =userService.count(new QueryWrapper<User>().eq("username",user.getUsername()));
        if(count>0)
        {
            return Result.error("账号已存在");
        }

        //安全校验，只能注册捐赠人和受助人
        if(user.getRole()==null||(user.getRole()!=0&&user.getRole()!=1)){
            user.setRole(0);
        }

        //密码加密
        user.setPassword(passwordEncoder.encode(user.getPassword()));
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
        map.put("avatar", dbUser.getAvatar());
        map.put("userId", dbUser.getUserId());

        return Result.success(map);
    }

    // 更新个人信息
    @PostMapping("/updateInfo")
    public Result<String> updateInfo(@RequestBody User user) {
        Long userId = StpUtil.getLoginIdAsLong();

        User oldUser = userService.getById(userId);
        if (oldUser == null) return Result.error("用户不存在");

        // 只允许修改头像、邮箱、真名、手机号
        oldUser.setAvatar(user.getAvatar());
        oldUser.setEmail(user.getEmail());
        oldUser.setRealName(user.getRealName());
        oldUser.setPhone(user.getPhone());

        userService.updateById(oldUser);
        return Result.success("个人信息更新成功");
    }

    // 修改密码
    @PostMapping("/updatePassword")
    public Result<String> updatePassword(@RequestBody Map<String, String> params) {
        Long userId = StpUtil.getLoginIdAsLong();
        User user = userService.getById(userId);

        String oldPwd = params.get("oldPassword");
        String newPwd = params.get("newPassword");

        if (oldPwd == null || newPwd == null || newPwd.trim().isEmpty()) {
            return Result.error("新密码不能为空");
        }
        if (newPwd.length() < 6) {
            return Result.error("新密码长度不能少于 6 位");
        }
        // 校验旧密码
        if (!passwordEncoder.matches(oldPwd, user.getPassword())) {
            return Result.error("原密码输入错误");
        }

        // 更新密码
        user.setPassword(passwordEncoder.encode(newPwd));
        userService.updateById(user);
        return Result.success("密码修改成功，请下次使用新密码登录");
    }

    @GetMapping("/myInfo")
    public Result<User> myInfo() {
        Long userId = StpUtil.getLoginIdAsLong();
        User user = userService.getById(userId);
        if (user != null) {
            user.setPassword(null); // 隐藏密码
        }
        return Result.success(user);
    }
}

package com.example.charity.controller;

import com.example.charity.common.Result;
import com.example.charity.entity.User;
import com.example.charity.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

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
}

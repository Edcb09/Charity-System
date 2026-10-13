package com.example.charity.service.impl;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.example.charity.entity.User;
import com.example.charity.mapper.UserMapper;
import com.example.charity.service.UserService;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper,User> implements UserService{
}

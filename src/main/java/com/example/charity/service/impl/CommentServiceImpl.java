package com.example.charity.service.impl;


import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.example.charity.entity.Comment;
import com.example.charity.mapper.CommentMapper;
import com.example.charity.service.CommentService;
import org.springframework.stereotype.Service;

@Service
public class CommentServiceImpl extends ServiceImpl<CommentMapper, Comment> implements CommentService {
}

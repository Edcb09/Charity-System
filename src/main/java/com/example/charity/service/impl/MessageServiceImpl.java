package com.example.charity.service.impl;

import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.example.charity.entity.Message;
import com.example.charity.mapper.MessageMapper;
import com.example.charity.service.MessageService;
import org.springframework.stereotype.Service;

@Service
public class MessageServiceImpl extends ServiceImpl<MessageMapper, Message> implements MessageService {
}

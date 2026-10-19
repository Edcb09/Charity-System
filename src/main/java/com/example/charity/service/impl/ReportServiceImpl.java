package com.example.charity.service.impl;


import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.example.charity.entity.Report;
import com.example.charity.mapper.ReportMapper;
import com.example.charity.service.ReportService;
import org.springframework.stereotype.Service;

@Service
public class ReportServiceImpl extends ServiceImpl<ReportMapper, Report> implements ReportService {
}

package com.example.charity.service.impl;

import com.example.charity.service.ProjectService;
import org.springframework.stereotype.Service;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.example.charity.entity.Project;
import com.example.charity.mapper.ProjectMapper;

@Service
public class ProjectServiceImpl extends ServiceImpl<ProjectMapper,Project> implements ProjectService {
}

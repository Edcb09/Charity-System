package com.example.charity.mapper;

import org.apache.ibatis.annotations.Mapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.charity.entity.Project;

@Mapper
public interface ProjectMapper extends BaseMapper<Project> {
}

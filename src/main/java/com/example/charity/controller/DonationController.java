package com.example.charity.controller;


import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.charity.common.Result;
import com.example.charity.entity.Donation;
import com.example.charity.entity.Project;
import com.example.charity.entity.User;
import com.example.charity.mapper.ProjectMapper;
import com.example.charity.service.DonationService;
import com.example.charity.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/donation")
public class DonationController {
    @Autowired
    private DonationService donationService;
    @Autowired
    private ProjectMapper projectMapper;
    @Autowired
    private UserService userService;

    //创建捐款订单
    @PostMapping("/create")
    public Result<String> create(@RequestBody Map<String,Object> params)
    {
        //获取当前登录用户ID
        Long userId= StpUtil.getLoginIdAsLong();
        User currentUser=userService.getById(userId);
        if(currentUser.getRole()!=0){
            return Result.error("只有普通用户才能进行捐款");
        }
        Long projectId=Long.valueOf(params.get("projectId").toString());
        BigDecimal amount=new BigDecimal(params.get("amount").toString());
        boolean isAnonymous=Boolean.parseBoolean(params.get("isAnonymous").toString());

        String oderNo=donationService.createDonation(userId,projectId,amount,isAnonymous);
        return Result.success(oderNo);
    }

    //模拟支付成功回调
    @PostMapping("/pay")
    public Result<String> pay(@RequestBody Map<String,String> params)
    {
        String orderNo=params.get("orderNo");
        donationService.paySuccess(orderNo);
        return Result.success("支付成功");
    }

    //分页查询“我”的捐款记录
    @GetMapping("/myList")
    public Result<com.baomidou.mybatisplus.extension.plugins.pagination.Page<Donation>> myList(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize)
    {

        //获取当前登录用户ID
        Long userId=StpUtil.getLoginIdAsLong();

        com.baomidou.mybatisplus.extension.plugins.pagination.Page<Donation> page=new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(pageNum,pageSize);
        QueryWrapper<Donation> wrapper=new QueryWrapper<>();

        //只能看自己已支付的记录
        wrapper.eq("user_id",userId).eq("pay_status",1).orderByDesc("donate_time");

        donationService.page(page,wrapper);

        //遍历结果，把项目名称展示出来
        List<Long> projectIdList=page.getRecords().stream()
                .map(Donation::getProjectId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        if(!projectIdList.isEmpty()) {
            //一次性批量查询所有项目
            List<Project> projectList = projectMapper.selectBatchIds(projectIdList);
            Map<Long, String> projectMap = projectList.stream()
                    .collect(Collectors.toMap(Project::getProjectId, Project::getTitle));
            //循环赋值
            for(Donation record:page.getRecords())
            {
                String title=projectMap.get(record.getProjectId());
                record.setProjectTitle(title);
            }
        }

        return Result.success(page);
    }

}

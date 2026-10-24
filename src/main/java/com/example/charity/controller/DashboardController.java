package com.example.charity.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.charity.common.Result;
import com.example.charity.entity.Donation;
import com.example.charity.entity.Project;
import com.example.charity.entity.User;
import com.example.charity.service.DonationService;
import com.example.charity.service.ProjectService;
import com.example.charity.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/admin/dashboard")
public class DashboardController {

    @Autowired private UserService userService;
    @Autowired private ProjectService projectService;
    @Autowired private DonationService donationService;

    @GetMapping("/data")
    public Result<Map<String, Object>> getDashboardData() {
        // 1. 权限校验
        Long userId = StpUtil.getLoginIdAsLong();
        User currentUser = userService.getById(userId);
        if (currentUser == null || currentUser.getRole() != 2) {
            return Result.error("无权限访问");
        }

        Map<String, Object> result = new HashMap<>();

        // ========== 这里插入你这段过滤逻辑 ==========
        // 获取所有非下架项目的 ID
        List<Long> validProjectIds = projectService.list(new QueryWrapper<Project>().ne("status", 7))
                .stream().map(Project::getProjectId).collect(Collectors.toList());

        // 统计已支付捐款，只统计有效项目
        QueryWrapper<Donation> paidWrapper = new QueryWrapper<Donation>().eq("pay_status", 1);
        if (!validProjectIds.isEmpty()) {
            paidWrapper.in("project_id", validProjectIds);
        } else {
            paidWrapper.eq("project_id", -1);
        }


        // 2. 顶部核心数据卡片
        long totalUsers = userService.count();
        long totalProjects = projectService.count();

        long totalDonations = donationService.count(paidWrapper);
        List<Donation> allPaid = donationService.list(paidWrapper);
        BigDecimal totalAmount = allPaid.stream()
                .map(Donation::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);


        Map<String, Object> cards = new HashMap<>();
        cards.put("totalUsers", totalUsers);
        cards.put("totalProjects", totalProjects);
        cards.put("totalDonations", totalDonations);
        cards.put("totalAmount", totalAmount);
        result.put("cards", cards);

        // 3. 近7天捐款趋势 (折线图)
        List<String> last7Days = new ArrayList<>();
        List<BigDecimal> dailyAmounts = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM-dd");

        // 按日期分组统计
        Map<String, BigDecimal> dailyMap = allPaid.stream()
                .filter(d -> d.getDonateTime() != null)
                .collect(Collectors.groupingBy(
                        d -> d.getDonateTime().format(formatter),
                        Collectors.reducing(BigDecimal.ZERO, Donation::getAmount, BigDecimal::add)
                ));

        for (int i = 6; i >= 0; i--) {
            String dateStr = LocalDate.now().minusDays(i).format(formatter);
            last7Days.add(dateStr);
            dailyAmounts.add(dailyMap.getOrDefault(dateStr, BigDecimal.ZERO));
        }

        Map<String, Object> trend = new HashMap<>();
        trend.put("dates", last7Days);
        trend.put("amounts", dailyAmounts);
        result.put("trend", trend);

        // 4. 项目类型占比 (饼图)
        List<Project> allProjects = projectService.list();
        Map<String, Long> typeMap = allProjects.stream()
                .filter(p -> p.getProjectType() != null)
                .collect(Collectors.groupingBy(Project::getProjectType, Collectors.counting()));

        List<Map<String, Object>> typeData = new ArrayList<>();
        for (Map.Entry<String, Long> entry : typeMap.entrySet()) {
            Map<String, Object> map = new HashMap<>();
            map.put("name", entry.getKey());
            map.put("value", entry.getValue());
            typeData.add(map);
        }
        result.put("projectTypes", typeData);

        // 5. 筹款金额 Top5 项目 (柱状图)
        List<Project> top5 = projectService.list(new QueryWrapper<Project>().ne("status", 7))
                .stream()
                .sorted((p1, p2) -> p2.getCurrentAmount().compareTo(p1.getCurrentAmount()))
                .limit(5)
                .collect(Collectors.toList());

        List<String> top5Titles = top5.stream().map(Project::getTitle).collect(Collectors.toList());
        List<BigDecimal> top5Amounts = top5.stream().map(Project::getCurrentAmount).collect(Collectors.toList());

        Map<String, Object> top5Map = new HashMap<>();
        top5Map.put("titles", top5Titles);
        top5Map.put("amounts", top5Amounts);
        result.put("top5", top5Map);

        return Result.success(result);
    }
}
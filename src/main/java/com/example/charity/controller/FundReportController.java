package com.example.charity.controller;


import cn.dev33.satoken.annotation.SaIgnore;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.example.charity.common.Result;
import com.example.charity.entity.FundReport;
import com.example.charity.entity.Message;
import com.example.charity.entity.Project;
import com.example.charity.entity.User;
import com.example.charity.service.FundReportService;
import com.example.charity.service.MessageService;
import com.example.charity.service.ProjectService;
import com.example.charity.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/fund-report")
public class FundReportController {

    @Autowired private FundReportService fundReportService;
    @Autowired private ProjectService projectService;
    @Autowired private UserService userService;
    @Autowired private MessageService messageService;

    //1.申请人提交资金报告（需登录）
    @PostMapping("/add")
    public Result<String> add(@RequestBody FundReport report){
        Long userId = StpUtil.getLoginIdAsLong();
        Project project =projectService.getById(report.getProjectId());

        if(project==null) return Result.error("项目不存在");
        if(!project.getUserId().equals(userId))return Result.error("你不是该项目的申请人！");
        if(project.getStatus()!=3)return Result.error("只有已结束的项目才能提交资金报告");

        // 金额校验：保证"结余不能为负"
        // 已占用金额 = 已公示(1) + 待审核(0) 的报告金额之和（待审核的也纳入计算）
        BigDecimal amount=report.getReportAmount();
        if(amount==null||amount.compareTo(BigDecimal.ZERO)<=0)
        {
            return Result.error("支出金额必须大于 0");
        }
        BigDecimal totalRaised=project.getCurrentAmount()==null?BigDecimal.ZERO:project.getCurrentAmount();
        BigDecimal usedAmount=BigDecimal.ZERO;
        List<FundReport> usedList=fundReportService.list(new QueryWrapper<FundReport>()
                .eq("project_id",report.getProjectId())
                .in("status",0,1));
        for(FundReport r:usedList)
        {
            if(r.getReportAmount()!=null) usedAmount=usedAmount.add(r.getReportAmount());
        }
        BigDecimal remain=totalRaised.subtract(usedAmount);
        if(amount.compareTo(remain)>0)
        {
            return Result.error("本次支出金额不能超过项目剩余结余（结余不能为负）：已筹 ￥"
                    + totalRaised + "，已申报/已公示 ￥" + usedAmount + "，剩余可申报 ￥" + remain);
        }

        report.setStatus(0);//待审核
        report.setCreateTime(LocalDateTime.now());
        fundReportService.save(report);
        return Result.success("资金报告提交成功，等待管理员审核");
    }

    //2.获取某个项目的公示列表（游客可见）
    @SaIgnore
    @GetMapping("/list/{projectId}")
    public Result<List<FundReport>> list(@PathVariable Long projectId)
    {
        QueryWrapper<FundReport> wrapper=new QueryWrapper<>();

        //只展示审核通过的项目公示
        wrapper.eq("project_id",projectId).eq("status",1).orderByDesc("create_time");
        return Result.success(fundReportService.list(wrapper));
    }

    //3.管理员获取待审核报告列表
    @GetMapping("/admin/pending")
    public Result<List<FundReport>> adminPending()
    {
        Long userId=StpUtil.getLoginIdAsLong();
        User currentUser=userService.getById(userId);
        if(currentUser==null||currentUser.getRole()!=2)return Result.error("无权限");

        QueryWrapper<FundReport> wrapper=new QueryWrapper<>();
        wrapper.eq("status",0).orderByAsc("create_time");
        return Result.success(fundReportService.list(wrapper));
    }

    //4.管理员审核资金报告
    @PostMapping("/admin/audit")
    public Result<String> adminAudit (@RequestBody Map<String,Object> params)
    {
        Long userId=StpUtil.getLoginIdAsLong();
        User currentUser=userService.getById(userId);
        if(currentUser==null||currentUser.getRole()!=2)return Result.error("无权限");

        Long reportId=Long.valueOf(params.get("reportId").toString());
        Integer status=Integer.valueOf(params.get("status").toString());//1公示 2驳回
        String rejectReason=params.get("rejectReason")!=null?params.get("rejectReason").toString():"";

        FundReport report=fundReportService.getById(reportId);
        if(report==null) return Result.error("报告不存在");
        if(report.getStatus()!=0) return Result.error("该报告已审核过，请勿重复处理");

        // 并发防护（乐观锁思想）：只有 status 仍为 0（待审核）时才能更新，
        // 后到的管理员更新不到任何行，直接失败，避免重复公示/重复驳回
        LambdaUpdateWrapper<FundReport> auditWrapper=new LambdaUpdateWrapper<>();
        auditWrapper.eq(FundReport::getReportId,reportId)
                .eq(FundReport::getStatus,0)
                .set(FundReport::getStatus,status)
                .set(FundReport::getAuditTime,LocalDateTime.now())
                .set(status==2,FundReport::getRejectReason,rejectReason);
        if(!fundReportService.update(auditWrapper))
        {
            return Result.error("该报告已被其他管理员处理，请刷新列表");
        }

        // 通知项目申请人审核结果（type=3 资金公示），否则申请人不知道报告被驳回、也不清楚为何不能结项
        Project project=projectService.getById(report.getProjectId());
        if(project!=null)
        {
            Message msg=new Message();
            msg.setReceiveUserId(project.getUserId());
            msg.setType(3); // 3=资金公示
            msg.setLinkId(report.getProjectId());
            msg.setIsRead(0);
            msg.setCreateTime(LocalDateTime.now());
            if(status==1)
            {
                // 公示通过后，如果结余已经为 0，顺带提示可以去申请结项归档
                BigDecimal remain=calcRemain(project);
                msg.setContent("您提交的项目【"+project.getTitle()+"】资金使用报告（￥"+report.getReportAmount()+"）已审核通过并公示。"
                        +(remain.compareTo(BigDecimal.ZERO)<=0
                            ? "当前结余已为 0，您可以申请结项归档了。"
                            : "当前仍有 ￥"+remain+" 元未公示，请继续提交资金报告。"));
            }
            else
            {
                msg.setContent("您提交的项目【"+project.getTitle()+"】资金使用报告（￥"+report.getReportAmount()+"）被驳回。理由："
                        +(rejectReason==null||rejectReason.isEmpty()?"未填写":rejectReason)+"，请修改后重新提交。");
            }
            messageService.save(msg);
        }

        return Result.success("审核完成");
    }

    // 计算项目结余：已筹金额 - 已公示(status=1)的报告金额之和（与结项校验口径一致）
    private BigDecimal calcRemain(Project project)
    {
        BigDecimal totalRaised=project.getCurrentAmount()==null?BigDecimal.ZERO:project.getCurrentAmount();
        BigDecimal used=BigDecimal.ZERO;
        List<FundReport> passed=fundReportService.list(new QueryWrapper<FundReport>()
                .eq("project_id",project.getProjectId())
                .eq("status",1));
        for(FundReport r:passed)
        {
            if(r.getReportAmount()!=null) used=used.add(r.getReportAmount());
        }
        return totalRaised.subtract(used);
    }

    // 获取项目的财务汇总信息（已筹、已支出、结余）
    @SaIgnore
    @GetMapping("/summary/{projectId}")
    public Result<Map<String, Object>> getSummary(@PathVariable Long projectId) {
        Project project = projectService.getById(projectId);
        if (project == null) return Result.error("项目不存在");
        if (project.getStatus() != 3 && project.getStatus() != 5 && project.getStatus() != 6) {
            return Result.error("项目尚未结束，无法查看资金汇总");
        }

        // 1. 已筹金额
        BigDecimal totalRaised = project.getCurrentAmount();

        // 2. 已支出金额（只统计已公示 status=1 的报告金额）
        QueryWrapper<FundReport> wrapper = new QueryWrapper<>();
        wrapper.eq("project_id", projectId).eq("status", 1);
        List<FundReport> reports = fundReportService.list(wrapper);

        BigDecimal totalExpended = BigDecimal.ZERO;
        for (FundReport r : reports) {
            if (r.getReportAmount() != null) {
                totalExpended = totalExpended.add(r.getReportAmount());
            }
        }

        // 3. 结余金额
        BigDecimal balance = totalRaised.subtract(totalExpended);

        Map<String, Object> map = new HashMap<>();
        map.put("totalRaised", totalRaised);
        map.put("totalExpended", totalExpended);
        map.put("balance", balance);

        return Result.success(map);
    }




}

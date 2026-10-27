package com.example.charity.controller;


import ch.qos.logback.core.pattern.util.RegularEscapeUtil;
import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaIgnore;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.charity.common.Result;
import com.example.charity.entity.Message;
import com.example.charity.entity.Project;
import com.example.charity.entity.User;
import com.example.charity.service.MessageService;
import com.example.charity.service.ProjectService;
import com.example.charity.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/project")
public class ProjectController {

    @Autowired
    private ProjectService projectService;
    @Autowired
    private UserService userService;
    @Autowired
    private MessageService messageService;
    @Autowired
    private com.example.charity.service.FundReportService fundReportService;

    // 查询项目列表（支持分页、搜索、筛选、Tab状态筛选）
    @SaIgnore
    @GetMapping("/list")
    public Result<Page<Project>> list(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String projectType,
            @RequestParam(required = false) Integer status) { //  status 参数

        Page<Project> page = new Page<>(pageNum, pageSize);
        QueryWrapper<Project> queryWrapper = new QueryWrapper<>();

        // 1. 基础条件：只能看公开的（募捐中2、已结束3、已归档5、待归档6
        // 修改：新增下架7，admin可看7，其他不可看
        if (status != null && status == 7) {
            // 只有管理员才能查已下架的
            User user = userService.getById(StpUtil.getLoginIdAsLong());
            if (user == null || user.getRole() != 2) {
                return Result.error("无权查看该状态");
            }
            queryWrapper.eq("status", 7);
        } else if (status != null) {
            if (status == 3) queryWrapper.in("status", 3, 6);
            else queryWrapper.eq("status", status);
        } else {
            // 如果没传 status（查全部）
            // 游客也可能访问（该方法已 @SaIgnore），这里不能用 getLoginIdAsLong()，否则未登录会抛 NotLoginException 返回 401
            Object loginId = StpUtil.getLoginIdDefaultNull();
            User user = loginId == null ? null : userService.getById(Long.valueOf(loginId.toString()));
            if (user != null && user.getRole() == 2) {
                queryWrapper.in("status", 2, 3, 5, 6, 7); // 管理员看 7
            } else {
                queryWrapper.in("status", 2, 3, 5, 6);    // 普通用户不看 7
            }
        }

        // 2. 模糊搜索
        if (keyword != null && !keyword.isEmpty()) {
            queryWrapper.and(w -> w.like("title", keyword).or().like("recipient_info", keyword));
        }
        // 3. 分类筛选
        if (projectType != null && !projectType.isEmpty()) {
            queryWrapper.eq("project_type", projectType);
        }

        // 4. Tab 状态筛选
        if (status != null) {
            if (status == 2) {
                queryWrapper.eq("status", 2); // 募捐中
            } else if (status == 3) {
                queryWrapper.in("status", 3, 6); // 已结束（包含状态6待归档）
            } else if (status == 5) {
                queryWrapper.eq("status", 5); // 已归档
            }
        } else {
            // 5. 如果选择了“全部”（status 为空），优先把“募捐中(2)”排在最前面
            // 使用 CASE WHEN 排序，将状态为 2 的权重设为 0，其他的设为 1
            queryWrapper.last("ORDER BY CASE WHEN status = 2 THEN 0 ELSE 1 END, create_time DESC");
        }

        // 如果指定了状态，正常按时间倒序
        if (status != null) {
            queryWrapper.orderByDesc("create_time");
        }

        projectService.page(page, queryWrapper);
        return Result.success(page);
    }

    //获取项目详情
    @SaIgnore
    @GetMapping("/{id}")
    public Result<Project> getDetail(@PathVariable Long id)
    {
        Project project=projectService.getById(id);
        if(project==null)
        {
            return Result.error("项目不存在");
        }

        //新增：查询申请人的用户名，返回给前端
        User applicant =userService.getById(project.getUserId());
        if(applicant!=null){
            project.setApplicantName(applicant.getUsername());
        }
        return Result.success(project);
    }

    //1.申请募捐项目，存草稿
    @SaCheckLogin
    @PostMapping("/add")
    public Result<String> addProject(@RequestBody Project project)
    {
        //获取当前登录用户的ID作为申请人
        Long userId = StpUtil.getLoginIdAsLong();
        User currentUser=userService.getById(userId);
        if(currentUser.getRole()!=1)
        {
            return Result.error("普通捐赠人无权发起项目，请联系管理员更改角色");
        }
        project.setUserId(userId);
        project.setCurrentAmount(BigDecimal.ZERO);

        if(project.getStatus()==null){
        project.setStatus(1);//待审核
        }

        project.setCreateTime(LocalDateTime.now());
        projectService.save(project);
        return Result.success(project.getStatus()==0?"草稿保存成功":"项目提交成功，等待管理员审核");
    }

    // 2. 修改项目（用于编辑草稿或驳回后重新提交）
    // 修改项目
    @PutMapping("/update")
    public Result<String> updateProject(@RequestBody Project project) {
        Long userId = StpUtil.getLoginIdAsLong();
        Project oldProject = projectService.getById(project.getProjectId());

        if (oldProject == null) return Result.error("项目不存在");
        if (!oldProject.getUserId().equals(userId)) return Result.error("无权修改该项目");

        // 只有草稿(0) 或 被驳回(4) 的项目才能修改
        if (oldProject.getStatus() != 0 && oldProject.getStatus() != 4) {
            return Result.error("只有草稿或被驳回的项目才能修改");
        }

        // 拦截：如果原状态是 4（被驳回），强制将新状态设为 1（待审核），不允许存草稿
        if (oldProject.getStatus() == 4) {
            project.setStatus(1);
        }

        // 防止前端恶意篡改用户ID、已筹金额和时间
        project.setUserId(userId);
        project.setCurrentAmount(oldProject.getCurrentAmount());
        project.setCreateTime(oldProject.getCreateTime());

        projectService.updateById(project);

        if (project.getEndTime() != null) {
            // 如果提交上来的时间在当前时间之前
            if (project.getEndTime().isBefore(java.time.LocalDateTime.now())) {
                return Result.error("项目截止时间不能早于当前时间，请重新选择");
            }
        }

        //修改成功提示语
        if (oldProject.getStatus() == 4) {
            return Result.success("项目重新提交成功，等待审核");
        }
        return Result.success(project.getStatus() == 0 ? "草稿更新成功" : "项目重新提交成功，等待审核");
    }

    //获取审核项目列表
    @GetMapping("/admin/pending")
    public Result<List<Project>> getPendingProjects()
    {
        //通过role进行权限校验
        Long userId=StpUtil.getLoginIdAsLong();
        User currentUser=userService.getById(userId);
        if(currentUser==null||currentUser.getRole()!=2)
        {
            return Result.error("无权限操作，仅管理员可用");
        }

        QueryWrapper<Project> wrapper=new QueryWrapper<>();
        wrapper.eq("status",1).orderByDesc("create_time");
        List<Project> list=projectService.list(wrapper);

        //填入申请人的名字
        for(Project p:list)
        {
            User applicant=userService.getById(p.getUserId());
            if(applicant!=null) p.setApplicantName(applicant.getUsername());
        }
        return Result.success(list);
    }

    //审核项目
    @PostMapping("/admin/audit")
    public Result<String> auditProject(@RequestBody Map<String,Object> params)
    {
        Long userId=StpUtil.getLoginIdAsLong();
        User currentUser=userService.getById(userId);
        if(currentUser==null||currentUser.getRole()!=2)
        {
            return Result.error("无权限操作");
        }

        Long projectId=Long.valueOf(params.get("projectId").toString());
        Integer status=Integer.valueOf(params.get("status").toString());
        String rejectReason=params.get("rejectReason")!=null?params.get("rejectReason").toString():"";

        Project project=projectService.getById(projectId);
        if (project==null)return Result.error("项目不存在");
        if(project.getStatus()!=1)
        {
            return Result.error("该项目已被处理，请勿重复审核");
        }

        // 并发防护（乐观锁思想）：UPDATE 时带上 status=1 的条件，
        // 只有把状态从"待审核"改掉的那一个请求能成功；后到的管理员更新行数为 0，直接返回失败，
        // 避免两个管理员同时审核导致"驳回+通过"都生效、还各发一条站内信
        LambdaUpdateWrapper<Project> auditWrapper=new LambdaUpdateWrapper<>();
        auditWrapper.eq(Project::getProjectId,projectId)
                .eq(Project::getStatus,1)
                .set(Project::getStatus,status)
                .set(Project::getAuditTime,LocalDateTime.now())
                .set(status==4,Project::getRejectReason,rejectReason);
        if(!projectService.update(auditWrapper))
        {
            return Result.error("该项目已被其他管理员处理，请刷新列表");
        }

        //发送审核结果通知
        Message msg=new Message();
        msg.setReceiveUserId(project.getUserId());
        if(status==2)
        {
            msg.setContent("您发起的项目【"+project.getTitle()+"】已通过审核，募捐项目已启动。");
        }else{
            msg.setContent("很遗憾，您发起的项目【"+project.getTitle()+"】未通过审核。理由："+rejectReason);
        }
        msg.setType(1);
        msg.setLinkId(projectId);
        msg.setIsRead(0);
        msg.setCreateTime(LocalDateTime.now());
        messageService.save(msg);

        return Result.success("审核操作成功");
    }

    // 获取当前登录用户发起的项目列表（分页）
    @GetMapping("/my")
    public Result<Page<Project>> myProjects(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        Long userId = StpUtil.getLoginIdAsLong();

        // 创建分页对象
        Page<Project> page = new Page<>(pageNum, pageSize);

        QueryWrapper<Project> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId).orderByDesc("create_time");

        // 执行分页查询
        projectService.page(page, wrapper);

        return Result.success(page);
    }


    // 管理员主动下架项目
    @PostMapping("/admin/offline/{id}")
    public Result<String> offlineProject(@PathVariable Long id) {
        Long userId = StpUtil.getLoginIdAsLong();
        User currentUser = userService.getById(userId);
        if (currentUser == null || currentUser.getRole() != 2) return Result.error("无权限");

        Project project = projectService.getById(id);
        if (project == null) return Result.error("项目不存在");

        // 只有"已发布但未归档"的项目才能强制下架：
        // 0草稿/1待审核/4已驳回 尚未发布（走审核驳回或申请人删除即可）
        // 5已归档 是合规终态（下架会抹掉历史资金公示，并影响统计与导出流水）
        int st = project.getStatus();
        if (st == 7) return Result.error("该项目已处于下架状态");
        if (st != 2 && st != 3 && st != 6) {
            return Result.error("当前状态不可强制下架（仅募捐中 / 已结束 / 结项待审核可下架）");
        }

        // 并发防护（乐观锁思想）：只有状态仍属于可下架范围时才更新成功，
        // 避免两个管理员同时下架、或下架与归档审核并发导致重复下发站内信
        LambdaUpdateWrapper<Project> offlineWrapper = new LambdaUpdateWrapper<>();
        offlineWrapper.eq(Project::getProjectId, id)
                .in(Project::getStatus, 2, 3, 6)
                .set(Project::getStatus, 7);
        if (!projectService.update(offlineWrapper)) {
            return Result.error("该项目状态已被其他操作变更，请刷新后重试");
        }

        // 发通知
        Message msg = new Message();
        msg.setReceiveUserId(project.getUserId());
        msg.setContent("您的项目【" + project.getTitle() + "】因违反平台规定，已被管理员强制下架。");
        msg.setType(5); // 5=系统通知（管理员处置，不是审核流程）
        msg.setLinkId(project.getProjectId());
        msg.setIsRead(0);
        msg.setCreateTime(LocalDateTime.now());
        messageService.save(msg);

        return Result.success("项目已强制下架");
    }

    // 申请人删除自己的项目
    @DeleteMapping("/{id}")
    public Result<String> deleteProject(@PathVariable Long id) {
        Long userId = StpUtil.getLoginIdAsLong();
        Project project = projectService.getById(id);
        if (project == null) return Result.error("项目不存在");
        if (!project.getUserId().equals(userId)) return Result.error("无权操作该项目");

        // 允许彻底删除的范围：草稿(0)、已驳回(4)、已下架(7)、0元已归档(5)
        // 其余状态（1待审核 / 2募捐中 / 3已结束 / 6结项待审核 / 有筹款的已归档）
        // 已进入业务流程，申请人不能自行删除，否则等于变相拥有了管理员的下架能力
        int st = project.getStatus();
        boolean canDelete = st == 0 || st == 4 || st == 7
                || (st == 5 && project.getCurrentAmount() != null
                    && project.getCurrentAmount().compareTo(BigDecimal.ZERO) == 0);
        if (!canDelete) {
            return Result.error("项目已进入流程，无法自行删除；如需下架请联系管理员");
        }

        try {
            projectService.removeById(id);
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            // 有外键关联记录（捐款 / 留言 / 资金报告 / 举报）时删不掉，给个明确提示而不是 500
            return Result.error("该项目存在关联记录（捐款、留言或资金报告），无法彻底删除");
        }
        return Result.success("项目已彻底删除");
    }

    // ================= 项目归档相关接口 =================

    // 1. 申请人申请结项 -> 状态变为 6
    @PostMapping("/applyFinish/{id}")
    public Result<String> applyFinish(@PathVariable Long id) {
        Long userId = StpUtil.getLoginIdAsLong();
        Project project = projectService.getById(id);

        if (project == null) return Result.error("项目不存在");
        if (!project.getUserId().equals(userId)) return Result.error("您无权操作该项目");
        if (project.getStatus() != 3) return Result.error("当前状态无法申请结项");

        // 新增：计算结余金额
        java.math.BigDecimal totalRaised = project.getCurrentAmount();
        java.math.BigDecimal totalExpended = java.math.BigDecimal.ZERO;

        // 查询所有已公示（status=1）的资金报告
        com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<com.example.charity.entity.FundReport> wrapper =
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();
        wrapper.eq("project_id", id).eq("status", 1);
        java.util.List<com.example.charity.entity.FundReport> reports = fundReportService.list(wrapper);

        for (com.example.charity.entity.FundReport r : reports) {
            if (r.getReportAmount() != null) {
                totalExpended = totalExpended.add(r.getReportAmount());
            }
        }

        // 计算结余 = 已筹金额 - 已支出金额
        java.math.BigDecimal balance = totalRaised.subtract(totalExpended);

        // 如果结余大于 0，拦截！
        if (balance.compareTo(java.math.BigDecimal.ZERO) > 0) {
            return Result.error("无法申请结项：当前项目仍有 ￥" + balance + " 元资金未公示支出，请先补充资金报告并等待审核通过。");
        }


        // 状态从 3 变为 6
        project.setStatus(6);
        projectService.updateById(project);

        // 无需给管理员发站内信：管理员的消息中心入口在前端已隐藏（Layout.vue v-if="role!=='2'"），
        // 待办提醒由导航栏"后台审核"红点承担（/admin/pendingCount 会实时统计 status=6 的待归档项目）
        return Result.success("结项申请已提交，等待管理员审核");
    }

    // 2. 管理员获取待归档(状态6)的项目列表
    @GetMapping("/admin/finished")
    public Result<List<Project>> getFinishedProjects() {
        Long userId = StpUtil.getLoginIdAsLong();
        User currentUser = userService.getById(userId);
        if (currentUser == null || currentUser.getRole() != 2) return Result.error("无权限");

        QueryWrapper<Project> wrapper = new QueryWrapper<>();
        wrapper.eq("status", 6).orderByDesc("audit_time"); // 👈 改为查 6
        return Result.success(projectService.list(wrapper));
    }

    // 3. 管理员同意归档 -> 状态变5
    @PostMapping("/admin/archive/{id}")
    public Result<String> archiveProject(@PathVariable Long id) {
        // ... 权限校验 ...
        Project project = projectService.getById(id);
        if (project == null || project.getStatus() != 6) {
            return Result.error("项目状态错误，无法归档");
        }
        // 并发防护（乐观锁思想）：只有 status 仍为 6（结项待审核）时才能归档，
        // 后到的管理员更新不到任何行，直接失败，避免重复归档
        LambdaUpdateWrapper<Project> archiveWrapper=new LambdaUpdateWrapper<>();
        archiveWrapper.eq(Project::getProjectId,id)
                .eq(Project::getStatus,6)
                .set(Project::getStatus,5);
        if(!projectService.update(archiveWrapper))
        {
            return Result.error("该项目已被其他管理员处理，请刷新列表");
        }
        // 发通知给申请人...
        return Result.success("项目已成功归档");
    }

    // 4. 管理员驳回归档 -> 状态变回 3
    @PostMapping("/admin/rejectArchive")
    public Result<String> rejectArchive(@RequestBody Map<String, Object> params) {
        Long userId = StpUtil.getLoginIdAsLong();
        User currentUser = userService.getById(userId);
        if (currentUser == null || currentUser.getRole() != 2) return Result.error("无权限");

        Long projectId = Long.valueOf(params.get("projectId").toString());
        String rejectReason = params.get("rejectReason").toString();

        Project project = projectService.getById(projectId);
        if (project == null || project.getStatus() != 6) return Result.error("状态错误");

        // 并发防护（乐观锁思想）：只有 status 仍为 6（结项待审核）时才能驳回，
        // 后到的管理员更新不到任何行，直接失败，避免与"同意归档"互相覆盖
        LambdaUpdateWrapper<Project> rejectWrapper=new LambdaUpdateWrapper<>();
        rejectWrapper.eq(Project::getProjectId,projectId)
                .eq(Project::getStatus,6)
                .set(Project::getStatus,3)// 退回已结束
                .set(Project::getRejectReason,"归档申请被驳回：" + rejectReason);// 借用这个字段存驳回理由
        if(!projectService.update(rejectWrapper))
        {
            return Result.error("该项目已被其他管理员处理，请刷新列表");
        }

        // 发通知给申请人
        Message msg = new Message();
        msg.setReceiveUserId(project.getUserId());
        msg.setContent("您的项目【" + project.getTitle() + "】归档申请被驳回。理由：" + rejectReason + "，请继续补充资金报告后重新申请。");
        msg.setType(1);
        msg.setLinkId(project.getProjectId());
        msg.setIsRead(0);
        msg.setCreateTime(java.time.LocalDateTime.now());
        messageService.save(msg);

        return Result.success("已驳回结项申请");
    }

}

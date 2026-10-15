package com.serviceflow.service;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.serviceflow.common.api.PageResult;
import com.serviceflow.common.enums.ErrorCode;
import com.serviceflow.common.enums.TicketAction;
import com.serviceflow.common.enums.TicketPriority;
import com.serviceflow.common.enums.TicketStatus;
import com.serviceflow.common.exception.BusinessException;
import com.serviceflow.common.util.TicketNoGenerator;
import com.serviceflow.dto.ticket.TicketAssignRequest;
import com.serviceflow.dto.ticket.TicketCancelRequest;
import com.serviceflow.dto.ticket.TicketCompleteRequest;
import com.serviceflow.dto.ticket.TicketConfirmRequest;
import com.serviceflow.dto.ticket.TicketCreateRequest;
import com.serviceflow.dto.ticket.TicketProcessRequest;
import com.serviceflow.dto.ticket.TicketQuery;
import com.serviceflow.dto.ticket.TicketRejectRequest;
import com.serviceflow.dto.ticket.TicketReopenRequest;
import com.serviceflow.dto.ticket.TicketTransferRequest;
import com.serviceflow.entity.Customer;
import com.serviceflow.entity.Device;
import com.serviceflow.entity.FaultCategory;
import com.serviceflow.entity.ServiceTicket;
import com.serviceflow.entity.SysRole;
import com.serviceflow.entity.SysUser;
import com.serviceflow.entity.TicketAssignment;
import com.serviceflow.entity.TicketAttachment;
import com.serviceflow.entity.TicketComment;
import com.serviceflow.entity.TicketEvaluation;
import com.serviceflow.entity.TicketProcessRecord;
import com.serviceflow.mapper.CustomerMapper;
import com.serviceflow.mapper.DeviceMapper;
import com.serviceflow.mapper.FaultCategoryMapper;
import com.serviceflow.mapper.ServiceTicketMapper;
import com.serviceflow.mapper.SysRoleMapper;
import com.serviceflow.mapper.SysUserMapper;
import com.serviceflow.mapper.TicketAssignmentMapper;
import com.serviceflow.mapper.TicketAttachmentMapper;
import com.serviceflow.mapper.TicketCommentMapper;
import com.serviceflow.mapper.TicketEvaluationMapper;
import com.serviceflow.mapper.TicketProcessRecordMapper;
import com.serviceflow.security.AccessGuard;
import com.serviceflow.security.DataScopeService;
import com.serviceflow.security.SecurityUtils;
import com.serviceflow.security.UserPrincipal;
import com.serviceflow.vo.TicketDetailVO;
import com.serviceflow.vo.EvaluationVO;
import com.serviceflow.vo.TicketRecordVO;
import com.serviceflow.vo.TicketVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Collections;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * 工单核心业务：创建、查询、分派、接单、转派、退回、维修处理、完成、确认、重开与取消。
 *
 * 并发控制：状态流转统一使用「状态 + 版本号」条件更新，影响行数为 0 时区分
 * 状态已被他人改变（1402/1403）与版本冲突（1602）；接单额外使用 Redis 锁降低冲突概率。
 */
@Service
public class TicketService {

    private static final Logger log = LoggerFactory.getLogger(TicketService.class);
    private static final String ACCEPT_LOCK_PREFIX = "ticket:accept:";
    private static final Duration ACCEPT_LOCK_TTL = Duration.ofSeconds(10);
    private static final int MAX_TICKET_NO_RETRY = 5;
    private static final String CONFIG_REOPEN_WINDOW_DAYS = "ticket.reopen_window_days";

    private final ServiceTicketMapper ticketMapper;
    private final TicketAssignmentMapper assignmentMapper;
    private final TicketProcessRecordMapper recordMapper;
    private final CustomerMapper customerMapper;
    private final DeviceMapper deviceMapper;
    private final SysUserMapper sysUserMapper;
    private final SysRoleMapper sysRoleMapper;
    private final FaultCategoryMapper faultCategoryMapper;
    private final DataScopeService dataScopeService;
    private final AccessGuard accessGuard;
    private final ConfigService configService;
    private final RedisLockService redisLockService;
    private final TicketNoGenerator ticketNoGenerator;
    private final TicketEvaluationMapper evaluationMapper;
    private final TicketAttachmentMapper attachmentMapper;
    private final TicketCommentMapper commentMapper;
    private final StatisticsCacheService statisticsCacheService;

    public TicketService(ServiceTicketMapper ticketMapper,
                         TicketAssignmentMapper assignmentMapper,
                         TicketProcessRecordMapper recordMapper,
                         CustomerMapper customerMapper,
                         DeviceMapper deviceMapper,
                         SysUserMapper sysUserMapper,
                         SysRoleMapper sysRoleMapper,
                         FaultCategoryMapper faultCategoryMapper,
                         DataScopeService dataScopeService,
                         AccessGuard accessGuard,
                         ConfigService configService,
                         RedisLockService redisLockService,
                         TicketNoGenerator ticketNoGenerator,
                         TicketEvaluationMapper evaluationMapper,
                         TicketAttachmentMapper attachmentMapper,
                         TicketCommentMapper commentMapper,
                         StatisticsCacheService statisticsCacheService) {
        this.ticketMapper = ticketMapper;
        this.assignmentMapper = assignmentMapper;
        this.recordMapper = recordMapper;
        this.customerMapper = customerMapper;
        this.deviceMapper = deviceMapper;
        this.sysUserMapper = sysUserMapper;
        this.sysRoleMapper = sysRoleMapper;
        this.faultCategoryMapper = faultCategoryMapper;
        this.dataScopeService = dataScopeService;
        this.accessGuard = accessGuard;
        this.configService = configService;
        this.redisLockService = redisLockService;
        this.ticketNoGenerator = ticketNoGenerator;
        this.evaluationMapper = evaluationMapper;
        this.attachmentMapper = attachmentMapper;
        this.commentMapper = commentMapper;
        this.statisticsCacheService = statisticsCacheService;
    }

    // --------------------------------------------------------------- 查询

    /**
     * 工单分页列表，按角色自动收敛数据域：管理员与客服可见全部，
     * 工程师仅见分配给自己的，客户仅见自己的。
     */
    @Transactional(readOnly = true)
    public PageResult<TicketVO> page(TicketQuery query) {
        return pageInternal(query, false);
    }

    /**
     * 逾期工单：状态未结束且已超过截止时间。
     */
    @Transactional(readOnly = true)
    public PageResult<TicketVO> overdue(TicketQuery query) {
        return pageInternal(query, true);
    }

    private PageResult<TicketVO> pageInternal(TicketQuery query, boolean forceOverdue) {
        if (!query.isSortFieldAllowed()) {
            throw BusinessException.of(ErrorCode.SORT_FIELD_INVALID);
        }
        TicketStatus status = TicketStatus.of(query.getStatus());
        if (StringUtils.hasText(query.getStatus()) && status == null) {
            throw BusinessException.of(ErrorCode.PARAM_INVALID, "状态取值不正确：" + query.getStatus());
        }
        // 注意：eq(condition, column, value) 的 value 会被提前求值，因此此处先转成字符串，避免空指针
        String statusName = status == null ? null : status.name();
        Long scopedCustomerId = dataScopeService.scopedCustomerId();
        Long scopedAssigneeId = dataScopeService.scopedAssigneeId();
        boolean overdueOnly = forceOverdue || Boolean.TRUE.equals(query.getOverdue());
        LocalDateTime start = query.getStartDate() == null ? null : query.getStartDate().atStartOfDay();
        LocalDateTime end = query.getEndDate() == null ? null : query.getEndDate().atTime(LocalTime.MAX);

        Page<ServiceTicket> page = query.toPage("createTime");
        Page<ServiceTicket> result = ticketMapper.selectPage(page,
                Wrappers.<ServiceTicket>lambdaQuery()
                        .eq(scopedCustomerId != null, ServiceTicket::getCustomerId, scopedCustomerId)
                        .eq(scopedAssigneeId != null, ServiceTicket::getAssigneeId, scopedAssigneeId)
                        .eq(status != null, ServiceTicket::getStatus, statusName)
                        .eq(StringUtils.hasText(query.getPriority()), ServiceTicket::getPriority, query.getPriority())
                        .eq(query.getCustomerId() != null, ServiceTicket::getCustomerId, query.getCustomerId())
                        .eq(query.getAssigneeId() != null, ServiceTicket::getAssigneeId, query.getAssigneeId())
                        .eq(query.getFaultCategoryId() != null, ServiceTicket::getFaultCategoryId,
                                query.getFaultCategoryId())
                        .ge(start != null, ServiceTicket::getCreateTime, start)
                        .le(end != null, ServiceTicket::getCreateTime, end)
                        .and(StringUtils.hasText(query.getKeyword()), wrapper -> wrapper
                                .like(ServiceTicket::getTicketNo, query.getKeyword())
                                .or().like(ServiceTicket::getTitle, query.getKeyword())
                                .or().like(ServiceTicket::getReporterName, query.getKeyword()))
                        .in(overdueOnly, ServiceTicket::getStatus, List.of(
                                TicketStatus.PENDING_ASSIGN.name(), TicketStatus.PENDING_ACCEPT.name(),
                                TicketStatus.PROCESSING.name(), TicketStatus.PENDING_CONFIRM.name()))
                        .isNotNull(overdueOnly, ServiceTicket::getDeadlineTime)
                        .lt(overdueOnly, ServiceTicket::getDeadlineTime, LocalDateTime.now())
                        .orderByAsc(overdueOnly, ServiceTicket::getDeadlineTime));
        return new PageResult<>(toVOList(result.getRecords()), result.getTotal(),
                result.getCurrent(), result.getSize());
    }

    /**
     * 工单详情：包含客户联系方式、设备信息与完整处理历史，并校验访问关系。
     */
    @Transactional(readOnly = true)
    public TicketDetailVO detail(Long id) {
        ServiceTicket ticket = requireTicket(id);
        assertVisible(ticket);

        TicketDetailVO detail = new TicketDetailVO();
        detail.setTicket(toVOList(List.of(ticket)).get(0));
        detail.setDescription(ticket.getDescription());
        detail.setReporterName(ticket.getReporterName());
        detail.setReporterPhone(ticket.getReporterPhone());
        detail.setAssignTime(ticket.getAssignTime());
        detail.setStartTime(ticket.getStartTime());
        detail.setConfirmTime(ticket.getConfirmTime());
        detail.setClosedTime(ticket.getClosedTime());
        detail.setCancelTime(ticket.getCancelTime());
        detail.setCancelReason(ticket.getCancelReason());
        detail.setRejectCount(ticket.getRejectCount());
        detail.setReopenCount(ticket.getReopenCount());
        detail.setTransferCount(ticket.getTransferCount());

        Customer customer = customerMapper.selectById(ticket.getCustomerId());
        if (customer != null) {
            detail.setCustomerContactName(customer.getContactName());
            detail.setCustomerContactPhone(customer.getContactPhone());
            detail.setCustomerAddress(customer.getAddress());
        }
        Device device = deviceMapper.selectById(ticket.getDeviceId());
        if (device != null) {
            detail.setDeviceBrand(device.getBrand());
            detail.setDeviceModel(device.getModel());
            detail.setDeviceSn(device.getSnNo());
            detail.setDeviceWarrantyExpireDate(device.getWarrantyExpireDate());
            detail.setDeviceInWarranty(device.getWarrantyExpireDate() == null
                    || !device.getWarrantyExpireDate().isBefore(LocalDate.now()));
        }
        detail.setRecords(recordsOf(id));
        detail.setAttachmentCount(attachmentMapper.selectCount(Wrappers.<TicketAttachment>lambdaQuery()
                .eq(TicketAttachment::getTicketId, id)
                .eq(TicketAttachment::getDeleted, 0L)));
        UserPrincipal viewer = SecurityUtils.currentUser();
        boolean staffViewer = viewer.hasRole(AccessGuard.ROLE_ADMIN) || viewer.hasRole(AccessGuard.ROLE_SERVICE)
                || viewer.hasRole(AccessGuard.ROLE_ENGINEER);
        // 客户只能看到公开评论数量，内部备注不外泄
        detail.setCommentCount(commentMapper.selectCount(Wrappers.<TicketComment>lambdaQuery()
                .eq(TicketComment::getTicketId, id)
                .eq(TicketComment::getDeleted, 0L)
                .eq(!staffViewer, TicketComment::getIsInternal, 0)));
        TicketEvaluation evaluation = evaluationMapper.selectOne(Wrappers.<TicketEvaluation>lambdaQuery()
                .eq(TicketEvaluation::getTicketId, id));
        if (evaluation != null) {
            detail.setEvaluation(EvaluationVO.from(evaluation,
                    evaluation.getEvaluatorId().equals(viewer.getUserId())));
        }
        return detail;
    }

    /**
     * 处理历史（按时间正序）。
     */
    @Transactional(readOnly = true)
    public List<TicketRecordVO> records(Long ticketId) {
        assertVisible(requireTicket(ticketId));
        return recordsOf(ticketId);
    }

    /**
     * 校验当前用户可访问该工单并返回工单实体，供评价、附件、评论等模块复用。
     */
    @Transactional(readOnly = true)
    public ServiceTicket requireVisibleTicket(Long ticketId) {
        ServiceTicket ticket = requireTicket(ticketId);
        assertVisible(ticket);
        return ticket;
    }

    /**
     * 追加一条处理记录（供评价等由其他模块触发的动作复用）。
     */
    @Transactional(rollbackFor = Exception.class)
    public void appendRecord(Long ticketId, TicketAction action, String fromStatus, String toStatus, String content) {
        addRecord(requireTicket(ticketId), action, fromStatus, toStatus, content, null);
    }

    // --------------------------------------------------------------- 创建

    /**
     * 创建工单：校验客户与设备归属、生成唯一编号、写入创建记录。
     */
    @Transactional(rollbackFor = Exception.class)
    public TicketVO create(TicketCreateRequest request) {
        Customer customer = customerMapper.selectById(request.getCustomerId());
        if (customer == null || (customer.getDeleted() != null && customer.getDeleted() != 0L)) {
            throw BusinessException.of(ErrorCode.CUSTOMER_NOT_FOUND);
        }
        if (customer.getStatus() == null || customer.getStatus() != 1) {
            throw BusinessException.of(ErrorCode.PARAM_INVALID, "客户已停用，无法创建工单");
        }
        Device device = deviceMapper.selectById(request.getDeviceId());
        if (device == null || (device.getDeleted() != null && device.getDeleted() != 0L)) {
            throw BusinessException.of(ErrorCode.DEVICE_NOT_FOUND);
        }
        if (!device.getCustomerId().equals(request.getCustomerId())) {
            throw BusinessException.of(ErrorCode.DEVICE_CUSTOMER_MISMATCH);
        }
        if (request.getFaultCategoryId() != null) {
            FaultCategory category = faultCategoryMapper.selectById(request.getFaultCategoryId());
            if (category == null || (category.getDeleted() != null && category.getDeleted() != 0L)) {
                throw BusinessException.of(ErrorCode.PARAM_INVALID, "故障分类不存在");
            }
        }
        TicketPriority priority = TicketPriority.of(request.getPriority());
        if (priority == null) {
            throw BusinessException.of(ErrorCode.PARAM_INVALID, "优先级取值不正确");
        }
        LocalDateTime deadline = request.getDeadlineTime() == null
                ? LocalDateTime.now().plusHours(priority.getSlaHours()) : request.getDeadlineTime();
        if (deadline.isBefore(LocalDateTime.now())) {
            throw BusinessException.of(ErrorCode.PARAM_INVALID, "截止时间必须晚于当前时间");
        }

        ServiceTicket ticket = new ServiceTicket();
        ticket.setTitle(request.getTitle());
        ticket.setDescription(request.getDescription());
        ticket.setCustomerId(request.getCustomerId());
        ticket.setDeviceId(request.getDeviceId());
        ticket.setFaultCategoryId(request.getFaultCategoryId());
        ticket.setPriority(priority.name());
        ticket.setStatus(TicketStatus.PENDING_ASSIGN.name());
        ticket.setSource(StringUtils.hasText(request.getSource()) ? request.getSource() : "WEB");
        ticket.setReporterName(StringUtils.hasText(request.getReporterName())
                ? request.getReporterName() : customer.getContactName());
        ticket.setReporterPhone(StringUtils.hasText(request.getReporterPhone())
                ? request.getReporterPhone() : customer.getContactPhone());
        ticket.setDeadlineTime(deadline);
        ticket.setRejectCount(0);
        ticket.setReopenCount(0);
        ticket.setTransferCount(0);
        ticket.setVersion(0);
        ticket.setCreateBy(SecurityUtils.currentUserId());
        insertWithGeneratedNo(ticket);

        addRecord(ticket, TicketAction.CREATE, null, TicketStatus.PENDING_ASSIGN.name(),
                "工单创建成功，等待分派", null);
        statisticsCacheService.evictStatisticsAfterCommit();
        return toVOList(List.of(requireTicket(ticket.getId()))).get(0);
    }

    /**
     * 生成编号并插入。
     *
     * 序列号通过 ticket_sequence 表在同一行上原子自增获得，并发建单会按顺序取到不同号码，
     * 因此无需"查询最大值 + 唯一索引冲突重试"（该方式在 REPEATABLE READ 下会读到相同快照，
     * 导致重试风暴甚至死锁，已通过并发集成测试验证）。唯一索引 uk_ticket_no 仍作为兜底约束。
     */
    private void insertWithGeneratedNo(ServiceTicket ticket) {
        LocalDate today = LocalDate.now();
        ticketMapper.nextSequence(today);
        Long sequence = ticketMapper.currentSequence(today);
        if (sequence == null) {
            throw BusinessException.of(ErrorCode.CONCURRENT_CONFLICT, "工单编号生成失败，请重试");
        }
        ticket.setTicketNo(ticketNoGenerator.generate(today, sequence));
        try {
            ticketMapper.insert(ticket);
        } catch (DuplicateKeyException ex) {
            log.error("工单编号冲突（序列号表与历史数据不一致）：ticketNo={}", ticket.getTicketNo(), ex);
            throw BusinessException.of(ErrorCode.CONCURRENT_CONFLICT, "工单编号生成失败，请重试");
        }
    }

    // --------------------------------------------------------------- 流转

    /**
     * 分派：待分派 → 待接单。
     */
    @Transactional(rollbackFor = Exception.class)
    public TicketVO assign(Long id, TicketAssignRequest request) {
        accessGuard.requireAnyRole(AccessGuard.ROLE_ADMIN, AccessGuard.ROLE_SERVICE);
        ServiceTicket ticket = requireTicket(id);
        requireTransition(ticket, TicketStatus.PENDING_ASSIGN, TicketStatus.PENDING_ACCEPT);
        SysUser engineer = requireEngineer(request.getAssigneeId());

        String fromStatus = ticket.getStatus();
        conditionalUpdate(ticket, fromStatus, wrapper -> wrapper
                .set(ServiceTicket::getStatus, TicketStatus.PENDING_ACCEPT.name())
                .set(ServiceTicket::getAssigneeId, engineer.getId())
                .set(ServiceTicket::getAssignTime, LocalDateTime.now())
                .set(ServiceTicket::getUpdateBy, SecurityUtils.currentUserId()));
        closeActiveAssignment(id, null);
        insertAssignment(id, null, engineer.getId(), "ASSIGN", request.getRemark());
        addRecord(ticket, TicketAction.ASSIGN, fromStatus, TicketStatus.PENDING_ACCEPT.name(),
                StringUtils.hasText(request.getRemark())
                        ? "分派给 " + engineer.getRealName() + "：" + request.getRemark()
                        : "分派给 " + engineer.getRealName(), null);
        statisticsCacheService.evictStatisticsAfterCommit();
        return toVOList(List.of(requireTicket(id))).get(0);
    }

    /**
     * 接单：待接单 → 处理中。Redis 锁 + 条件更新双重保护，避免并发重复接单。
     */
    @Transactional(rollbackFor = Exception.class)
    public TicketVO accept(Long id) {
        ServiceTicket ticket = requireTicket(id);
        requireTransition(ticket, TicketStatus.PENDING_ACCEPT, TicketStatus.PROCESSING);
        // 先判断状态再判断负责人：待分派工单直接返回 1402，避免给出误导性的 1405
        assertAssignee(ticket);

        Long currentUserId = SecurityUtils.currentUserId();
        String lockKey = ACCEPT_LOCK_PREFIX + id;
        if (!redisLockService.tryLock(lockKey, ACCEPT_LOCK_TTL)) {
            throw BusinessException.of(ErrorCode.CONCURRENT_CONFLICT, "该工单正在被其他工程师接单，请稍后重试");
        }
        try {
            String fromStatus = ticket.getStatus();
            LocalDateTime now = LocalDateTime.now();
            int rows = conditionalUpdateQuietly(ticket, fromStatus, wrapper -> wrapper
                    .set(ServiceTicket::getStatus, TicketStatus.PROCESSING.name())
                    .set(ServiceTicket::getAcceptTime, now)
                    .set(ServiceTicket::getStartTime, now)
                    .set(ServiceTicket::getUpdateBy, currentUserId));
            if (rows == 0) {
                ServiceTicket fresh = ticketMapper.selectById(id);
                if (fresh == null) {
                    throw BusinessException.of(ErrorCode.TICKET_NOT_FOUND);
                }
                // 已被他人接单：返回 1403，便于前端给出准确提示
                if (TicketStatus.PROCESSING.name().equals(fresh.getStatus())
                        && fresh.getAssigneeId() != null && !fresh.getAssigneeId().equals(currentUserId)) {
                    throw BusinessException.of(ErrorCode.TICKET_ALREADY_ACCEPTED);
                }
                if (!TicketStatus.PENDING_ACCEPT.name().equals(fresh.getStatus())) {
                    throw BusinessException.of(ErrorCode.TICKET_STATUS_INVALID,
                            "工单状态已变更为「" + label(fresh.getStatus()) + "」，请刷新后重试");
                }
                throw BusinessException.of(ErrorCode.CONCURRENT_CONFLICT);
            }
            addRecord(ticket, TicketAction.ACCEPT, fromStatus, TicketStatus.PROCESSING.name(),
                    "工程师接单，开始处理", null);
        } finally {
            redisLockService.unlock(lockKey);
        }
        statisticsCacheService.evictStatisticsAfterCommit();
        return toVOList(List.of(requireTicket(id))).get(0);
    }

    /**
     * 转派：待接单或处理中 → 待接单，转派后需要重新接单。
     */
    @Transactional(rollbackFor = Exception.class)
    public TicketVO transfer(Long id, TicketTransferRequest request) {
        ServiceTicket ticket = requireTicket(id);
        TicketStatus current = requireTicketStatus(ticket);
        if (current != TicketStatus.PENDING_ACCEPT && current != TicketStatus.PROCESSING) {
            throw BusinessException.of(ErrorCode.TICKET_STATUS_INVALID, "当前状态不允许转派");
        }
        UserPrincipal principal = SecurityUtils.currentUser();
        boolean staff = principal.hasRole(AccessGuard.ROLE_ADMIN) || principal.hasRole(AccessGuard.ROLE_SERVICE);
        if (!staff) {
            accessGuard.requirePermission("ticket:transfer");
            assertAssignee(ticket);
        }
        SysUser engineer = requireEngineer(request.getAssigneeId());
        if (engineer.getId().equals(ticket.getAssigneeId())) {
            throw BusinessException.of(ErrorCode.PARAM_INVALID, "转派对象不能与当前负责人相同");
        }

        Long previousAssignee = ticket.getAssigneeId();
        String fromStatus = ticket.getStatus();
        conditionalUpdate(ticket, fromStatus, wrapper -> wrapper
                .set(ServiceTicket::getStatus, TicketStatus.PENDING_ACCEPT.name())
                .set(ServiceTicket::getAssigneeId, engineer.getId())
                .set(ServiceTicket::getAssignTime, LocalDateTime.now())
                .setSql("transfer_count = transfer_count + 1")
                .set(ServiceTicket::getUpdateBy, principal.getUserId()));
        closeActiveAssignment(id, null);
        insertAssignment(id, previousAssignee, engineer.getId(), "TRANSFER", request.getReason());
        addRecord(ticket, TicketAction.TRANSFER, fromStatus, TicketStatus.PENDING_ACCEPT.name(),
                "转派给 " + engineer.getRealName() + "：" + request.getReason(), null);
        statisticsCacheService.evictStatisticsAfterCommit();
        return toVOList(List.of(requireTicket(id))).get(0);
    }

    /**
     * 退回：待接单 → 待分派，由客服重新分派。
     */
    @Transactional(rollbackFor = Exception.class)
    public TicketVO reject(Long id, TicketRejectRequest request) {
        ServiceTicket ticket = requireTicket(id);
        assertAssignee(ticket);
        accessGuard.requirePermission("ticket:transfer");
        requireTransition(ticket, TicketStatus.PENDING_ACCEPT, TicketStatus.PENDING_ASSIGN);

        String fromStatus = ticket.getStatus();
        conditionalUpdate(ticket, fromStatus, wrapper -> wrapper
                .set(ServiceTicket::getStatus, TicketStatus.PENDING_ASSIGN.name())
                .set(ServiceTicket::getAssigneeId, null)
                .setSql("reject_count = reject_count + 1")
                .set(ServiceTicket::getUpdateBy, SecurityUtils.currentUserId()));
        closeActiveAssignment(id, "REJECT_TO_POOL");
        addRecord(ticket, TicketAction.REJECT, fromStatus, TicketStatus.PENDING_ASSIGN.name(),
                "退回待重新分派：" + request.getReason(), null);
        statisticsCacheService.evictStatisticsAfterCommit();
        return toVOList(List.of(requireTicket(id))).get(0);
    }

    /**
     * 填写处理记录：处理中状态下追加维修记录，不改变工单状态。
     */
    @Transactional(rollbackFor = Exception.class)
    public TicketVO process(Long id, TicketProcessRequest request) {
        ServiceTicket ticket = requireTicket(id);
        assertAssignee(ticket);
        requireTicketStatus(ticket, TicketStatus.PROCESSING);
        accessGuard.requirePermission("ticket:process");

        if (ticket.getStartTime() == null) {
            conditionalUpdate(ticket, TicketStatus.PROCESSING.name(), wrapper -> wrapper
                    .set(ServiceTicket::getStartTime, LocalDateTime.now())
                    .set(ServiceTicket::getUpdateBy, SecurityUtils.currentUserId()));
        }
        addRecord(ticket, TicketAction.PROCESS, ticket.getStatus(), TicketStatus.PROCESSING.name(),
                request.getContent(), request.getUsedHours());
        return toVOList(List.of(requireTicket(id))).get(0);
    }

    /**
     * 提交处理结果：处理中 → 待确认，要求先有处理记录。
     */
    @Transactional(rollbackFor = Exception.class)
    public TicketVO complete(Long id, TicketCompleteRequest request) {
        ServiceTicket ticket = requireTicket(id);
        assertAssignee(ticket);
        accessGuard.requirePermission("ticket:complete");
        requireTransition(ticket, TicketStatus.PROCESSING, TicketStatus.PENDING_CONFIRM);
        Long processRecords = recordMapper.selectCount(Wrappers.<TicketProcessRecord>lambdaQuery()
                .eq(TicketProcessRecord::getTicketId, id)
                .eq(TicketProcessRecord::getAction, TicketAction.PROCESS.name()));
        if (processRecords == null || processRecords == 0) {
            throw BusinessException.of(ErrorCode.PARAM_INVALID, "请先填写处理记录，再提交处理结果");
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startedAt = ticket.getAcceptTime() == null ? ticket.getStartTime() : ticket.getAcceptTime();
        BigDecimal handleHours = hoursBetween(startedAt, now);
        String fromStatus = ticket.getStatus();
        conditionalUpdate(ticket, fromStatus, wrapper -> wrapper
                .set(ServiceTicket::getStatus, TicketStatus.PENDING_CONFIRM.name())
                .set(ServiceTicket::getFinishTime, now)
                .set(ServiceTicket::getHandleHours, handleHours)
                .set(ServiceTicket::getUpdateBy, SecurityUtils.currentUserId()));
        addRecord(ticket, TicketAction.COMPLETE, fromStatus, TicketStatus.PENDING_CONFIRM.name(),
                request.getResult(), request.getUsedHours());
        statisticsCacheService.evictStatisticsAfterCommit();
        return toVOList(List.of(requireTicket(id))).get(0);
    }

    /**
     * 客户确认：待确认 → 已完成，仅工单所属客户或管理员可操作。
     */
    @Transactional(rollbackFor = Exception.class)
    public TicketVO confirm(Long id, TicketConfirmRequest request) {
        ServiceTicket ticket = requireTicket(id);
        assertCustomerOwnsOrStaff(ticket);
        accessGuard.requirePermission("ticket:confirm");
        requireTransition(ticket, TicketStatus.PENDING_CONFIRM, TicketStatus.COMPLETED);

        LocalDateTime now = LocalDateTime.now();
        String fromStatus = ticket.getStatus();
        conditionalUpdate(ticket, fromStatus, wrapper -> wrapper
                .set(ServiceTicket::getStatus, TicketStatus.COMPLETED.name())
                .set(ServiceTicket::getConfirmTime, now)
                .set(ServiceTicket::getClosedTime, now)
                .set(ServiceTicket::getUpdateBy, SecurityUtils.currentUserId()));
        addRecord(ticket, TicketAction.CONFIRM, fromStatus, TicketStatus.COMPLETED.name(),
                StringUtils.hasText(request.getRemark()) ? request.getRemark() : "客户确认服务结果", null);
        statisticsCacheService.evictStatisticsAfterCommit();
        return toVOList(List.of(requireTicket(id))).get(0);
    }

    /**
     * 退回重做（待确认 → 处理中）或重新打开（已完成 → 待分派，受重开窗口限制）。
     */
    @Transactional(rollbackFor = Exception.class)
    public TicketVO reopen(Long id, TicketReopenRequest request) {
        ServiceTicket ticket = requireTicket(id);
        assertCustomerOwnsOrStaff(ticket);
        accessGuard.requirePermission("ticket:reopen");
        TicketStatus current = requireTicketStatus(ticket);

        TicketStatus target;
        String content;
        if (current == TicketStatus.PENDING_CONFIRM) {
            target = TicketStatus.PROCESSING;
            content = "退回重做：" + request.getReason();
        } else if (current == TicketStatus.COMPLETED) {
            int windowDays = configService.getInt(CONFIG_REOPEN_WINDOW_DAYS, 7);
            if (ticket.getClosedTime() != null
                    && ticket.getClosedTime().isBefore(LocalDateTime.now().minusDays(windowDays))) {
                throw BusinessException.of(ErrorCode.PARAM_INVALID,
                        "工单已完成超过 " + windowDays + " 天，不能重新打开，请新建工单");
            }
            target = TicketStatus.PENDING_ASSIGN;
            content = "重新打开工单：" + request.getReason();
        } else {
            throw BusinessException.of(ErrorCode.TICKET_STATUS_INVALID, "当前状态不允许退回重做或重新打开");
        }

        String fromStatus = ticket.getStatus();
        int reopenCount = (ticket.getReopenCount() == null ? 0 : ticket.getReopenCount()) + 1;
        conditionalUpdate(ticket, fromStatus, wrapper -> {
            wrapper.set(ServiceTicket::getStatus, target.name())
                    .set(ServiceTicket::getReopenCount, reopenCount)
                    .set(ServiceTicket::getConfirmTime, null)
                    .set(ServiceTicket::getUpdateBy, SecurityUtils.currentUserId());
            if (target == TicketStatus.PENDING_ASSIGN) {
                wrapper.set(ServiceTicket::getAssigneeId, null);
            }
        });
        if (target == TicketStatus.PENDING_ASSIGN) {
            closeActiveAssignment(id, null);
        }
        addRecord(ticket, TicketAction.REOPEN, fromStatus, target.name(), content, null);
        statisticsCacheService.evictStatisticsAfterCommit();
        return toVOList(List.of(requireTicket(id))).get(0);
    }

    /**
     * 取消工单：非终态 → 已取消，仅管理员与客服可操作。
     */
    @Transactional(rollbackFor = Exception.class)
    public TicketVO cancel(Long id, TicketCancelRequest request) {
        accessGuard.requireAnyRole(AccessGuard.ROLE_ADMIN, AccessGuard.ROLE_SERVICE);
        ServiceTicket ticket = requireTicket(id);
        TicketStatus current = requireTicketStatus(ticket);
        if (current == TicketStatus.COMPLETED) {
            throw BusinessException.of(ErrorCode.TICKET_STATUS_INVALID, "已完成工单不能取消");
        }
        String fromStatus = ticket.getStatus();
        LocalDateTime now = LocalDateTime.now();
        conditionalUpdate(ticket, fromStatus, wrapper -> wrapper
                .set(ServiceTicket::getStatus, TicketStatus.CANCELLED.name())
                .set(ServiceTicket::getCancelTime, now)
                .set(ServiceTicket::getCancelReason, request.getReason())
                .set(ServiceTicket::getUpdateBy, SecurityUtils.currentUserId()));
        closeActiveAssignment(id, null);
        addRecord(ticket, TicketAction.CANCEL, fromStatus, TicketStatus.CANCELLED.name(),
                "取消工单：" + request.getReason(), null);
        statisticsCacheService.evictStatisticsAfterCommit();
        return toVOList(List.of(requireTicket(id))).get(0);
    }

    // ----------------------------------------------------------- 内部工具

    private int conditionalUpdate(ServiceTicket ticket, String expectedStatus,
                                  Consumer<LambdaUpdateWrapper<ServiceTicket>> mutation) {
        int rows = conditionalUpdateQuietly(ticket, expectedStatus, mutation);
        if (rows == 0) {
            ServiceTicket fresh = ticketMapper.selectById(ticket.getId());
            if (fresh == null) {
                throw BusinessException.of(ErrorCode.TICKET_NOT_FOUND);
            }
            if (!expectedStatus.equals(fresh.getStatus())) {
                throw BusinessException.of(ErrorCode.TICKET_STATUS_INVALID,
                        "工单状态已变更为「" + label(fresh.getStatus()) + "」，请刷新后重试");
            }
            throw BusinessException.of(ErrorCode.CONCURRENT_CONFLICT);
        }
        return rows;
    }

    /**
     * 只执行条件更新并返回影响行数，不抛出异常，由调用方按业务语义决定错误码。
     */
    private int conditionalUpdateQuietly(ServiceTicket ticket, String expectedStatus,
                                         Consumer<LambdaUpdateWrapper<ServiceTicket>> mutation) {
        LambdaUpdateWrapper<ServiceTicket> wrapper = Wrappers.<ServiceTicket>lambdaUpdate()
                .eq(ServiceTicket::getId, ticket.getId())
                .eq(ServiceTicket::getStatus, expectedStatus)
                .eq(ServiceTicket::getVersion, ticket.getVersion());
        mutation.accept(wrapper);
        wrapper.setSql("version = version + 1");
        return ticketMapper.update(null, wrapper);
    }

    private void requireTransition(ServiceTicket ticket, TicketStatus expected, TicketStatus target) {
        TicketStatus current = requireTicketStatus(ticket);
        if (current != expected) {
            throw BusinessException.of(ErrorCode.TICKET_STATUS_INVALID,
                    "当前状态「" + current.getLabel() + "」不允许该操作");
        }
        if (!current.canTransitionTo(target)) {
            throw BusinessException.of(ErrorCode.TICKET_STATUS_INVALID,
                    "不允许从「" + current.getLabel() + "」流转到「" + target.getLabel() + "」");
        }
    }

    private TicketStatus requireTicketStatus(ServiceTicket ticket) {
        return requireTicketStatus(ticket, null);
    }

    private TicketStatus requireTicketStatus(ServiceTicket ticket, TicketStatus expected) {
        TicketStatus current = TicketStatus.of(ticket.getStatus());
        if (current == null) {
            throw BusinessException.of(ErrorCode.TICKET_STATUS_INVALID);
        }
        if (current == TicketStatus.CANCELLED) {
            throw BusinessException.of(ErrorCode.TICKET_CANCELLED);
        }
        if (expected != null && current != expected) {
            throw BusinessException.of(ErrorCode.TICKET_STATUS_INVALID,
                    "当前状态「" + current.getLabel() + "」不允许该操作");
        }
        return current;
    }

    private ServiceTicket requireTicket(Long id) {
        ServiceTicket ticket = ticketMapper.selectById(id);
        if (ticket == null) {
            throw BusinessException.of(ErrorCode.TICKET_NOT_FOUND);
        }
        return ticket;
    }

    private SysUser requireEngineer(Long userId) {
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null || (user.getDeleted() != null && user.getDeleted() != 0L)) {
            throw BusinessException.of(ErrorCode.PARAM_INVALID, "指定的负责人不存在");
        }
        if (user.getStatus() == null || user.getStatus() != 1) {
            throw BusinessException.of(ErrorCode.PARAM_INVALID, "指定的负责人已被禁用");
        }
        boolean isEngineer = sysRoleMapper.selectByUserId(userId).stream()
                .map(SysRole::getRoleCode)
                .anyMatch(code -> AccessGuard.ROLE_ENGINEER.equals(code));
        if (!isEngineer) {
            throw BusinessException.of(ErrorCode.PARAM_INVALID, "指定的用户不是维修工程师");
        }
        return user;
    }

    private void assertAssignee(ServiceTicket ticket) {
        Long currentUserId = SecurityUtils.currentUserId();
        if (ticket.getAssigneeId() == null || !ticket.getAssigneeId().equals(currentUserId)) {
            throw BusinessException.of(ErrorCode.TICKET_ACCESS_DENIED, "该工单当前不由你负责");
        }
    }

    private void assertCustomerOwnsOrStaff(ServiceTicket ticket) {
        if (dataScopeService.isCustomer()) {
            accessGuard.assertCustomerScope(ticket.getCustomerId());
        }
    }

    private void assertVisible(ServiceTicket ticket) {
        UserPrincipal principal = SecurityUtils.currentUser();
        if (principal.hasRole(AccessGuard.ROLE_ADMIN) || principal.hasRole(AccessGuard.ROLE_SERVICE)) {
            return;
        }
        if (principal.hasRole(AccessGuard.ROLE_CUSTOMER)) {
            accessGuard.assertCustomerScope(ticket.getCustomerId());
            return;
        }
        if (principal.hasRole(AccessGuard.ROLE_ENGINEER)) {
            if (ticket.getAssigneeId() == null || !ticket.getAssigneeId().equals(principal.getUserId())) {
                throw BusinessException.of(ErrorCode.DATA_SCOPE_DENIED);
            }
            return;
        }
        throw BusinessException.of(ErrorCode.ACCESS_DENIED);
    }

    private void closeActiveAssignment(Long ticketId, String assignType) {
        List<TicketAssignment> activeList = assignmentMapper.selectList(Wrappers.<TicketAssignment>lambdaQuery()
                .eq(TicketAssignment::getTicketId, ticketId)
                .eq(TicketAssignment::getActiveFlag, 1L));
        for (TicketAssignment active : activeList) {
            assignmentMapper.update(null, Wrappers.<TicketAssignment>lambdaUpdate()
                    .eq(TicketAssignment::getId, active.getId())
                    .set(TicketAssignment::getActiveFlag, active.getId())
                    .set(StringUtils.hasText(assignType), TicketAssignment::getAssignType, assignType));
        }
    }

    private void insertAssignment(Long ticketId, Long fromUserId, Long toUserId, String assignType, String reason) {
        TicketAssignment assignment = new TicketAssignment();
        assignment.setTicketId(ticketId);
        assignment.setFromUserId(fromUserId);
        assignment.setToUserId(toUserId);
        assignment.setAssignType(assignType);
        assignment.setReason(reason);
        assignment.setActiveFlag(1L);
        assignment.setCreateBy(SecurityUtils.currentUserId());
        assignmentMapper.insert(assignment);
    }

    private void addRecord(ServiceTicket ticket, TicketAction action, String fromStatus, String toStatus,
                           String content, BigDecimal usedHours) {
        UserPrincipal principal = SecurityUtils.currentUser();
        TicketProcessRecord record = new TicketProcessRecord();
        record.setTicketId(ticket.getId());
        record.setOperatorId(principal.getUserId());
        record.setOperatorName(principal.getRealName());
        record.setOperatorRole(String.join(",", principal.getRoles()));
        record.setAction(action.name());
        record.setFromStatus(fromStatus);
        record.setToStatus(toStatus);
        record.setContent(content);
        record.setUsedHours(usedHours);
        recordMapper.insert(record);
    }

    private List<TicketRecordVO> recordsOf(Long ticketId) {
        return recordMapper.selectList(Wrappers.<TicketProcessRecord>lambdaQuery()
                        .eq(TicketProcessRecord::getTicketId, ticketId)
                        .orderByAsc(TicketProcessRecord::getCreateTime)
                        .orderByAsc(TicketProcessRecord::getId))
                .stream().map(TicketRecordVO::from).toList();
    }

    /**
     * 批量装载关联名称，避免列表页出现 N+1 查询。
     */
    private List<TicketVO> toVOList(List<ServiceTicket> tickets) {
        if (tickets.isEmpty()) {
            return List.of();
        }
        Map<Long, Customer> customers = loadMap(idsOf(tickets, ServiceTicket::getCustomerId),
                customerMapper::selectBatchIds, Customer::getId);
        Map<Long, Device> devices = loadMap(idsOf(tickets, ServiceTicket::getDeviceId),
                deviceMapper::selectBatchIds, Device::getId);
        Map<Long, SysUser> users = loadMap(idsOf(tickets, ServiceTicket::getAssigneeId),
                sysUserMapper::selectBatchIds, SysUser::getId);
        Map<Long, FaultCategory> categories = loadMap(idsOf(tickets, ServiceTicket::getFaultCategoryId),
                faultCategoryMapper::selectBatchIds, FaultCategory::getId);
        Map<Long, TicketEvaluation> evaluations = new LinkedHashMap<>();
        List<Long> ticketIds = tickets.stream().map(ServiceTicket::getId).toList();
        if (!ticketIds.isEmpty()) {
            for (TicketEvaluation evaluation : evaluationMapper.selectList(
                    Wrappers.<TicketEvaluation>lambdaQuery().in(TicketEvaluation::getTicketId, ticketIds))) {
                evaluations.put(evaluation.getTicketId(), evaluation);
            }
        }
        return tickets.stream().map(ticket -> {
            Customer customer = ticket.getCustomerId() == null ? null : customers.get(ticket.getCustomerId());
            Device device = ticket.getDeviceId() == null ? null : devices.get(ticket.getDeviceId());
            // 未分派工单的 assigneeId 为 null，必须避免对空 Map 做 get(null)（不可变空 Map 会抛 NPE）
            SysUser user = ticket.getAssigneeId() == null ? null : users.get(ticket.getAssigneeId());
            FaultCategory category = ticket.getFaultCategoryId() == null
                    ? null : categories.get(ticket.getFaultCategoryId());
            TicketVO vo = TicketVO.from(ticket,
                    customer == null ? null : customer.getCustomerName(),
                    device == null ? null : device.getDeviceName(),
                    device == null ? null : device.getDeviceCode(),
                    category == null ? null : category.getCategoryName(),
                    user == null ? null : user.getRealName());
            TicketEvaluation evaluation = evaluations.get(ticket.getId());
            if (evaluation != null) {
                vo.setEvaluationScore(evaluation.getScore());
                vo.setEvaluationSatisfaction(evaluation.getSatisfaction());
            }
            return vo;
        }).toList();
    }

    private List<Long> idsOf(List<ServiceTicket> tickets, Function<ServiceTicket, Long> getter) {
        return tickets.stream().map(getter).filter(Objects::nonNull).distinct().toList();
    }

    private <T> Map<Long, T> loadMap(List<Long> ids, Function<List<Long>, List<T>> loader,
                                     Function<T, Long> idGetter) {
        if (ids.isEmpty()) {
            // 使用允许 null 键查询的空 Map，避免调用方 get(null) 时抛 NPE
            return Collections.emptyMap();
        }
        Map<Long, T> map = new LinkedHashMap<>();
        loader.apply(ids).forEach(item -> map.put(idGetter.apply(item), item));
        return map;
    }

    private String label(String status) {
        TicketStatus value = TicketStatus.of(status);
        return value == null ? status : value.getLabel();
    }

    private BigDecimal hoursBetween(LocalDateTime from, LocalDateTime to) {
        if (from == null) {
            return null;
        }
        long minutes = Duration.between(from, to).toMinutes();
        return BigDecimal.valueOf(minutes).divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
    }
}

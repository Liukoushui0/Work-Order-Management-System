package com.serviceflow.service;

import com.serviceflow.common.enums.ErrorCode;
import com.serviceflow.common.enums.TicketStatus;
import com.serviceflow.common.exception.BusinessException;
import com.serviceflow.common.util.TicketNoGenerator;
import com.serviceflow.dto.ticket.TicketAssignRequest;
import com.serviceflow.dto.ticket.TicketCancelRequest;
import com.serviceflow.dto.ticket.TicketCompleteRequest;
import com.serviceflow.dto.ticket.TicketCreateRequest;
import com.serviceflow.dto.ticket.TicketRejectRequest;
import com.serviceflow.dto.ticket.TicketReopenRequest;
import com.serviceflow.entity.Customer;
import com.serviceflow.entity.Device;
import com.serviceflow.entity.ServiceTicket;
import com.serviceflow.entity.SysRole;
import com.serviceflow.entity.SysUser;
import com.serviceflow.mapper.CustomerMapper;
import com.serviceflow.mapper.DeviceMapper;
import com.serviceflow.mapper.FaultCategoryMapper;
import com.serviceflow.mapper.ServiceTicketMapper;
import com.serviceflow.mapper.SysRoleMapper;
import com.serviceflow.mapper.SysUserMapper;
import com.serviceflow.mapper.TicketAssignmentMapper;
import com.serviceflow.mapper.TicketProcessRecordMapper;
import com.serviceflow.security.AccessGuard;
import com.serviceflow.security.DataScopeService;
import com.serviceflow.security.UserPrincipal;
import com.serviceflow.support.MybatisTestSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * 工单服务单元测试：聚焦状态机、权限与业务前置校验（不依赖数据库）。
 */
@ExtendWith(MockitoExtension.class)
class TicketServiceTest {

    @Mock private ServiceTicketMapper ticketMapper;
    @Mock private TicketAssignmentMapper assignmentMapper;
    @Mock private TicketProcessRecordMapper recordMapper;
    @Mock private CustomerMapper customerMapper;
    @Mock private DeviceMapper deviceMapper;
    @Mock private SysUserMapper sysUserMapper;
    @Mock private SysRoleMapper sysRoleMapper;
    @Mock private FaultCategoryMapper faultCategoryMapper;
    @Mock private ConfigService configService;
    @Mock private RedisLockService redisLockService;
    @Mock private com.serviceflow.mapper.TicketEvaluationMapper evaluationMapper;
    @Mock private com.serviceflow.mapper.TicketAttachmentMapper attachmentMapper;
    @Mock private com.serviceflow.mapper.TicketCommentMapper commentMapper;
    @Mock private StatisticsCacheService statisticsCacheService;

    private TicketService ticketService;

    @BeforeAll
    static void initMybatisMetadata() {
        MybatisTestSupport.initTableInfo(ServiceTicket.class, Customer.class, Device.class,
                SysUser.class, SysRole.class, com.serviceflow.entity.TicketAssignment.class,
                com.serviceflow.entity.TicketProcessRecord.class);
    }

    @BeforeEach
    void setUp() {
        ticketService = new TicketService(ticketMapper, assignmentMapper, recordMapper, customerMapper,
                deviceMapper, sysUserMapper, sysRoleMapper, faultCategoryMapper, new DataScopeService(),
                new AccessGuard(), configService, redisLockService, new TicketNoGenerator(),
                evaluationMapper, attachmentMapper, commentMapper, statisticsCacheService);
        loginAs(3L, "engineer01", AccessGuard.ROLE_ENGINEER, Set.of("ticket:view", "ticket:accept",
                "ticket:process", "ticket:complete", "ticket:transfer"));
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    private void loginAs(Long userId, String username, String roleCode, Set<String> permissions) {
        UserPrincipal principal = new UserPrincipal(userId, username, "hash", username, null, true,
                Set.of(roleCode), permissions);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }

    private void loginAsService() {
        loginAs(2L, "service01", AccessGuard.ROLE_SERVICE, Set.of("ticket:create", "ticket:assign",
                "ticket:cancel", "ticket:view"));
    }

    private ServiceTicket ticket(Long id, TicketStatus status, Long assigneeId, Integer version) {
        ServiceTicket entity = new ServiceTicket();
        entity.setId(id);
        entity.setTicketNo("SO20261015000001");
        entity.setTitle("测试工单");
        entity.setCustomerId(1L);
        entity.setDeviceId(1L);
        entity.setStatus(status.name());
        entity.setAssigneeId(assigneeId);
        entity.setVersion(version);
        entity.setPriority("MEDIUM");
        entity.setRejectCount(0);
        entity.setReopenCount(0);
        entity.setTransferCount(0);
        entity.setDeadlineTime(LocalDateTime.now().plusHours(8));
        return entity;
    }

    private static ErrorCode errorCodeOf(Throwable throwable) {
        return ((BusinessException) throwable).getErrorCode();
    }

    private TicketCreateRequest createRequest() {
        TicketCreateRequest request = new TicketCreateRequest();
        request.setTitle("新工单");
        request.setDescription("故障描述");
        request.setCustomerId(1L);
        request.setDeviceId(1L);
        request.setPriority("HIGH");
        return request;
    }

    private Customer customer(Long id, Integer status) {
        Customer entity = new Customer();
        entity.setId(id);
        entity.setCustomerName("测试客户");
        entity.setContactName("联系人");
        entity.setContactPhone("13900000000");
        entity.setStatus(status);
        entity.setDeleted(0L);
        return entity;
    }

    private Device device(Long id, Long customerId) {
        Device entity = new Device();
        entity.setId(id);
        entity.setDeviceCode("DEV-0001");
        entity.setDeviceName("测试设备");
        entity.setCustomerId(customerId);
        entity.setStatus(1);
        entity.setDeleted(0L);
        return entity;
    }

    private SysUser engineer(Long id) {
        SysUser user = new SysUser();
        user.setId(id);
        user.setUsername("engineer0" + id);
        user.setRealName("工程师" + id);
        user.setStatus(1);
        user.setDeleted(0L);
        return user;
    }

    private SysRole engineerRole() {
        SysRole role = new SysRole();
        role.setId(3L);
        role.setRoleCode(AccessGuard.ROLE_ENGINEER);
        role.setDeleted(0L);
        return role;
    }

    @Test
    @DisplayName("创建的设备不属于所选客户时拒绝建单")
    void shouldRejectDeviceNotBelongingToCustomer() {
        loginAsService();
        given(customerMapper.selectById(1L)).willReturn(customer(1L, 1));
        given(deviceMapper.selectById(1L)).willReturn(device(1L, 2L));

        assertThatThrownBy(() -> ticketService.create(createRequest()))
                .isInstanceOf(BusinessException.class)
                .extracting(TicketServiceTest::errorCodeOf)
                .isEqualTo(ErrorCode.DEVICE_CUSTOMER_MISMATCH);

        verify(ticketMapper, never()).insert(any(ServiceTicket.class));
    }

    @Test
    @DisplayName("客户已停用时拒绝建单")
    void shouldRejectCreateForDisabledCustomer() {
        loginAsService();
        given(customerMapper.selectById(1L)).willReturn(customer(1L, 0));

        assertThatThrownBy(() -> ticketService.create(createRequest()))
                .isInstanceOf(BusinessException.class)
                .extracting(TicketServiceTest::errorCodeOf)
                .isEqualTo(ErrorCode.PARAM_INVALID);
    }

    @Test
    @DisplayName("截止时间早于当前时间时拒绝建单")
    void shouldRejectDeadlineInPast() {
        loginAsService();
        given(customerMapper.selectById(1L)).willReturn(customer(1L, 1));
        given(deviceMapper.selectById(1L)).willReturn(device(1L, 1L));
        TicketCreateRequest request = createRequest();
        request.setDeadlineTime(LocalDateTime.now().minusHours(1));

        assertThatThrownBy(() -> ticketService.create(request))
                .isInstanceOf(BusinessException.class)
                .extracting(TicketServiceTest::errorCodeOf)
                .isEqualTo(ErrorCode.PARAM_INVALID);
    }

    @Test
    @DisplayName("非待分派状态不能再次分派")
    void shouldRejectAssignOnWrongStatus() {
        loginAsService();
        given(ticketMapper.selectById(1L)).willReturn(ticket(1L, TicketStatus.PROCESSING, 3L, 1));
        TicketAssignRequest request = new TicketAssignRequest();
        request.setAssigneeId(3L);

        assertThatThrownBy(() -> ticketService.assign(1L, request))
                .isInstanceOf(BusinessException.class)
                .extracting(TicketServiceTest::errorCodeOf)
                .isEqualTo(ErrorCode.TICKET_STATUS_INVALID);
    }

    @Test
    @DisplayName("分派对象不是工程师时拒绝")
    void shouldRejectAssignTargetNotEngineer() {
        loginAsService();
        given(ticketMapper.selectById(1L)).willReturn(ticket(1L, TicketStatus.PENDING_ASSIGN, null, 0));
        given(sysUserMapper.selectById(9L)).willReturn(engineer(9L));
        given(sysRoleMapper.selectByUserId(9L)).willReturn(List.of());
        TicketAssignRequest request = new TicketAssignRequest();
        request.setAssigneeId(9L);

        assertThatThrownBy(() -> ticketService.assign(1L, request))
                .isInstanceOf(BusinessException.class)
                .extracting(TicketServiceTest::errorCodeOf)
                .isEqualTo(ErrorCode.PARAM_INVALID);
    }

    @Test
    @DisplayName("非负责人接单被拒绝（1405）")
    void shouldRejectAcceptByOtherEngineer() {
        given(ticketMapper.selectById(1L)).willReturn(ticket(1L, TicketStatus.PENDING_ACCEPT, 99L, 0));

        assertThatThrownBy(() -> ticketService.accept(1L))
                .isInstanceOf(BusinessException.class)
                .extracting(TicketServiceTest::errorCodeOf)
                .isEqualTo(ErrorCode.TICKET_ACCESS_DENIED);

        verify(redisLockService, never()).tryLock(anyString(), any());
    }

    @Test
    @DisplayName("并发接单：条件更新影响 0 行时返回 1602 或 1403，绝不静默成功")
    void shouldReportConflictOnConcurrentAccept() {
        ServiceTicket current = ticket(1L, TicketStatus.PENDING_ACCEPT, 3L, 5);
        given(ticketMapper.selectById(1L)).willReturn(current);
        given(redisLockService.tryLock(anyString(), any())).willReturn(true);
        given(ticketMapper.update(any(), any())).willReturn(0);
        ServiceTicket fresh = ticket(1L, TicketStatus.PROCESSING, 99L, 6);
        given(ticketMapper.selectById(1L)).willReturn(current, fresh);

        assertThatThrownBy(() -> ticketService.accept(1L))
                .isInstanceOf(BusinessException.class)
                .extracting(TicketServiceTest::errorCodeOf)
                .isEqualTo(ErrorCode.TICKET_ALREADY_ACCEPTED);
    }

    @Test
    @DisplayName("取消已完工单被拒绝")
    void shouldRejectCancelOnCompletedTicket() {
        loginAsService();
        given(ticketMapper.selectById(1L)).willReturn(ticket(1L, TicketStatus.COMPLETED, 3L, 3));
        TicketCancelRequest request = new TicketCancelRequest();
        request.setReason("客户要求取消");

        assertThatThrownBy(() -> ticketService.cancel(1L, request))
                .isInstanceOf(BusinessException.class)
                .extracting(TicketServiceTest::errorCodeOf)
                .isEqualTo(ErrorCode.TICKET_STATUS_INVALID);
    }

    @Test
    @DisplayName("没有处理记录时不允许提交处理结果")
    void shouldRequireProcessRecordBeforeComplete() {
        given(ticketMapper.selectById(1L)).willReturn(ticket(1L, TicketStatus.PROCESSING, 3L, 2));
        given(recordMapper.selectCount(any())).willReturn(0L);
        TicketCompleteRequest request = new TicketCompleteRequest();
        request.setResult("已修复");

        assertThatThrownBy(() -> ticketService.complete(1L, request))
                .isInstanceOf(BusinessException.class)
                .extracting(TicketServiceTest::errorCodeOf)
                .isEqualTo(ErrorCode.PARAM_INVALID);
    }

    @Test
    @DisplayName("超过重开窗口的完工单不能重新打开")
    void shouldRejectReopenBeyondWindow() {
        loginAs(1L, "admin", AccessGuard.ROLE_ADMIN, Set.of("ticket:reopen", "ticket:view"));
        ServiceTicket closed = ticket(1L, TicketStatus.COMPLETED, 3L, 4);
        closed.setClosedTime(LocalDateTime.now().minusDays(30));
        given(ticketMapper.selectById(1L)).willReturn(closed);
        given(configService.getInt("ticket.reopen_window_days", 7)).willReturn(7);
        TicketReopenRequest request = new TicketReopenRequest();
        request.setReason("问题复发");

        assertThatThrownBy(() -> ticketService.reopen(1L, request))
                .isInstanceOf(BusinessException.class)
                .extracting(TicketServiceTest::errorCodeOf)
                .isEqualTo(ErrorCode.PARAM_INVALID);
    }

    @Test
    @DisplayName("处于待分派的工单不能被工程师退回")
    void shouldRejectRejectOnWrongStatus() {
        given(ticketMapper.selectById(1L)).willReturn(ticket(1L, TicketStatus.PENDING_ASSIGN, null, 0));
        TicketRejectRequest request = new TicketRejectRequest();
        request.setReason("设备型号不匹配");

        assertThatThrownBy(() -> ticketService.reject(1L, request))
                .isInstanceOf(BusinessException.class)
                .extracting(TicketServiceTest::errorCodeOf)
                .isEqualTo(ErrorCode.TICKET_ACCESS_DENIED);
    }
}

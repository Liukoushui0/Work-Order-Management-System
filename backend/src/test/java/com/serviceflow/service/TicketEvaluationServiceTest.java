package com.serviceflow.service;

import com.serviceflow.common.enums.ErrorCode;
import com.serviceflow.common.enums.TicketStatus;
import com.serviceflow.common.exception.BusinessException;
import com.serviceflow.dto.feedback.EvaluationCreateRequest;
import com.serviceflow.entity.ServiceTicket;
import com.serviceflow.entity.TicketEvaluation;
import com.serviceflow.mapper.TicketEvaluationMapper;
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
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TicketEvaluationServiceTest {

    @Mock private TicketEvaluationMapper evaluationMapper;
    @Mock private TicketService ticketService;

    private TicketEvaluationService evaluationService;

    @BeforeAll
    static void initMybatisMetadata() {
        MybatisTestSupport.initTableInfo(TicketEvaluation.class, ServiceTicket.class);
    }

    @BeforeEach
    void setUp() {
        evaluationService = new TicketEvaluationService(evaluationMapper, ticketService,
                new AccessGuard(), new DataScopeService());
        loginAsCustomer(1L);
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    private void loginAsCustomer(Long customerId) {
        UserPrincipal principal = new UserPrincipal(5L, "customer01", "hash", "客户联系人一", customerId, true,
                Set.of(AccessGuard.ROLE_CUSTOMER), Set.of("ticket:view", "ticket:evaluate"));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }

    private ServiceTicket ticket(TicketStatus status, Long customerId) {
        ServiceTicket entity = new ServiceTicket();
        entity.setId(1L);
        entity.setTicketNo("SO20261015000001");
        entity.setStatus(status.name());
        entity.setCustomerId(customerId);
        return entity;
    }

    private EvaluationCreateRequest request() {
        EvaluationCreateRequest request = new EvaluationCreateRequest();
        request.setScore(5);
        return request;
    }

    private static ErrorCode errorCodeOf(Throwable throwable) {
        return ((BusinessException) throwable).getErrorCode();
    }

    @Test
    @DisplayName("未完成的工单不能评价（1501）")
    void shouldRejectEvaluationOnUnfinishedTicket() {
        given(ticketService.requireVisibleTicket(1L)).willReturn(ticket(TicketStatus.PROCESSING, 1L));

        assertThatThrownBy(() -> evaluationService.create(1L, request()))
                .isInstanceOf(BusinessException.class)
                .extracting(TicketEvaluationServiceTest::errorCodeOf)
                .isEqualTo(ErrorCode.TICKET_NOT_COMPLETED);

        verify(evaluationMapper, never()).insert(any(TicketEvaluation.class));
    }

    @Test
    @DisplayName("重复评价被拒绝（1502）")
    void shouldRejectDuplicateEvaluation() {
        given(ticketService.requireVisibleTicket(1L)).willReturn(ticket(TicketStatus.COMPLETED, 1L));
        given(evaluationMapper.selectCount(any())).willReturn(1L);

        assertThatThrownBy(() -> evaluationService.create(1L, request()))
                .isInstanceOf(BusinessException.class)
                .extracting(TicketEvaluationServiceTest::errorCodeOf)
                .isEqualTo(ErrorCode.TICKET_ALREADY_EVALUATED);
    }

    @Test
    @DisplayName("非本人客户不能评价他人工单（1102）")
    void shouldRejectEvaluationForOtherCustomer() {
        given(ticketService.requireVisibleTicket(1L)).willReturn(ticket(TicketStatus.COMPLETED, 999L));

        assertThatThrownBy(() -> evaluationService.create(1L, request()))
                .isInstanceOf(BusinessException.class)
                .extracting(TicketEvaluationServiceTest::errorCodeOf)
                .isEqualTo(ErrorCode.DATA_SCOPE_DENIED);
    }

    @Test
    @DisplayName("评分自动映射满意度，匿名标记正确写入")
    void shouldMapSatisfactionAndAnonymousFlag() {
        given(ticketService.requireVisibleTicket(1L)).willReturn(ticket(TicketStatus.COMPLETED, 1L));
        given(evaluationMapper.selectCount(any())).willReturn(0L);
        EvaluationCreateRequest request = request();
        request.setAnonymous(true);
        request.setContent("服务很好");

        evaluationService.create(1L, request);

        ArgumentCaptor<TicketEvaluation> captor = ArgumentCaptor.forClass(TicketEvaluation.class);
        verify(evaluationMapper).insert(captor.capture());
        assertThat(captor.getValue().getSatisfaction()).isEqualTo("VERY_SATISFIED");
        assertThat(captor.getValue().getIsAnonymous()).isEqualTo(1);
        assertThat(captor.getValue().getCustomerId()).isEqualTo(1L);
        assertThat(captor.getValue().getEvaluatorId()).isEqualTo(5L);
        verify(ticketService).appendRecord(any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("3 分映射为中性满意度")
    void shouldMapNeutralSatisfaction() {
        given(ticketService.requireVisibleTicket(1L)).willReturn(ticket(TicketStatus.COMPLETED, 1L));
        given(evaluationMapper.selectCount(any())).willReturn(0L);
        EvaluationCreateRequest request = new EvaluationCreateRequest();
        request.setScore(3);

        evaluationService.create(1L, request);

        ArgumentCaptor<TicketEvaluation> captor = ArgumentCaptor.forClass(TicketEvaluation.class);
        verify(evaluationMapper).insert(captor.capture());
        assertThat(captor.getValue().getSatisfaction()).isEqualTo("NEUTRAL");
    }
}

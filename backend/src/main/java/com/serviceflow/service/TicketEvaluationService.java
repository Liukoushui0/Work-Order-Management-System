package com.serviceflow.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.serviceflow.common.enums.ErrorCode;
import com.serviceflow.common.enums.TicketAction;
import com.serviceflow.common.enums.TicketStatus;
import com.serviceflow.common.exception.BusinessException;
import com.serviceflow.dto.feedback.EvaluationCreateRequest;
import com.serviceflow.entity.ServiceTicket;
import com.serviceflow.entity.TicketEvaluation;
import com.serviceflow.mapper.TicketEvaluationMapper;
import com.serviceflow.security.AccessGuard;
import com.serviceflow.security.DataScopeService;
import com.serviceflow.security.SecurityUtils;
import com.serviceflow.security.UserPrincipal;
import com.serviceflow.vo.EvaluationVO;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Map;

/**
 * 客户服务评价：仅"已完成且未评价"的工单可评价，一个工单只允许一条评价。
 */
@Service
public class TicketEvaluationService {

    /** 评分 → 默认满意度映射。 */
    private static final Map<Integer, String> SCORE_TO_SATISFACTION = Map.of(
            5, "VERY_SATISFIED",
            4, "SATISFIED",
            3, "NEUTRAL",
            2, "UNSATISFIED",
            1, "VERY_UNSATISFIED");

    private final TicketEvaluationMapper evaluationMapper;
    private final TicketService ticketService;
    private final AccessGuard accessGuard;
    private final DataScopeService dataScopeService;

    public TicketEvaluationService(TicketEvaluationMapper evaluationMapper,
                                   TicketService ticketService,
                                   AccessGuard accessGuard,
                                   DataScopeService dataScopeService) {
        this.evaluationMapper = evaluationMapper;
        this.ticketService = ticketService;
        this.accessGuard = accessGuard;
        this.dataScopeService = dataScopeService;
    }

    /**
     * 提交评价。
     */
    @Transactional(rollbackFor = Exception.class)
    public EvaluationVO create(Long ticketId, EvaluationCreateRequest request) {
        ServiceTicket ticket = ticketService.requireVisibleTicket(ticketId);
        accessGuard.requirePermission("ticket:evaluate");
        UserPrincipal principal = SecurityUtils.currentUser();
        boolean staff = principal.hasRole(AccessGuard.ROLE_ADMIN) || principal.hasRole(AccessGuard.ROLE_SERVICE);
        if (!staff && !dataScopeService.isCustomer()) {
            throw BusinessException.of(ErrorCode.ACCESS_DENIED, "仅客户可提交服务评价");
        }
        if (dataScopeService.isCustomer()) {
            accessGuard.assertCustomerScope(ticket.getCustomerId());
        }
        if (!TicketStatus.COMPLETED.name().equals(ticket.getStatus())) {
            throw BusinessException.of(ErrorCode.TICKET_NOT_COMPLETED);
        }
        Long existing = evaluationMapper.selectCount(Wrappers.<TicketEvaluation>lambdaQuery()
                .eq(TicketEvaluation::getTicketId, ticketId));
        if (existing != null && existing > 0) {
            throw BusinessException.of(ErrorCode.TICKET_ALREADY_EVALUATED);
        }

        TicketEvaluation evaluation = new TicketEvaluation();
        evaluation.setTicketId(ticketId);
        evaluation.setCustomerId(ticket.getCustomerId());
        evaluation.setEvaluatorId(principal.getUserId());
        evaluation.setScore(request.getScore());
        evaluation.setSatisfaction(StringUtils.hasText(request.getSatisfaction())
                ? request.getSatisfaction()
                : SCORE_TO_SATISFACTION.getOrDefault(request.getScore(), "SATISFIED"));
        evaluation.setSpeedScore(request.getSpeedScore());
        evaluation.setQualityScore(request.getQualityScore());
        evaluation.setAttitudeScore(request.getAttitudeScore());
        evaluation.setTags(request.getTags());
        evaluation.setContent(request.getContent());
        evaluation.setIsAnonymous(Boolean.TRUE.equals(request.getAnonymous()) ? 1 : 0);
        try {
            evaluationMapper.insert(evaluation);
        } catch (DuplicateKeyException ex) {
            // 并发重复提交由唯一索引 uk_evaluation_ticket 兜底
            throw BusinessException.of(ErrorCode.TICKET_ALREADY_EVALUATED);
        }
        ticketService.appendRecord(ticketId, TicketAction.EVALUATE, ticket.getStatus(), ticket.getStatus(),
                "客户提交服务评价：" + evaluation.getSatisfaction() + "（" + evaluation.getScore() + " 分）");
        return EvaluationVO.from(evaluation, true);
    }

    /**
     * 查询工单评价，未评价返回 null。
     */
    @Transactional(readOnly = true)
    public EvaluationVO detail(Long ticketId) {
        ticketService.requireVisibleTicket(ticketId);
        TicketEvaluation evaluation = evaluationMapper.selectOne(Wrappers.<TicketEvaluation>lambdaQuery()
                .eq(TicketEvaluation::getTicketId, ticketId));
        if (evaluation == null) {
            return null;
        }
        boolean viewerIsEvaluator = evaluation.getEvaluatorId().equals(SecurityUtils.currentUserId());
        return EvaluationVO.from(evaluation, viewerIsEvaluator);
    }
}

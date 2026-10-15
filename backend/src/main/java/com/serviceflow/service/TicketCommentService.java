package com.serviceflow.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.serviceflow.common.enums.ErrorCode;
import com.serviceflow.common.exception.BusinessException;
import com.serviceflow.dto.feedback.CommentCreateRequest;
import com.serviceflow.entity.ServiceTicket;
import com.serviceflow.entity.SysUser;
import com.serviceflow.entity.TicketComment;
import com.serviceflow.mapper.SysUserMapper;
import com.serviceflow.mapper.TicketCommentMapper;
import com.serviceflow.security.AccessGuard;
import com.serviceflow.security.SecurityUtils;
import com.serviceflow.security.UserPrincipal;
import com.serviceflow.vo.CommentVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 工单评论：员工可写内部备注，客户只能看到公开评论且只能写公开评论。
 */
@Service
public class TicketCommentService {

    private final TicketCommentMapper commentMapper;
    private final SysUserMapper sysUserMapper;
    private final TicketService ticketService;
    private final AccessGuard accessGuard;

    public TicketCommentService(TicketCommentMapper commentMapper,
                                SysUserMapper sysUserMapper,
                                TicketService ticketService,
                                AccessGuard accessGuard) {
        this.commentMapper = commentMapper;
        this.sysUserMapper = sysUserMapper;
        this.ticketService = ticketService;
        this.accessGuard = accessGuard;
    }

    /**
     * 评论列表。客户角色只能看到公开评论，内部备注仅员工可见。
     */
    @Transactional(readOnly = true)
    public List<CommentVO> list(Long ticketId) {
        ticketService.requireVisibleTicket(ticketId);
        boolean staff = isStaff();
        List<TicketComment> comments = commentMapper.selectList(Wrappers.<TicketComment>lambdaQuery()
                .eq(TicketComment::getTicketId, ticketId)
                .eq(TicketComment::getDeleted, 0L)
                .eq(!staff, TicketComment::getIsInternal, 0)
                .orderByAsc(TicketComment::getCreateTime)
                .orderByAsc(TicketComment::getId));
        Map<Long, String> names = namesOf(comments);
        return comments.stream().map(comment -> CommentVO.from(comment, names.get(comment.getUserId()))).toList();
    }

    /**
     * 新增评论。
     */
    @Transactional(rollbackFor = Exception.class)
    public CommentVO create(Long ticketId, CommentCreateRequest request) {
        ServiceTicket ticket = ticketService.requireVisibleTicket(ticketId);
        UserPrincipal principal = SecurityUtils.currentUser();
        boolean staff = isStaff();
        boolean internal = staff && Boolean.TRUE.equals(request.getInternal());

        TicketComment comment = new TicketComment();
        comment.setTicketId(ticketId);
        comment.setUserId(principal.getUserId());
        comment.setUserType(staff ? "STAFF" : "CUSTOMER");
        comment.setContent(request.getContent());
        comment.setIsInternal(internal ? 1 : 0);
        comment.setDeleted(0L);
        commentMapper.insert(comment);
        // 客户评论同样可见于工单，但不写入状态流转记录，保持处理历史的纯净
        if (!staff) {
            accessGuard.assertCustomerScope(ticket.getCustomerId());
        }
        return CommentVO.from(comment, principal.getRealName());
    }

    private boolean isStaff() {
        UserPrincipal principal = SecurityUtils.currentUser();
        return principal.hasRole(AccessGuard.ROLE_ADMIN) || principal.hasRole(AccessGuard.ROLE_SERVICE)
                || principal.hasRole(AccessGuard.ROLE_ENGINEER);
    }

    private Map<Long, String> namesOf(List<TicketComment> comments) {
        List<Long> userIds = comments.stream().map(TicketComment::getUserId).filter(Objects::nonNull)
                .distinct().toList();
        if (userIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, String> names = new LinkedHashMap<>();
        for (SysUser user : sysUserMapper.selectBatchIds(userIds)) {
            names.put(user.getId(), user.getRealName());
        }
        return names;
    }
}

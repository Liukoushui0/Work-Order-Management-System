package com.serviceflow.vo;

import com.serviceflow.entity.TicketComment;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Schema(description = "工单评论")
public class CommentVO {

    private Long id;
    private Long ticketId;
    private Long userId;
    private String operatorName;
    private String userType;
    private String content;
    private Boolean internal;
    private LocalDateTime createTime;

    public static CommentVO from(TicketComment comment, String operatorName) {
        CommentVO vo = new CommentVO();
        vo.setId(comment.getId());
        vo.setTicketId(comment.getTicketId());
        vo.setUserId(comment.getUserId());
        vo.setOperatorName(operatorName);
        vo.setUserType(comment.getUserType());
        vo.setContent(comment.getContent());
        vo.setInternal(comment.getIsInternal() != null && comment.getIsInternal() == 1);
        vo.setCreateTime(comment.getCreateTime());
        return vo;
    }
}

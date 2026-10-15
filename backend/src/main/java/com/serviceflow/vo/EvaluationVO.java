package com.serviceflow.vo;

import com.serviceflow.entity.TicketEvaluation;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Schema(description = "服务评价")
public class EvaluationVO {

    private Long id;
    private Long ticketId;
    private Long customerId;
    private Integer score;
    private String satisfaction;
    private Integer speedScore;
    private Integer qualityScore;
    private Integer attitudeScore;
    private String tags;
    private String content;
    private Boolean anonymous;
    private LocalDateTime createTime;

    /**
     * 转换为响应对象。匿名评价对非评价人隐藏内容与标签。
     */
    public static EvaluationVO from(TicketEvaluation evaluation, boolean viewerIsEvaluator) {
        EvaluationVO vo = new EvaluationVO();
        boolean anonymous = evaluation.getIsAnonymous() != null && evaluation.getIsAnonymous() == 1;
        vo.setId(evaluation.getId());
        vo.setTicketId(evaluation.getTicketId());
        vo.setCustomerId(anonymous && !viewerIsEvaluator ? null : evaluation.getCustomerId());
        vo.setScore(evaluation.getScore());
        vo.setSatisfaction(evaluation.getSatisfaction());
        vo.setSpeedScore(evaluation.getSpeedScore());
        vo.setQualityScore(evaluation.getQualityScore());
        vo.setAttitudeScore(evaluation.getAttitudeScore());
        vo.setAnonymous(anonymous);
        vo.setTags(anonymous && !viewerIsEvaluator ? null : evaluation.getTags());
        vo.setContent(anonymous && !viewerIsEvaluator ? null : evaluation.getContent());
        vo.setCreateTime(evaluation.getCreateTime());
        return vo;
    }
}

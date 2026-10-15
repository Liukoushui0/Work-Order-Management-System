package com.serviceflow.mapper;

import com.serviceflow.mapper.view.CountByOwner;
import com.serviceflow.mapper.view.DateCount;
import com.serviceflow.mapper.view.EngineerWorkloadRow;
import com.serviceflow.mapper.view.LabelCount;
import com.serviceflow.mapper.view.SatisfactionRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 运营统计查询。
 *
 * 所有语句都支持按负责人（assigneeId）收敛数据域：管理员与客服传 null 表示全局，
 * 工程师传自身用户 ID 表示仅本人。统计查询统一落在 service_ticket / ticket_evaluation 上。
 */
@Mapper
public interface StatisticsMapper {

    @Select("""
            <script>
            SELECT status AS label, COUNT(*) AS total FROM service_ticket
            WHERE 1 = 1
            <if test="assigneeId != null"> AND assignee_id = #{assigneeId}</if>
            GROUP BY status
            </script>
            """)
    List<LabelCount> countByStatus(@Param("assigneeId") Long assigneeId);

    @Select("""
            <script>
            SELECT priority AS label, COUNT(*) AS total FROM service_ticket
            WHERE 1 = 1
            <if test="assigneeId != null"> AND assignee_id = #{assigneeId}</if>
            GROUP BY priority
            </script>
            """)
    List<LabelCount> countByPriority(@Param("assigneeId") Long assigneeId);

    @Select("""
            <script>
            SELECT COUNT(*) FROM service_ticket
            WHERE status NOT IN ('COMPLETED', 'CANCELLED')
              AND deadline_time IS NOT NULL AND deadline_time &lt; #{now}
            <if test="assigneeId != null"> AND assignee_id = #{assigneeId}</if>
            </script>
            """)
    long countOverdue(@Param("assigneeId") Long assigneeId, @Param("now") LocalDateTime now);

    @Select("""
            <script>
            SELECT AVG(handle_hours) FROM service_ticket
            WHERE handle_hours IS NOT NULL
            <if test="assigneeId != null"> AND assignee_id = #{assigneeId}</if>
            </script>
            """)
    Double avgHandleHours(@Param("assigneeId") Long assigneeId);

    @Select("""
            <script>
            SELECT DATE(create_time) AS stat_date, COUNT(*) AS total FROM service_ticket
            WHERE create_time BETWEEN #{start} AND #{end}
            <if test="assigneeId != null"> AND assignee_id = #{assigneeId}</if>
            GROUP BY DATE(create_time) ORDER BY stat_date
            </script>
            """)
    List<DateCount> countCreatedByDate(@Param("start") LocalDateTime start,
                                       @Param("end") LocalDateTime end,
                                       @Param("assigneeId") Long assigneeId);

    @Select("""
            <script>
            SELECT DATE(closed_time) AS stat_date, COUNT(*) AS total FROM service_ticket
            WHERE closed_time BETWEEN #{start} AND #{end}
            <if test="assigneeId != null"> AND assignee_id = #{assigneeId}</if>
            GROUP BY DATE(closed_time) ORDER BY stat_date
            </script>
            """)
    List<DateCount> countClosedByDate(@Param("start") LocalDateTime start,
                                      @Param("end") LocalDateTime end,
                                      @Param("assigneeId") Long assigneeId);

    @Select("""
            <script>
            SELECT fault_category_id AS ownerId, COUNT(*) AS total FROM service_ticket
            WHERE fault_category_id IS NOT NULL
            <if test="assigneeId != null"> AND assignee_id = #{assigneeId}</if>
            GROUP BY fault_category_id ORDER BY total DESC
            </script>
            """)
    List<CountByOwner> countByFaultCategory(@Param("assigneeId") Long assigneeId);

    @Select("""
            <script>
            SELECT assignee_id AS ownerId, COUNT(*) AS total,
                   SUM(CASE WHEN status = 'COMPLETED' THEN 1 ELSE 0 END) AS completed,
                   AVG(handle_hours) AS avgHours
            FROM service_ticket
            WHERE assignee_id IS NOT NULL
            <if test="assigneeId != null"> AND assignee_id = #{assigneeId}</if>
            GROUP BY assignee_id ORDER BY total DESC
            </script>
            """)
    List<EngineerWorkloadRow> engineerWorkload(@Param("assigneeId") Long assigneeId);

    @Select("""
            <script>
            SELECT COUNT(*) AS total, AVG(e.score) AS avgScore,
                   SUM(CASE WHEN e.score = 5 THEN 1 ELSE 0 END) AS fiveStar,
                   SUM(CASE WHEN e.score = 4 THEN 1 ELSE 0 END) AS fourStar,
                   SUM(CASE WHEN e.score = 3 THEN 1 ELSE 0 END) AS threeStar,
                   SUM(CASE WHEN e.score &lt; 3 THEN 1 ELSE 0 END) AS lowStar
            FROM ticket_evaluation e
            INNER JOIN service_ticket t ON t.id = e.ticket_id
            WHERE 1 = 1
            <if test="assigneeId != null"> AND t.assignee_id = #{assigneeId}</if>
            </script>
            """)
    SatisfactionRow satisfaction(@Param("assigneeId") Long assigneeId);

    @Select("""
            <script>
            SELECT COUNT(*) FROM service_ticket
            WHERE 1 = 1
            <if test="assigneeId != null"> AND assignee_id = #{assigneeId}</if>
            </script>
            """)
    long countTotal(@Param("assigneeId") Long assigneeId);
}

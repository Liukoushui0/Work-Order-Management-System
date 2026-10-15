package com.serviceflow.mapper;

import com.serviceflow.mapper.view.CountByOwner;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 工单相关的只读统计查询。
 *
 * <p>工单模块（Stage 4）尚未落地时，客户/设备详情需要展示关联工单数量，
 * 因此这里只放"只读聚合"语句；工单写入逻辑一律放在工单模块中。</p>
 */
@Mapper
public interface TicketStatMapper {

    @Select("""
            <script>
            SELECT customer_id AS ownerId, COUNT(*) AS total FROM service_ticket
            WHERE customer_id IN
            <foreach collection="customerIds" item="id" open="(" separator="," close=")">#{id}</foreach>
            GROUP BY customer_id
            </script>
            """)
    List<CountByOwner> countTicketsByCustomerIds(@Param("customerIds") List<Long> customerIds);

    @Select("""
            SELECT COUNT(*) FROM service_ticket
            WHERE customer_id = #{customerId} AND status NOT IN ('COMPLETED', 'CANCELLED')
            """)
    long countUnfinishedTicketsByCustomerId(@Param("customerId") Long customerId);

    @Select("""
            SELECT COUNT(*) FROM service_ticket
            WHERE device_id = #{deviceId} AND status NOT IN ('COMPLETED', 'CANCELLED')
            """)
    long countUnfinishedTicketsByDeviceId(@Param("deviceId") Long deviceId);
}

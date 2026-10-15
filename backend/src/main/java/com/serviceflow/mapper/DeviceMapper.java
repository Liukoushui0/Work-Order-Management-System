package com.serviceflow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.serviceflow.entity.Device;
import com.serviceflow.mapper.view.CountByOwner;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface DeviceMapper extends BaseMapper<Device> {

    /**
     * 批量统计设备下的工单数量（跨模块只读聚合，工单模块在后续阶段复用）。
     */
    @Select("""
            <script>
            SELECT device_id AS ownerId, COUNT(*) AS total FROM service_ticket
            WHERE device_id IN
            <foreach collection="deviceIds" item="id" open="(" separator="," close=")">#{id}</foreach>
            GROUP BY device_id
            </script>
            """)
    List<CountByOwner> countTicketsByDeviceIds(@Param("deviceIds") List<Long> deviceIds);
}

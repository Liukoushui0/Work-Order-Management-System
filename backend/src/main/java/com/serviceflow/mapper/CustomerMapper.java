package com.serviceflow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.serviceflow.entity.Customer;
import com.serviceflow.mapper.view.CountByOwner;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface CustomerMapper extends BaseMapper<Customer> {

    /**
     * 批量统计客户下的设备数量，用于客户列表与详情展示（避免 N+1）。
     */
    @Select("""
            <script>
            SELECT customer_id AS ownerId, COUNT(*) AS total FROM device
            WHERE deleted = 0 AND customer_id IN
            <foreach collection="customerIds" item="id" open="(" separator="," close=")">#{id}</foreach>
            GROUP BY customer_id
            </script>
            """)
    List<CountByOwner> countDevicesByCustomerIds(@Param("customerIds") List<Long> customerIds);
}

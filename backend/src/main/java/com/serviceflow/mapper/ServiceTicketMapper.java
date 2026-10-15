package com.serviceflow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.serviceflow.entity.ServiceTicket;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;

@Mapper
public interface ServiceTicketMapper extends BaseMapper<ServiceTicket> {

    /**
     * 查询指定前缀（SO + yyyyMMdd）下已有的最大当日序列号，用于生成工单编号。
     */
    @Select("SELECT COALESCE(MAX(CAST(SUBSTRING(ticket_no, 11) AS UNSIGNED)), 0) FROM service_ticket "
            + "WHERE ticket_no LIKE CONCAT(#{prefix}, '%')")
    long selectMaxSequence(@Param("prefix") String prefix);

    /**
     * 原子取号：当天首单插入初始行，之后在同一行上自增。
     * 行锁会持续到事务提交，因此并发建单按顺序取得不同序列号，无需重试。
     */
    @Insert("INSERT INTO ticket_sequence (seq_date, current_value) VALUES (#{seqDate}, 1) "
            + "ON DUPLICATE KEY UPDATE current_value = current_value + 1")
    int nextSequence(@Param("seqDate") LocalDate seqDate);

    /**
     * 读取当前已分配的当日序列号（本事务写入的值一定可见）。
     */
    @Select("SELECT current_value FROM ticket_sequence WHERE seq_date = #{seqDate}")
    Long currentSequence(@Param("seqDate") LocalDate seqDate);
}

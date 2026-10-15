package com.serviceflow.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.serviceflow.common.api.PageResult;
import com.serviceflow.common.enums.ErrorCode;
import com.serviceflow.common.exception.BusinessException;
import com.serviceflow.dto.system.OperationLogQuery;
import com.serviceflow.entity.SysOperationLog;
import com.serviceflow.mapper.SysOperationLogMapper;
import com.serviceflow.vo.OperationLogVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 操作日志查询与写入。
 */
@Service
public class OperationLogService {

    private final SysOperationLogMapper sysOperationLogMapper;

    public OperationLogService(SysOperationLogMapper sysOperationLogMapper) {
        this.sysOperationLogMapper = sysOperationLogMapper;
    }

    /**
     * 写入审计日志：使用独立事务，保证业务回滚时日志仍然保留。
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void save(SysOperationLog log) {
        sysOperationLogMapper.insert(log);
    }

    /**
     * 分页查询操作日志。
     */
    @Transactional(readOnly = true)
    public PageResult<OperationLogVO> page(OperationLogQuery query) {
        if (!query.isSortFieldAllowed()) {
            throw BusinessException.of(ErrorCode.SORT_FIELD_INVALID);
        }
        Page<SysOperationLog> page = query.toPage("createTime");
        LocalDateTime start = query.getStartDate() == null ? null : query.getStartDate().atStartOfDay();
        LocalDateTime end = query.getEndDate() == null ? null : query.getEndDate().atTime(LocalTime.MAX);
        Page<SysOperationLog> result = sysOperationLogMapper.selectPage(page,
                Wrappers.<SysOperationLog>lambdaQuery()
                        .eq(StringUtils.hasText(query.getModule()), SysOperationLog::getModule, query.getModule())
                        .eq(StringUtils.hasText(query.getUsername()), SysOperationLog::getUsername, query.getUsername())
                        .eq(query.getSuccess() != null, SysOperationLog::getSuccess, query.getSuccess())
                        .ge(start != null, SysOperationLog::getCreateTime, start)
                        .le(end != null, SysOperationLog::getCreateTime, end));
        return PageResult.of(result, OperationLogVO::from);
    }
}

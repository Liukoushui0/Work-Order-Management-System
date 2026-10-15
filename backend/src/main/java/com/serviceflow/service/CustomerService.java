package com.serviceflow.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.serviceflow.common.api.PageResult;
import com.serviceflow.common.enums.ErrorCode;
import com.serviceflow.common.exception.BusinessException;
import com.serviceflow.dto.customer.CustomerCreateRequest;
import com.serviceflow.dto.customer.CustomerQuery;
import com.serviceflow.dto.customer.CustomerUpdateRequest;
import com.serviceflow.entity.Customer;
import com.serviceflow.entity.Device;
import com.serviceflow.mapper.CustomerMapper;
import com.serviceflow.mapper.DeviceMapper;
import com.serviceflow.mapper.TicketStatMapper;
import com.serviceflow.mapper.view.CountByOwner;
import com.serviceflow.security.AccessGuard;
import com.serviceflow.security.DataScopeService;
import com.serviceflow.security.SecurityUtils;
import com.serviceflow.vo.CustomerVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 客户资料管理。
 *
 * <p>数据域：客户角色只能看到自己的客户档案；管理员与客服可维护全部客户。</p>
 */
@Service
public class CustomerService {

    private final CustomerMapper customerMapper;
    private final DeviceMapper deviceMapper;
    private final TicketStatMapper ticketStatMapper;
    private final DataScopeService dataScopeService;
    private final AccessGuard accessGuard;

    public CustomerService(CustomerMapper customerMapper,
                           DeviceMapper deviceMapper,
                           TicketStatMapper ticketStatMapper,
                           DataScopeService dataScopeService,
                           AccessGuard accessGuard) {
        this.customerMapper = customerMapper;
        this.deviceMapper = deviceMapper;
        this.ticketStatMapper = ticketStatMapper;
        this.dataScopeService = dataScopeService;
        this.accessGuard = accessGuard;
    }

    @Transactional(readOnly = true)
    public PageResult<CustomerVO> page(CustomerQuery query) {
        if (!query.isSortFieldAllowed()) {
            throw BusinessException.of(ErrorCode.SORT_FIELD_INVALID);
        }
        Long scopedCustomerId = dataScopeService.scopedCustomerId();
        if (scopedCustomerId == null && !dataScopeService.canViewAll()) {
            // 既不是客户也不是可查看全部的角色（理论上不会发生），返回空列表而不是全量数据
            return PageResult.empty(query.getPageNum(), query.getPageSize());
        }
        Page<Customer> page = query.toPage("createTime");
        Page<Customer> result = customerMapper.selectPage(page,
                Wrappers.<Customer>lambdaQuery()
                        .eq(Customer::getDeleted, 0L)
                        .eq(scopedCustomerId != null, Customer::getId, scopedCustomerId)
                        .eq(query.getStatus() != null, Customer::getStatus, query.getStatus())
                        .eq(StringUtils.hasText(query.getLevel()), Customer::getLevel, query.getLevel())
                        .and(StringUtils.hasText(query.getKeyword()), wrapper -> wrapper
                                .like(Customer::getCustomerCode, query.getKeyword())
                                .or().like(Customer::getCustomerName, query.getKeyword())
                                .or().like(Customer::getContactName, query.getKeyword())
                                .or().like(Customer::getContactPhone, query.getKeyword())));
        List<Long> customerIds = result.getRecords().stream().map(Customer::getId).toList();
        Map<Long, Long> deviceCounts = countToMap(customerMapper.countDevicesByCustomerIds(customerIds));
        Map<Long, Long> ticketCounts = countToMap(ticketStatMapper.countTicketsByCustomerIds(customerIds));
        return PageResult.of(result, customer -> CustomerVO.from(customer,
                deviceCounts.get(customer.getId()), ticketCounts.get(customer.getId())));
    }

    @Transactional(readOnly = true)
    public CustomerVO detail(Long id) {
        Customer customer = requireCustomer(id);
        accessGuard.assertCustomerScope(customer.getId());
        Long deviceCount = countToMap(customerMapper.countDevicesByCustomerIds(List.of(id))).get(id);
        Long ticketCount = countToMap(ticketStatMapper.countTicketsByCustomerIds(List.of(id))).get(id);
        return CustomerVO.from(customer, deviceCount, ticketCount);
    }

    /**
     * 客户下拉选项（供设备建档、工单创建使用），客户角色只能拿到自己的档案。
     */
    @Transactional(readOnly = true)
    public List<CustomerVO> options(String keyword) {
        Long scopedCustomerId = dataScopeService.scopedCustomerId();
        return customerMapper.selectList(Wrappers.<Customer>lambdaQuery()
                        .eq(Customer::getDeleted, 0L)
                        .eq(Customer::getStatus, 1)
                        .eq(scopedCustomerId != null, Customer::getId, scopedCustomerId)
                        .like(StringUtils.hasText(keyword), Customer::getCustomerName, keyword)
                        .orderByAsc(Customer::getCustomerName)
                        .last("LIMIT 50"))
                .stream()
                .map(customer -> CustomerVO.from(customer, null, null))
                .toList();
    }

    @Transactional(rollbackFor = Exception.class)
    public CustomerVO create(CustomerCreateRequest request) {
        Long duplicate = customerMapper.selectCount(Wrappers.<Customer>lambdaQuery()
                .eq(Customer::getCustomerCode, request.getCustomerCode())
                .eq(Customer::getDeleted, 0L));
        if (duplicate != null && duplicate > 0) {
            throw BusinessException.of(ErrorCode.PARAM_INVALID, "客户编号已存在：" + request.getCustomerCode());
        }
        Customer entity = new Customer();
        entity.setCustomerCode(request.getCustomerCode());
        entity.setCustomerName(request.getCustomerName());
        entity.setContactName(request.getContactName());
        entity.setContactPhone(request.getContactPhone());
        entity.setContactEmail(request.getContactEmail());
        entity.setProvince(request.getProvince());
        entity.setCity(request.getCity());
        entity.setAddress(request.getAddress());
        entity.setIndustry(request.getIndustry());
        entity.setLevel(StringUtils.hasText(request.getLevel()) ? request.getLevel() : "B");
        entity.setSource(request.getSource());
        entity.setRemark(request.getRemark());
        entity.setStatus(1);
        entity.setDeleted(0L);
        entity.setCreateBy(SecurityUtils.currentUserId());
        customerMapper.insert(entity);
        return CustomerVO.from(entity, 0L, 0L);
    }

    @Transactional(rollbackFor = Exception.class)
    public CustomerVO update(Long id, CustomerUpdateRequest request) {
        Customer entity = requireCustomer(id);
        entity.setCustomerName(request.getCustomerName());
        entity.setContactName(request.getContactName());
        entity.setContactPhone(request.getContactPhone());
        entity.setContactEmail(request.getContactEmail());
        entity.setProvince(request.getProvince());
        entity.setCity(request.getCity());
        entity.setAddress(request.getAddress());
        entity.setIndustry(request.getIndustry());
        if (StringUtils.hasText(request.getLevel())) {
            entity.setLevel(request.getLevel());
        }
        entity.setSource(request.getSource());
        entity.setRemark(request.getRemark());
        entity.setUpdateBy(SecurityUtils.currentUserId());
        customerMapper.updateById(entity);
        return detail(id);
    }

    /**
     * 启用/停用客户。停用前校验是否存在未完成工单，避免影响在途服务。
     */
    @Transactional(rollbackFor = Exception.class)
    public CustomerVO updateStatus(Long id, Integer status) {
        Customer entity = requireCustomer(id);
        if (status == 0) {
            long unfinished = ticketStatMapper.countUnfinishedTicketsByCustomerId(id);
            if (unfinished > 0) {
                throw BusinessException.of(ErrorCode.PARAM_INVALID,
                        "该客户存在 " + unfinished + " 个未完成工单，无法停用");
            }
        }
        customerMapper.update(null, Wrappers.<Customer>lambdaUpdate()
                .eq(Customer::getId, id)
                .set(Customer::getStatus, status)
                .set(Customer::getUpdateBy, SecurityUtils.currentUserId()));
        entity.setStatus(status);
        return detail(id);
    }

    /**
     * 删除客户（逻辑删除）。名下仍有设备时拒绝，保证设备档案不会变成孤立数据。
     */
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        Customer entity = requireCustomer(id);
        Long deviceCount = deviceMapper.selectCount(Wrappers.<Device>lambdaQuery()
                .eq(Device::getCustomerId, id)
                .eq(Device::getDeleted, 0L));
        if (deviceCount != null && deviceCount > 0) {
            throw BusinessException.of(ErrorCode.PARAM_INVALID,
                    "该客户名下存在 " + deviceCount + " 台设备，请先处理设备档案");
        }
        long unfinished = ticketStatMapper.countUnfinishedTicketsByCustomerId(id);
        if (unfinished > 0) {
            throw BusinessException.of(ErrorCode.PARAM_INVALID,
                    "该客户存在 " + unfinished + " 个未完成工单，无法删除");
        }
        customerMapper.update(null, Wrappers.<Customer>lambdaUpdate()
                .eq(Customer::getId, id)
                .set(Customer::getDeleted, entity.getId())
                .set(Customer::getUpdateBy, SecurityUtils.currentUserId()));
    }

    /**
     * 校验客户存在且处于可用状态，供设备模块共用。
     */
    @Transactional(readOnly = true)
    public Customer requireEnabledCustomer(Long customerId) {
        Customer customer = requireCustomer(customerId);
        if (customer.getStatus() == null || customer.getStatus() != 1) {
            throw BusinessException.of(ErrorCode.PARAM_INVALID, "客户已停用，无法关联设备或创建工单");
        }
        return customer;
    }

    private Customer requireCustomer(Long id) {
        Customer customer = customerMapper.selectOne(Wrappers.<Customer>lambdaQuery()
                .eq(Customer::getId, id)
                .eq(Customer::getDeleted, 0L));
        if (customer == null) {
            throw BusinessException.of(ErrorCode.CUSTOMER_NOT_FOUND);
        }
        return customer;
    }

    static Map<Long, Long> countToMap(List<CountByOwner> counts) {
        if (CollectionUtils.isEmpty(counts)) {
            return Collections.emptyMap();
        }
        Map<Long, Long> map = new LinkedHashMap<>();
        counts.forEach(item -> map.put(item.getOwnerId(), item.getTotal()));
        return map;
    }
}

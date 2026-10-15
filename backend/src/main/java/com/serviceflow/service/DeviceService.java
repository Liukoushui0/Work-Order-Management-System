package com.serviceflow.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.serviceflow.common.api.PageResult;
import com.serviceflow.common.enums.ErrorCode;
import com.serviceflow.common.exception.BusinessException;
import com.serviceflow.dto.device.DeviceCreateRequest;
import com.serviceflow.dto.device.DeviceQuery;
import com.serviceflow.dto.device.DeviceUpdateRequest;
import com.serviceflow.entity.Customer;
import com.serviceflow.entity.Device;
import com.serviceflow.mapper.CustomerMapper;
import com.serviceflow.mapper.DeviceMapper;
import com.serviceflow.mapper.view.CountByOwner;
import com.serviceflow.security.AccessGuard;
import com.serviceflow.security.DataScopeService;
import com.serviceflow.security.SecurityUtils;
import com.serviceflow.vo.DeviceVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 客户设备档案管理。
 *
 * <p>数据域：客户角色只能看到自己名下的设备；工程师可读全部设备（用于处理工单），但无维护权限（由接口权限控制）。</p>
 */
@Service
public class DeviceService {

    private final DeviceMapper deviceMapper;
    private final CustomerMapper customerMapper;
    private final CustomerService customerService;
    private final DataScopeService dataScopeService;
    private final AccessGuard accessGuard;

    public DeviceService(DeviceMapper deviceMapper,
                         CustomerMapper customerMapper,
                         CustomerService customerService,
                         DataScopeService dataScopeService,
                         AccessGuard accessGuard) {
        this.deviceMapper = deviceMapper;
        this.customerMapper = customerMapper;
        this.customerService = customerService;
        this.dataScopeService = dataScopeService;
        this.accessGuard = accessGuard;
    }

    @Transactional(readOnly = true)
    public PageResult<DeviceVO> page(DeviceQuery query) {
        if (!query.isSortFieldAllowed()) {
            throw BusinessException.of(ErrorCode.SORT_FIELD_INVALID);
        }
        Long scopedCustomerId = dataScopeService.scopedCustomerId();
        Long effectiveCustomerId = scopedCustomerId != null ? scopedCustomerId : query.getCustomerId();
        Page<Device> page = query.toPage("createTime");
        Page<Device> result = deviceMapper.selectPage(page,
                Wrappers.<Device>lambdaQuery()
                        .eq(Device::getDeleted, 0L)
                        .eq(effectiveCustomerId != null, Device::getCustomerId, effectiveCustomerId)
                        .eq(query.getStatus() != null, Device::getStatus, query.getStatus())
                        .and(StringUtils.hasText(query.getKeyword()), wrapper -> wrapper
                                .like(Device::getDeviceCode, query.getKeyword())
                                .or().like(Device::getDeviceName, query.getKeyword())
                                .or().like(Device::getModel, query.getKeyword())
                                .or().like(Device::getSnNo, query.getKeyword())));
        Map<Long, String> customerNames = loadCustomerNames(result.getRecords());
        Map<Long, Long> ticketCounts = countToMap(deviceMapper.countTicketsByDeviceIds(
                result.getRecords().stream().map(Device::getId).toList()));
        return PageResult.of(result, device -> DeviceVO.from(device,
                customerNames.get(device.getCustomerId()), ticketCounts.get(device.getId())));
    }

    @Transactional(readOnly = true)
    public DeviceVO detail(Long id) {
        Device device = requireDevice(id);
        accessGuard.assertCustomerScope(device.getCustomerId());
        Customer customer = customerMapper.selectById(device.getCustomerId());
        Long ticketCount = countToMap(deviceMapper.countTicketsByDeviceIds(List.of(id))).get(id);
        return DeviceVO.from(device, customer == null ? null : customer.getCustomerName(), ticketCount);
    }

    @Transactional(rollbackFor = Exception.class)
    public DeviceVO create(DeviceCreateRequest request) {
        if (dataScopeService.isCustomer()) {
            throw BusinessException.of(ErrorCode.ACCESS_DENIED, "客户角色不能新建设备档案");
        }
        Customer customer = customerService.requireEnabledCustomer(request.getCustomerId());
        checkCodeDuplicate(request.getDeviceCode(), null);
        checkSnDuplicate(request.getSnNo(), null);
        Device entity = new Device();
        entity.setDeviceCode(request.getDeviceCode());
        entity.setDeviceName(request.getDeviceName());
        entity.setDeviceType(request.getDeviceType());
        entity.setBrand(request.getBrand());
        entity.setModel(request.getModel());
        entity.setSnNo(StringUtils.hasText(request.getSnNo()) ? request.getSnNo() : null);
        entity.setCustomerId(customer.getId());
        entity.setPurchaseDate(request.getPurchaseDate());
        entity.setWarrantyExpireDate(request.getWarrantyExpireDate());
        entity.setInstallAddress(request.getInstallAddress());
        entity.setStatus(request.getStatus() == null ? 1 : request.getStatus());
        entity.setRemark(request.getRemark());
        entity.setDeleted(0L);
        entity.setCreateBy(SecurityUtils.currentUserId());
        deviceMapper.insert(entity);
        return DeviceVO.from(entity, customer.getCustomerName(), 0L);
    }

    /**
     * 修改设备。变更归属时需要校验目标客户可用，且客户角色不能把设备转移给他人。
     */
    @Transactional(rollbackFor = Exception.class)
    public DeviceVO update(Long id, DeviceUpdateRequest request) {
        Device entity = requireDevice(id);
        if (dataScopeService.isCustomer()) {
            accessGuard.assertCustomerScope(entity.getCustomerId());
            if (!entity.getCustomerId().equals(request.getCustomerId())) {
                throw BusinessException.of(ErrorCode.ACCESS_DENIED, "客户角色不能变更设备归属");
            }
        }
        Customer customer = customerService.requireEnabledCustomer(request.getCustomerId());
        checkSnDuplicate(request.getSnNo(), id);
        entity.setDeviceName(request.getDeviceName());
        entity.setDeviceType(request.getDeviceType());
        entity.setBrand(request.getBrand());
        entity.setModel(request.getModel());
        entity.setSnNo(StringUtils.hasText(request.getSnNo()) ? request.getSnNo() : null);
        entity.setCustomerId(customer.getId());
        entity.setPurchaseDate(request.getPurchaseDate());
        entity.setWarrantyExpireDate(request.getWarrantyExpireDate());
        entity.setInstallAddress(request.getInstallAddress());
        if (request.getStatus() != null) {
            entity.setStatus(request.getStatus());
        }
        entity.setRemark(request.getRemark());
        entity.setUpdateBy(SecurityUtils.currentUserId());
        deviceMapper.updateById(entity);
        Long ticketCount = countToMap(deviceMapper.countTicketsByDeviceIds(List.of(id))).get(id);
        return DeviceVO.from(entity, customer.getCustomerName(), ticketCount);
    }

    /**
     * 删除设备（逻辑删除）。存在未完成工单时拒绝，避免工单指向失效设备。
     */
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        Device entity = requireDevice(id);
        accessGuard.assertCustomerScope(entity.getCustomerId());
        Long ticketCount = countToMap(deviceMapper.countTicketsByDeviceIds(List.of(id))).get(id);
        if (ticketCount != null && ticketCount > 0) {
            throw BusinessException.of(ErrorCode.PARAM_INVALID,
                    "该设备存在 " + ticketCount + " 条工单记录，无法删除");
        }
        deviceMapper.update(null, Wrappers.<Device>lambdaUpdate()
                .eq(Device::getId, id)
                .set(Device::getDeleted, entity.getId())
                .set(Device::getUpdateBy, SecurityUtils.currentUserId()));
    }

    /**
     * 查询客户名下启用状态的设备（供工单创建设备下拉使用）。
     */
    @Transactional(readOnly = true)
    public List<DeviceVO> optionsByCustomer(Long customerId) {
        accessGuard.assertCustomerScope(customerId);
        return deviceMapper.selectList(Wrappers.<Device>lambdaQuery()
                        .eq(Device::getDeleted, 0L)
                        .eq(Device::getCustomerId, customerId)
                        .eq(Device::getStatus, 1)
                        .orderByAsc(Device::getDeviceCode))
                .stream()
                .map(device -> DeviceVO.from(device, null, null))
                .toList();
    }

    private Device requireDevice(Long id) {
        Device device = deviceMapper.selectOne(Wrappers.<Device>lambdaQuery()
                .eq(Device::getId, id)
                .eq(Device::getDeleted, 0L));
        if (device == null) {
            throw BusinessException.of(ErrorCode.DEVICE_NOT_FOUND);
        }
        return device;
    }

    private void checkCodeDuplicate(String deviceCode, Long excludeId) {
        Long duplicate = deviceMapper.selectCount(Wrappers.<Device>lambdaQuery()
                .eq(Device::getDeviceCode, deviceCode)
                .eq(Device::getDeleted, 0L)
                .ne(excludeId != null, Device::getId, excludeId));
        if (duplicate != null && duplicate > 0) {
            throw BusinessException.of(ErrorCode.PARAM_INVALID, "设备编号已存在：" + deviceCode);
        }
    }

    private void checkSnDuplicate(String snNo, Long excludeId) {
        if (!StringUtils.hasText(snNo)) {
            return;
        }
        Long duplicate = deviceMapper.selectCount(Wrappers.<Device>lambdaQuery()
                .eq(Device::getSnNo, snNo)
                .eq(Device::getDeleted, 0L)
                .ne(excludeId != null, Device::getId, excludeId));
        if (duplicate != null && duplicate > 0) {
            throw BusinessException.of(ErrorCode.PARAM_INVALID, "设备序列号已存在：" + snNo);
        }
    }

    private Map<Long, String> loadCustomerNames(List<Device> devices) {
        if (devices.isEmpty()) {
            return Collections.emptyMap();
        }
        List<Long> customerIds = devices.stream().map(Device::getCustomerId).distinct().toList();
        Map<Long, String> names = new LinkedHashMap<>();
        for (Customer customer : customerMapper.selectBatchIds(customerIds)) {
            names.put(customer.getId(), customer.getCustomerName());
        }
        return names;
    }

    static Map<Long, Long> countToMap(List<CountByOwner> counts) {
        return CustomerService.countToMap(counts);
    }
}

package com.serviceflow.service;

import com.serviceflow.common.enums.ErrorCode;
import com.serviceflow.common.exception.BusinessException;
import com.serviceflow.dto.device.DeviceCreateRequest;
import com.serviceflow.dto.device.DeviceUpdateRequest;
import com.serviceflow.entity.Customer;
import com.serviceflow.entity.Device;
import com.serviceflow.mapper.CustomerMapper;
import com.serviceflow.mapper.DeviceMapper;
import com.serviceflow.security.AccessGuard;
import com.serviceflow.security.DataScopeService;
import com.serviceflow.security.UserPrincipal;
import com.serviceflow.support.MybatisTestSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class DeviceServiceTest {

    @Mock
    private DeviceMapper deviceMapper;

    @Mock
    private CustomerMapper customerMapper;

    @Mock
    private CustomerService customerService;

    private DeviceService deviceService;

    @BeforeAll
    static void initMybatisMetadata() {
        MybatisTestSupport.initTableInfo(Device.class, Customer.class);
    }

    @BeforeEach
    void setUp() {
        deviceService = new DeviceService(deviceMapper, customerMapper, customerService,
                new DataScopeService(), new AccessGuard());
        loginAs("ROLE_SERVICE", null);
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    private void loginAs(String roleCode, Long customerId) {
        UserPrincipal principal = new UserPrincipal(2L, "tester", "hash", "测试用户", customerId, true,
                Set.of(roleCode), Set.of("device:view", "device:manage"));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }

    private Device device(Long id, Long customerId) {
        Device entity = new Device();
        entity.setId(id);
        entity.setDeviceCode("DEV-0001");
        entity.setDeviceName("测试设备");
        entity.setCustomerId(customerId);
        entity.setStatus(1);
        entity.setDeleted(0L);
        return entity;
    }

    private Customer customer(Long id) {
        Customer entity = new Customer();
        entity.setId(id);
        entity.setCustomerName("测试客户");
        entity.setStatus(1);
        entity.setDeleted(0L);
        return entity;
    }

    private static ErrorCode errorCodeOf(Throwable throwable) {
        return ((BusinessException) throwable).getErrorCode();
    }

    private DeviceCreateRequest createRequest() {
        DeviceCreateRequest request = new DeviceCreateRequest();
        request.setDeviceCode("DEV-9001");
        request.setDeviceName("新设备");
        request.setCustomerId(1L);
        request.setSnNo("SN9001");
        return request;
    }

    @Test
    @DisplayName("新建设备时设备编号重复被拒绝")
    void shouldRejectDuplicateDeviceCode() {
        given(customerService.requireEnabledCustomer(1L)).willReturn(customer(1L));
        given(deviceMapper.selectCount(any())).willReturn(1L);

        assertThatThrownBy(() -> deviceService.create(createRequest()))
                .isInstanceOf(BusinessException.class)
                .extracting(DeviceServiceTest::errorCodeOf)
                .isEqualTo(ErrorCode.PARAM_INVALID);

        verify(deviceMapper, never()).insert(any(Device.class));
    }

    @Test
    @DisplayName("新建设备成功时写入客户归属与审计字段")
    void shouldCreateDeviceWithCustomer() {
        given(customerService.requireEnabledCustomer(1L)).willReturn(customer(1L));
        given(deviceMapper.selectCount(any())).willReturn(0L);

        deviceService.create(createRequest());

        ArgumentCaptor<Device> captor = ArgumentCaptor.forClass(Device.class);
        verify(deviceMapper).insert(captor.capture());
        assertThat(captor.getValue().getCustomerId()).isEqualTo(1L);
        assertThat(captor.getValue().getStatus()).isEqualTo(1);
        assertThat(captor.getValue().getCreateBy()).isEqualTo(2L);
    }

    @Test
    @DisplayName("客户角色不能新建设备档案")
    void customerRoleCannotCreateDevice() {
        loginAs("ROLE_CUSTOMER", 1L);

        assertThatThrownBy(() -> deviceService.create(createRequest()))
                .isInstanceOf(BusinessException.class)
                .extracting(DeviceServiceTest::errorCodeOf)
                .isEqualTo(ErrorCode.ACCESS_DENIED);
    }

    @Test
    @DisplayName("客户角色读取他人设备返回 1102")
    void customerRoleCannotReadOtherDevice() {
        loginAs("ROLE_CUSTOMER", 100L);
        given(deviceMapper.selectOne(any())).willReturn(device(5L, 200L));

        assertThatThrownBy(() -> deviceService.detail(5L))
                .isInstanceOf(BusinessException.class)
                .extracting(DeviceServiceTest::errorCodeOf)
                .isEqualTo(ErrorCode.DATA_SCOPE_DENIED);
    }

    @Test
    @DisplayName("客户角色不能把设备转移到其他客户名下")
    void customerRoleCannotTransferDeviceOwnership() {
        loginAs("ROLE_CUSTOMER", 1L);
        given(deviceMapper.selectOne(any())).willReturn(device(5L, 1L));
        DeviceUpdateRequest request = new DeviceUpdateRequest();
        request.setDeviceName("改名");
        request.setCustomerId(2L);

        assertThatThrownBy(() -> deviceService.update(5L, request))
                .isInstanceOf(BusinessException.class)
                .extracting(DeviceServiceTest::errorCodeOf)
                .isEqualTo(ErrorCode.ACCESS_DENIED);
    }

    @Test
    @DisplayName("设备不存在时返回 1302")
    void shouldReturnNotFoundWhenDeviceMissing() {
        given(deviceMapper.selectOne(any())).willReturn(null);

        assertThatThrownBy(() -> deviceService.detail(404L))
                .isInstanceOf(BusinessException.class)
                .extracting(DeviceServiceTest::errorCodeOf)
                .isEqualTo(ErrorCode.DEVICE_NOT_FOUND);
    }

    @Test
    @DisplayName("设备存在工单记录时不允许删除")
    void shouldRejectDeleteWhenTicketsExist() {
        given(deviceMapper.selectOne(any())).willReturn(device(5L, 1L));
        given(deviceMapper.countTicketsByDeviceIds(any())).willReturn(java.util.List.of(countRow(5L, 2L)));

        assertThatThrownBy(() -> deviceService.delete(5L))
                .isInstanceOf(BusinessException.class)
                .extracting(DeviceServiceTest::errorCodeOf)
                .isEqualTo(ErrorCode.PARAM_INVALID);
    }

    private com.serviceflow.mapper.view.CountByOwner countRow(Long ownerId, Long total) {
        com.serviceflow.mapper.view.CountByOwner row = new com.serviceflow.mapper.view.CountByOwner();
        row.setOwnerId(ownerId);
        row.setTotal(total);
        return row;
    }
}

package com.serviceflow.service;

import com.serviceflow.common.enums.ErrorCode;
import com.serviceflow.common.exception.BusinessException;
import com.serviceflow.dto.customer.CustomerCreateRequest;
import com.serviceflow.entity.Customer;
import com.serviceflow.mapper.CustomerMapper;
import com.serviceflow.mapper.DeviceMapper;
import com.serviceflow.mapper.TicketStatMapper;
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
class CustomerServiceTest {

    @Mock
    private CustomerMapper customerMapper;

    @Mock
    private DeviceMapper deviceMapper;

    @Mock
    private TicketStatMapper ticketStatMapper;

    private CustomerService customerService;

    @BeforeAll
    static void initMybatisMetadata() {
        MybatisTestSupport.initTableInfo(Customer.class, com.serviceflow.entity.Device.class);
    }

    @BeforeEach
    void setUp() {
        DataScopeService dataScopeService = new DataScopeService();
        customerService = new CustomerService(customerMapper, deviceMapper, ticketStatMapper,
                dataScopeService, new AccessGuard());
        loginAs("ROLE_SERVICE", null);
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    private void loginAs(String roleCode, Long customerId) {
        UserPrincipal principal = new UserPrincipal(2L, "tester", "hash", "测试用户", customerId, true,
                Set.of(roleCode), Set.of("customer:view", "customer:manage"));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }

    private Customer customer(Long id, String code, Integer status) {
        Customer entity = new Customer();
        entity.setId(id);
        entity.setCustomerCode(code);
        entity.setCustomerName("测试客户");
        entity.setContactName("联系人");
        entity.setContactPhone("13900000000");
        entity.setLevel("B");
        entity.setStatus(status);
        entity.setDeleted(0L);
        return entity;
    }

    private static ErrorCode errorCodeOf(Throwable throwable) {
        return ((BusinessException) throwable).getErrorCode();
    }

    private CustomerCreateRequest createRequest(String code) {
        CustomerCreateRequest request = new CustomerCreateRequest();
        request.setCustomerCode(code);
        request.setCustomerName("新建客户");
        request.setContactName("王五");
        request.setContactPhone("13912345678");
        request.setLevel("A");
        return request;
    }

    @Test
    @DisplayName("客户编号重复时拒绝新增")
    void shouldRejectDuplicateCustomerCode() {
        given(customerMapper.selectCount(any())).willReturn(1L);

        assertThatThrownBy(() -> customerService.create(createRequest("CUS-0001")))
                .isInstanceOf(BusinessException.class)
                .extracting(CustomerServiceTest::errorCodeOf)
                .isEqualTo(ErrorCode.PARAM_INVALID);

        verify(customerMapper, never()).insert(any(Customer.class));
    }

    @Test
    @DisplayName("新增客户成功时状态默认为启用且写入审计字段")
    void shouldCreateCustomerWithDefaults() {
        given(customerMapper.selectCount(any())).willReturn(0L);

        customerService.create(createRequest("CUS-9001"));

        org.mockito.ArgumentCaptor<Customer> captor = org.mockito.ArgumentCaptor.forClass(Customer.class);
        verify(customerMapper).insert(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(1);
        assertThat(captor.getValue().getDeleted()).isZero();
        assertThat(captor.getValue().getCreateBy()).isEqualTo(2L);
    }

    @Test
    @DisplayName("客户详情超出数据域时返回 1102")
    void customerRoleCannotReadOtherCustomer() {
        loginAs("ROLE_CUSTOMER", 100L);
        given(customerMapper.selectOne(any())).willReturn(customer(200L, "CUS-0200", 1));

        assertThatThrownBy(() -> customerService.detail(200L))
                .isInstanceOf(BusinessException.class)
                .extracting(CustomerServiceTest::errorCodeOf)
                .isEqualTo(ErrorCode.DATA_SCOPE_DENIED);
    }

    @Test
    @DisplayName("客户不存在时返回 1301")
    void shouldReturnNotFoundWhenCustomerMissing() {
        given(customerMapper.selectOne(any())).willReturn(null);

        assertThatThrownBy(() -> customerService.detail(999L))
                .isInstanceOf(BusinessException.class)
                .extracting(CustomerServiceTest::errorCodeOf)
                .isEqualTo(ErrorCode.CUSTOMER_NOT_FOUND);
    }

    @Test
    @DisplayName("存在未完成工单时不允许停用客户")
    void shouldRejectDisableWhenUnfinishedTicketsExist() {
        given(customerMapper.selectOne(any())).willReturn(customer(1L, "CUS-0001", 1));
        given(ticketStatMapper.countUnfinishedTicketsByCustomerId(1L)).willReturn(3L);

        assertThatThrownBy(() -> customerService.updateStatus(1L, 0))
                .isInstanceOf(BusinessException.class)
                .extracting(CustomerServiceTest::errorCodeOf)
                .isEqualTo(ErrorCode.PARAM_INVALID);

        verify(customerMapper, never()).update(any(), any());
    }

    @Test
    @DisplayName("名下仍有设备时不允许删除客户")
    void shouldRejectDeleteWhenDevicesExist() {
        given(customerMapper.selectOne(any())).willReturn(customer(1L, "CUS-0001", 1));
        given(deviceMapper.selectCount(any())).willReturn(2L);

        assertThatThrownBy(() -> customerService.delete(1L))
                .isInstanceOf(BusinessException.class)
                .extracting(CustomerServiceTest::errorCodeOf)
                .isEqualTo(ErrorCode.PARAM_INVALID);
    }

    @Test
    @DisplayName("已停用客户不能作为设备或工单的关联对象")
    void disabledCustomerCannotBeUsed() {
        given(customerMapper.selectOne(any())).willReturn(customer(1L, "CUS-0001", 0));

        assertThatThrownBy(() -> customerService.requireEnabledCustomer(1L))
                .isInstanceOf(BusinessException.class)
                .extracting(CustomerServiceTest::errorCodeOf)
                .isEqualTo(ErrorCode.PARAM_INVALID);
    }
}

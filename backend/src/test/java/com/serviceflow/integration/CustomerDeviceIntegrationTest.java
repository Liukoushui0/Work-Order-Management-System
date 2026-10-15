package com.serviceflow.integration;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 客户与设备模块集成测试：真实 MySQL 下的增删改查、筛选、关联完整性与数据隔离。
 */
class CustomerDeviceIntegrationTest extends BaseIntegrationTest {

    private static final long SEED_CUSTOMER_A = 1L;
    private static final long SEED_CUSTOMER_B = 2L;

    private Map<String, Object> customerBody(String code, String name, String level) {
        Map<String, Object> body = new HashMap<>();
        body.put("customerCode", code);
        body.put("customerName", name);
        body.put("contactName", "测试联系人");
        body.put("contactPhone", "13900001111");
        body.put("contactEmail", "contact@it.local");
        body.put("level", level);
        body.put("source", "WEB");
        body.put("province", "江苏省");
        body.put("city", "南京市");
        return body;
    }

    private Map<String, Object> deviceBody(String code, String name, long customerId, String sn) {
        Map<String, Object> body = new HashMap<>();
        body.put("deviceCode", code);
        body.put("deviceName", name);
        body.put("deviceType", "智能终端");
        body.put("brand", "ServiceFlow");
        body.put("model", "IT-MODEL");
        body.put("snNo", sn);
        body.put("customerId", customerId);
        body.put("purchaseDate", "2026-01-01");
        body.put("warrantyExpireDate", "2029-01-01");
        body.put("status", 1);
        return body;
    }

    @Test
    @DisplayName("客服可创建客户与设备，客户详情返回设备数量")
    void serviceCanCreateCustomerAndDevice() throws Exception {
        String token = login("service01", PASSWORD);

        JsonNode customer = call(HttpMethod.POST, "/api/v1/customers", token,
                customerBody("IT-CUS-01", "集成测试客户", "A"), 200);
        long customerId = customer.path("data").path("id").asLong();
        assertThat(customerId).isPositive();

        call(HttpMethod.POST, "/api/v1/devices", token,
                deviceBody("IT-DEV-01", "集成测试设备", customerId, "IT-SN-01"), 200);

        JsonNode detail = call(HttpMethod.GET, "/api/v1/customers/" + customerId, token, null, 200);
        assertThat(detail.path("data").path("deviceCount").asLong()).isEqualTo(1);
        assertThat(detail.path("data").path("ticketCount").asLong()).isZero();
    }

    @Test
    @DisplayName("客户编号或设备编号重复时拒绝创建")
    void duplicateCodesAreRejected() throws Exception {
        String token = login("service01", PASSWORD);
        call(HttpMethod.POST, "/api/v1/customers", token, customerBody("IT-CUS-DUP", "重复客户", "B"), 200);

        JsonNode duplicate = call(HttpMethod.POST, "/api/v1/customers", token,
                customerBody("IT-CUS-DUP", "重复客户2", "B"), 400);
        assertThat(duplicate.path("message").asText()).contains("客户编号已存在");

        JsonNode device = call(HttpMethod.POST, "/api/v1/devices", token,
                deviceBody("IT-DEV-DUP", "设备", SEED_CUSTOMER_A, "IT-SN-DUP"), 200);
        long deviceId = device.path("data").path("id").asLong();
        assertThat(deviceId).isPositive();

        JsonNode duplicateDevice = call(HttpMethod.POST, "/api/v1/devices", token,
                deviceBody("IT-DEV-DUP", "设备2", SEED_CUSTOMER_A, "IT-SN-DUP2"), 400);
        assertThat(duplicateDevice.path("message").asText()).contains("设备编号已存在");
    }

    @Test
    @DisplayName("设备序列号重复时拒绝创建")
    void duplicateSerialNumberIsRejected() throws Exception {
        String token = login("service01", PASSWORD);
        call(HttpMethod.POST, "/api/v1/devices", token,
                deviceBody("IT-DEV-SN1", "设备一", SEED_CUSTOMER_A, "IT-SN-SAME"), 200);

        JsonNode response = call(HttpMethod.POST, "/api/v1/devices", token,
                deviceBody("IT-DEV-SN2", "设备二", SEED_CUSTOMER_A, "IT-SN-SAME"), 400);

        assertThat(response.path("message").asText()).contains("设备序列号已存在");
    }

    @Test
    @DisplayName("新建设备时客户必须存在且可用")
    void deviceRequiresExistingEnabledCustomer() throws Exception {
        String token = login("service01", PASSWORD);

        JsonNode notExists = call(HttpMethod.POST, "/api/v1/devices", token,
                deviceBody("IT-DEV-NC", "设备", 999999L, "IT-SN-NC"), 404);
        assertThat(notExists.path("code").asInt()).isEqualTo(1301);
    }

    @Test
    @DisplayName("客户列表支持关键词、等级与状态筛选")
    void customerListSupportsFilters() throws Exception {
        String token = login("service01", PASSWORD);
        call(HttpMethod.POST, "/api/v1/customers", token, customerBody("IT-CUS-F1", "筛选客户甲", "A"), 200);
        call(HttpMethod.POST, "/api/v1/customers", token, customerBody("IT-CUS-F2", "筛选客户乙", "C"), 200);

        JsonNode byKeyword = call(HttpMethod.GET, "/api/v1/customers?keyword=筛选客户甲", token, null, 200);
        assertThat(byKeyword.path("data").path("total").asLong()).isEqualTo(1);

        JsonNode byLevel = call(HttpMethod.GET, "/api/v1/customers?level=C&keyword=筛选客户", token, null, 200);
        assertThat(byLevel.path("data").path("total").asLong()).isEqualTo(1);
        assertThat(byLevel.path("data").path("records").get(0).path("customerName").asText())
                .isEqualTo("筛选客户乙");
    }

    @Test
    @DisplayName("客户角色只能看到自己的档案与设备，访问他人资源返回 403")
    void customerRoleIsIsolated() throws Exception {
        String token = login("customer01", PASSWORD);

        JsonNode list = call(HttpMethod.GET, "/api/v1/customers", token, null, 200);
        assertThat(list.path("data").path("total").asLong()).isEqualTo(1);
        assertThat(list.path("data").path("records").get(0).path("id").asLong()).isEqualTo(SEED_CUSTOMER_A);

        JsonNode otherCustomer = call(HttpMethod.GET, "/api/v1/customers/" + SEED_CUSTOMER_B, token, null, 403);
        assertThat(otherCustomer.path("code").asInt()).isEqualTo(1102);

        JsonNode devices = call(HttpMethod.GET, "/api/v1/devices", token, null, 200);
        for (JsonNode record : devices.path("data").path("records")) {
            assertThat(record.path("customerId").asLong()).isEqualTo(SEED_CUSTOMER_A);
        }

        JsonNode otherDevice = call(HttpMethod.GET, "/api/v1/devices/4", token, null, 403);
        assertThat(otherDevice.path("code").asInt()).isEqualTo(1102);
    }

    @Test
    @DisplayName("客户角色不能新增客户或设备")
    void customerRoleCannotManageCustomerOrDevice() throws Exception {
        String token = login("customer01", PASSWORD);

        assertThat(call(HttpMethod.POST, "/api/v1/customers", token,
                customerBody("IT-CUS-RO", "越权客户", "B"), 403).path("code").asInt()).isEqualTo(1101);
        assertThat(call(HttpMethod.POST, "/api/v1/devices", token,
                deviceBody("IT-DEV-RO", "越权设备", SEED_CUSTOMER_A, "IT-SN-RO"), 403).path("code").asInt())
                .isEqualTo(1101);
    }

    @Test
    @DisplayName("工程师可读设备但无维护权限")
    void engineerCanReadDevicesOnly() throws Exception {
        String token = login("engineer01", PASSWORD);

        assertThat(call(HttpMethod.GET, "/api/v1/devices", token, null, 200).path("code").asInt()).isZero();
        assertThat(call(HttpMethod.POST, "/api/v1/devices", token,
                deviceBody("IT-DEV-EN", "工程师建的设备", SEED_CUSTOMER_A, "IT-SN-EN"), 403).path("code").asInt())
                .isEqualTo(1101);
        assertThat(call(HttpMethod.GET, "/api/v1/customers", token, null, 403).path("code").asInt())
                .isEqualTo(1101);
    }

    @Test
    @DisplayName("参数校验失败返回统一 1201 结构")
    void validationErrorsUseUnifiedStructure() throws Exception {
        String token = login("service01", PASSWORD);
        Map<String, Object> body = customerBody("IT-CUS-V1", "校验客户", "A");
        body.put("contactPhone", "12345");
        body.put("level", "X");

        JsonNode response = call(HttpMethod.POST, "/api/v1/customers", token, body, 400);

        assertThat(response.path("code").asInt()).isEqualTo(1201);
        assertThat(response.path("message").asText()).contains("联系电话格式不正确");
    }

    @Test
    @DisplayName("停用与删除客户受关联数据保护")
    void customerStatusAndDeleteAreProtected() throws Exception {
        String token = login("service01", PASSWORD);
        JsonNode created = call(HttpMethod.POST, "/api/v1/customers", token,
                customerBody("IT-CUS-P1", "受保护客户", "B"), 200);
        long customerId = created.path("data").path("id").asLong();
        call(HttpMethod.POST, "/api/v1/devices", token,
                deviceBody("IT-DEV-P1", "受保护设备", customerId, "IT-SN-P1"), 200);

        JsonNode deleteBlocked = call(HttpMethod.DELETE, "/api/v1/customers/" + customerId, token, null, 400);
        assertThat(deleteBlocked.path("message").asText()).contains("设备");

        // 无未完成工单时可以正常停用
        JsonNode disabled = call(HttpMethod.PUT, "/api/v1/customers/" + customerId + "/status", token,
                Map.of("status", 0), 200);
        assertThat(disabled.path("data").path("status").asInt()).isZero();

        // 停用后的客户不能再关联新设备
        JsonNode newDevice = call(HttpMethod.POST, "/api/v1/devices", token,
                deviceBody("IT-DEV-P2", "停用客户设备", customerId, "IT-SN-P2"), 400);
        assertThat(newDevice.path("message").asText()).contains("客户已停用");
    }

    @Test
    @DisplayName("设备存在工单记录时不允许删除")
    void deviceWithTicketsCannotBeDeleted() throws Exception {
        String token = login("service01", PASSWORD);
        JsonNode device = call(HttpMethod.POST, "/api/v1/devices", token,
                deviceBody("IT-DEV-T1", "有工单设备", SEED_CUSTOMER_A, "IT-SN-T1"), 200);
        long deviceId = device.path("data").path("id").asLong();
        jdbcTemplate.update("INSERT INTO service_ticket (ticket_no, title, customer_id, device_id, status, "
                + "priority, source, create_time) VALUES (?, ?, ?, ?, 'PENDING_ASSIGN', 'MEDIUM', 'WEB', NOW())",
                "IT-TK-" + deviceId, "集成测试工单", SEED_CUSTOMER_A, deviceId);

        JsonNode response = call(HttpMethod.DELETE, "/api/v1/devices/" + deviceId, token, null, 400);

        assertThat(response.path("message").asText()).contains("工单记录");
        jdbcTemplate.update("DELETE FROM service_ticket WHERE ticket_no = ?", "IT-TK-" + deviceId);
    }
}

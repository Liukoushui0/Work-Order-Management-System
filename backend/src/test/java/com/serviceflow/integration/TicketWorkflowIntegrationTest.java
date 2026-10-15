package com.serviceflow.integration;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;

/**
 * 工单核心流程集成测试：真实 MySQL 下的状态流转、并发保护、数据隔离与逾期查询。
 */
class TicketWorkflowIntegrationTest extends BaseIntegrationTest {

    private static final long ADMIN = 1L;
    private static final long SERVICE = 2L;
    private static final long ENGINEER_ONE = 3L;
    private static final long ENGINEER_TWO = 4L;
    private static final long CUSTOMER_ONE = 1L;
    private static final long CUSTOMER_TWO = 2L;
    private static final long DEVICE_OF_CUSTOMER_ONE = 1L;
    private static final long DEVICE_OF_CUSTOMER_TWO = 4L;
    private static final long CATEGORY_HARDWARE = 1L;

    private Map<String, Object> ticketBody(long customerId, long deviceId, long categoryId) {
        Map<String, Object> body = new HashMap<>();
        body.put("title", "集成测试工单");
        body.put("description", "集成测试故障描述");
        body.put("customerId", customerId);
        body.put("deviceId", deviceId);
        body.put("faultCategoryId", categoryId);
        body.put("priority", "HIGH");
        body.put("source", "WEB");
        body.put("reporterName", "集成测试报修人");
        body.put("reporterPhone", "13900002222");
        return body;
    }

    private long createTicket(String token, long customerId, long deviceId) throws Exception {
        JsonNode created = call(HttpMethod.POST, "/api/v1/tickets", token,
                ticketBody(customerId, deviceId, CATEGORY_HARDWARE), 200);
        return created.path("data").path("id").asLong();
    }

    private void assign(String token, long ticketId, long engineerId) throws Exception {
        call(HttpMethod.POST, "/api/v1/tickets/" + ticketId + "/assign", token,
                Map.of("assigneeId", engineerId, "remark", "集成测试分派"), 200);
    }

    private int rawStatus(HttpMethod method, String url, String token, Object body) throws Exception {
        var builder = request(method, url).contentType(MediaType.APPLICATION_JSON);
        if (token != null) {
            builder.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        }
        if (body != null) {
            builder.content(objectMapper.writeValueAsString(body));
        }
        return mockMvc.perform(builder).andReturn().getResponse().getStatus();
    }

    @Test
    @DisplayName("完整主流程：创建 → 分派 → 接单 → 处理 → 完成 → 客户确认，并留下完整历史")
    void shouldCompleteFullLifecycle() throws Exception {
        String serviceToken = login("service01", PASSWORD);
        String engineerToken = login("engineer01", PASSWORD);
        String customerToken = login("customer01", PASSWORD);

        JsonNode created = call(HttpMethod.POST, "/api/v1/tickets", serviceToken,
                ticketBody(CUSTOMER_ONE, DEVICE_OF_CUSTOMER_ONE, CATEGORY_HARDWARE), 200);
        long ticketId = created.path("data").path("id").asLong();
        String ticketNo = created.path("data").path("ticketNo").asText();

        assertThat(ticketNo).matches("^SO\\d{8}\\d{6}$");
        assertThat(created.path("data").path("status").asText()).isEqualTo("PENDING_ASSIGN");
        assertThat(created.path("data").path("customerName").asText()).isNotBlank();
        assertThat(created.path("data").path("deviceName").asText()).isNotBlank();
        assertThat(created.path("data").path("deadlineTime").asText()).isNotBlank();

        assign(serviceToken, ticketId, ENGINEER_ONE);

        JsonNode accepted = call(HttpMethod.POST, "/api/v1/tickets/" + ticketId + "/accept", engineerToken,
                null, 200);
        assertThat(accepted.path("data").path("status").asText()).isEqualTo("PROCESSING");
        assertThat(accepted.path("data").path("assigneeName").asText()).isEqualTo("维修工程师一号");

        call(HttpMethod.POST, "/api/v1/tickets/" + ticketId + "/process", engineerToken,
                Map.of("content", "已更换故障模块", "usedHours", 2.5), 200);

        JsonNode completed = call(HttpMethod.POST, "/api/v1/tickets/" + ticketId + "/complete", engineerToken,
                Map.of("result", "设备已恢复正常", "usedHours", 3), 200);
        assertThat(completed.path("data").path("status").asText()).isEqualTo("PENDING_CONFIRM");
        assertThat(completed.path("data").path("handleHours").asDouble()).isGreaterThanOrEqualTo(0);

        JsonNode confirmed = call(HttpMethod.POST, "/api/v1/tickets/" + ticketId + "/confirm", customerToken,
                Map.of("remark", "服务及时，问题已解决"), 200);
        assertThat(confirmed.path("data").path("status").asText()).isEqualTo("COMPLETED");

        JsonNode detail = call(HttpMethod.GET, "/api/v1/tickets/" + ticketId, serviceToken, null, 200);
        List<String> actions = new ArrayList<>();
        for (JsonNode record : detail.path("data").path("records")) {
            actions.add(record.path("action").asText());
            assertThat(record.path("operatorName").asText()).isNotBlank();
        }
        assertThat(actions).containsExactly("CREATE", "ASSIGN", "ACCEPT", "PROCESS", "COMPLETE", "CONFIRM");
        assertThat(detail.path("data").path("customerContactPhone").asText()).isNotBlank();
        assertThat(detail.path("data").path("deviceModel").asText()).isNotBlank();
    }

    @Test
    @DisplayName("非法状态跳转被拒绝：待分派不能接单，待接单不能直接完成")
    void shouldRejectIllegalTransitions() throws Exception {
        String serviceToken = login("service01", PASSWORD);
        String engineerToken = login("engineer01", PASSWORD);
        long ticketId = createTicket(serviceToken, CUSTOMER_ONE, DEVICE_OF_CUSTOMER_ONE);

        JsonNode acceptBeforeAssign = call(HttpMethod.POST, "/api/v1/tickets/" + ticketId + "/accept",
                engineerToken, null, 409);
        assertThat(acceptBeforeAssign.path("code").asInt()).isEqualTo(1402);

        assign(serviceToken, ticketId, ENGINEER_ONE);

        JsonNode completeBeforeProcess = call(HttpMethod.POST, "/api/v1/tickets/" + ticketId + "/complete",
                engineerToken, Map.of("result", "直接完成"), 409);
        assertThat(completeBeforeProcess.path("code").asInt()).isEqualTo(1402);

        call(HttpMethod.POST, "/api/v1/tickets/" + ticketId + "/accept", engineerToken, null, 200);
        JsonNode completeWithoutProcess = call(HttpMethod.POST, "/api/v1/tickets/" + ticketId + "/complete",
                engineerToken, Map.of("result", "未填处理记录"), 400);
        assertThat(completeWithoutProcess.path("message").asText()).contains("请先填写处理记录");
    }

    @Test
    @DisplayName("接单并发保护：并发重复接单只有一个成功，处理记录不重复")
    void concurrentAcceptOnlySucceedsOnce() throws Exception {
        String serviceToken = login("service01", PASSWORD);
        String engineerToken = login("engineer01", PASSWORD);
        long ticketId = createTicket(serviceToken, CUSTOMER_ONE, DEVICE_OF_CUSTOMER_ONE);
        assign(serviceToken, ticketId, ENGINEER_ONE);

        int threads = 4;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch startGate = new CountDownLatch(1);
        List<Callable<Integer>> tasks = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            tasks.add(() -> {
                startGate.await();
                return rawStatus(HttpMethod.POST, "/api/v1/tickets/" + ticketId + "/accept", engineerToken, null);
            });
        }
        List<Future<Integer>> futures = new ArrayList<>();
        tasks.forEach(task -> futures.add(pool.submit(task)));
        startGate.countDown();
        List<Integer> statuses = new ArrayList<>();
        for (Future<Integer> future : futures) {
            statuses.add(future.get());
        }
        pool.shutdown();

        long successCount = statuses.stream().filter(status -> status == 200).count();
        assertThat(successCount).as("并发接单只允许一个成功，实际成功 %s 次", successCount).isEqualTo(1);
        statuses.stream().filter(status -> status != 200)
                .forEach(status -> assertThat(status).isIn(409, 403));

        Integer acceptRecords = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM ticket_process_record WHERE ticket_id = ? AND action = 'ACCEPT'",
                Integer.class, ticketId);
        assertThat(acceptRecords).isEqualTo(1);
        String status = jdbcTemplate.queryForObject("SELECT status FROM service_ticket WHERE id = ?",
                String.class, ticketId);
        assertThat(status).isEqualTo("PROCESSING");
    }

    @Test
    @DisplayName("工单编号并发唯一：多线程同时建单不产生重复编号")
    void concurrentCreateGeneratesUniqueTicketNumbers() throws Exception {
        String serviceToken = login("service01", PASSWORD);
        int threads = 5;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch startGate = new CountDownLatch(1);
        List<Callable<Integer>> tasks = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            tasks.add(() -> {
                startGate.await();
                return rawStatus(HttpMethod.POST, "/api/v1/tickets", serviceToken,
                        ticketBody(CUSTOMER_ONE, DEVICE_OF_CUSTOMER_ONE, CATEGORY_HARDWARE));
            });
        }
        List<Future<Integer>> futures = new ArrayList<>();
        tasks.forEach(task -> futures.add(pool.submit(task)));
        startGate.countDown();
        for (Future<Integer> future : futures) {
            assertThat(future.get()).isEqualTo(200);
        }
        pool.shutdown();

        List<String> numbers = jdbcTemplate.queryForList(
                "SELECT ticket_no FROM service_ticket WHERE ticket_no LIKE CONCAT('SO', DATE_FORMAT(NOW(), '%Y%m%d'), '%')",
                String.class);
        assertThat(numbers).hasSize(threads);
        assertThat(new HashSet<>(numbers)).hasSize(threads);
    }

    @Test
    @DisplayName("越权访问：客户只能看自己的工单，工程师只能看分配给自己的工单")
    void shouldEnforceTicketDataScope() throws Exception {
        String serviceToken = login("service01", PASSWORD);
        String engineerToken = login("engineer01", PASSWORD);
        String customerOneToken = login("customer01", PASSWORD);

        long ownTicket = createTicket(serviceToken, CUSTOMER_ONE, DEVICE_OF_CUSTOMER_ONE);
        long otherTicket = createTicket(serviceToken, CUSTOMER_TWO, DEVICE_OF_CUSTOMER_TWO);
        assign(serviceToken, ownTicket, ENGINEER_ONE);

        assertThat(call(HttpMethod.GET, "/api/v1/tickets/" + ownTicket, customerOneToken, null, 200)
                .path("code").asInt()).isZero();
        assertThat(call(HttpMethod.GET, "/api/v1/tickets/" + otherTicket, customerOneToken, null, 403)
                .path("code").asInt()).isEqualTo(1102);
        assertThat(call(HttpMethod.GET, "/api/v1/tickets/" + ownTicket, engineerToken, null, 200)
                .path("code").asInt()).isZero();
        assertThat(call(HttpMethod.GET, "/api/v1/tickets/" + otherTicket, engineerToken, null, 403)
                .path("code").asInt()).isEqualTo(1102);

        JsonNode customerList = call(HttpMethod.GET, "/api/v1/tickets?pageSize=50", customerOneToken, null, 200);
        for (JsonNode record : customerList.path("data").path("records")) {
            assertThat(record.path("customerId").asLong()).isEqualTo(CUSTOMER_ONE);
        }
        JsonNode engineerList = call(HttpMethod.GET, "/api/v1/tickets?pageSize=50", engineerToken, null, 200);
        for (JsonNode record : engineerList.path("data").path("records")) {
            assertThat(record.path("assigneeId").asLong()).isEqualTo(ENGINEER_ONE);
        }
    }

    @Test
    @DisplayName("客户角色不能创建工单，工程师不能分派工单")
    void shouldEnforceTicketOperationPermissions() throws Exception {
        String customerToken = login("customer01", PASSWORD);
        String engineerToken = login("engineer01", PASSWORD);

        assertThat(call(HttpMethod.POST, "/api/v1/tickets", customerToken,
                ticketBody(CUSTOMER_ONE, DEVICE_OF_CUSTOMER_ONE, CATEGORY_HARDWARE), 403)
                .path("code").asInt()).isEqualTo(1101);

        String serviceToken = login("service01", PASSWORD);
        long ticketId = createTicket(serviceToken, CUSTOMER_ONE, DEVICE_OF_CUSTOMER_ONE);
        assertThat(call(HttpMethod.POST, "/api/v1/tickets/" + ticketId + "/assign", engineerToken,
                Map.of("assigneeId", ENGINEER_ONE), 403).path("code").asInt()).isEqualTo(1101);
    }

    @Test
    @DisplayName("工程师不能接单非本人负责的工单（1405）")
    void otherEngineerCannotAcceptTicket() throws Exception {
        String serviceToken = login("service01", PASSWORD);
        String engineerTwoToken = login("engineer02", PASSWORD);
        long ticketId = createTicket(serviceToken, CUSTOMER_ONE, DEVICE_OF_CUSTOMER_ONE);
        assign(serviceToken, ticketId, ENGINEER_ONE);

        JsonNode response = call(HttpMethod.POST, "/api/v1/tickets/" + ticketId + "/accept",
                engineerTwoToken, null, 403);

        assertThat(response.path("code").asInt()).isEqualTo(1405);
    }

    @Test
    @DisplayName("逾期工单查询返回超过截止时间的未结束工单")
    void overdueQueryReturnsExpiredTickets() throws Exception {
        String serviceToken = login("service01", PASSWORD);

        JsonNode overdue = call(HttpMethod.GET, "/api/v1/tickets/overdue?pageSize=50", serviceToken, null, 200);

        assertThat(overdue.path("data").path("total").asLong()).isGreaterThan(0);
        boolean containsOverdueDemoTicket = false;
        for (JsonNode record : overdue.path("data").path("records")) {
            assertThat(record.path("status").asText()).isNotIn("COMPLETED", "CANCELLED");
            assertThat(record.path("overdue").asBoolean()).isTrue();
            if ("SO20260101000008".equals(record.path("ticketNo").asText())) {
                containsOverdueDemoTicket = true;
            }
        }
        assertThat(containsOverdueDemoTicket).as("演示数据中的逾期工单应被查出").isTrue();
    }

    @Test
    @DisplayName("取消后的工单不能被继续操作")
    void cancelledTicketCannotBeOperated() throws Exception {
        String serviceToken = login("service01", PASSWORD);
        long ticketId = createTicket(serviceToken, CUSTOMER_ONE, DEVICE_OF_CUSTOMER_ONE);

        JsonNode cancelled = call(HttpMethod.POST, "/api/v1/tickets/" + ticketId + "/cancel", serviceToken,
                Map.of("reason", "客户重复报修"), 200);
        assertThat(cancelled.path("data").path("status").asText()).isEqualTo("CANCELLED");

        JsonNode assignAfterCancel = call(HttpMethod.POST, "/api/v1/tickets/" + ticketId + "/assign",
                serviceToken, Map.of("assigneeId", ENGINEER_ONE), 409);
        assertThat(assignAfterCancel.path("code").asInt()).isEqualTo(1404);
    }

    @Test
    @DisplayName("转派后需要重新接单，转派对象与当前负责人相同时拒绝")
    void transferRequiresReAccept() throws Exception {
        String serviceToken = login("service01", PASSWORD);
        String engineerTwoToken = login("engineer02", PASSWORD);
        long ticketId = createTicket(serviceToken, CUSTOMER_ONE, DEVICE_OF_CUSTOMER_ONE);
        assign(serviceToken, ticketId, ENGINEER_ONE);

        JsonNode sameAssignee = call(HttpMethod.POST, "/api/v1/tickets/" + ticketId + "/transfer", serviceToken,
                Map.of("assigneeId", ENGINEER_ONE, "reason", "原地转派"), 400);
        assertThat(sameAssignee.path("message").asText()).contains("不能与当前负责人相同");

        JsonNode transferred = call(HttpMethod.POST, "/api/v1/tickets/" + ticketId + "/transfer", serviceToken,
                Map.of("assigneeId", ENGINEER_TWO, "reason", "工程师一号休假"), 200);
        assertThat(transferred.path("data").path("status").asText()).isEqualTo("PENDING_ACCEPT");
        assertThat(transferred.path("data").path("assigneeId").asLong()).isEqualTo(ENGINEER_TWO);
        assertThat(transferred.path("data").path("transferCount").asInt()).isEqualTo(1);

        call(HttpMethod.POST, "/api/v1/tickets/" + ticketId + "/accept", engineerTwoToken, null, 200);
        assertThat(jdbcTemplate.queryForObject("SELECT status FROM service_ticket WHERE id = ?", String.class,
                ticketId)).isEqualTo("PROCESSING");
    }

    @Test
    @DisplayName("退回工单后回到待分派，且不再有有效分派记录")
    void rejectReturnsTicketToPool() throws Exception {
        String serviceToken = login("service01", PASSWORD);
        String engineerToken = login("engineer01", PASSWORD);
        long ticketId = createTicket(serviceToken, CUSTOMER_ONE, DEVICE_OF_CUSTOMER_ONE);
        assign(serviceToken, ticketId, ENGINEER_ONE);

        JsonNode rejected = call(HttpMethod.POST, "/api/v1/tickets/" + ticketId + "/reject", engineerToken,
                Map.of("reason", "现场设备型号与工单不符"), 200);

        assertThat(rejected.path("data").path("status").asText()).isEqualTo("PENDING_ASSIGN");
        assertThat(rejected.path("data").path("assigneeId").isMissingNode()
                || rejected.path("data").path("assigneeId").isNull()).isTrue();
        Integer activeAssignments = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM ticket_assignment WHERE ticket_id = ? AND active_flag = 1",
                Integer.class, ticketId);
        assertThat(activeAssignments).isZero();
    }

    @Test
    @DisplayName("管理员可以查看任意工单，且已完工单不能被取消")
    void adminCanViewAndCompletedTicketCannotBeCancelled() throws Exception {
        String adminToken = login("admin", PASSWORD);
        String serviceToken = login("service01", PASSWORD);
        String engineerToken = login("engineer01", PASSWORD);
        String customerToken = login("customer01", PASSWORD);
        long ticketId = createTicket(serviceToken, CUSTOMER_ONE, DEVICE_OF_CUSTOMER_ONE);
        assign(serviceToken, ticketId, ENGINEER_ONE);
        call(HttpMethod.POST, "/api/v1/tickets/" + ticketId + "/accept", engineerToken, null, 200);
        call(HttpMethod.POST, "/api/v1/tickets/" + ticketId + "/process", engineerToken,
                Map.of("content", "处理完成"), 200);
        call(HttpMethod.POST, "/api/v1/tickets/" + ticketId + "/complete", engineerToken,
                Map.of("result", "已修复"), 200);
        call(HttpMethod.POST, "/api/v1/tickets/" + ticketId + "/confirm", customerToken, Map.of(), 200);

        assertThat(call(HttpMethod.GET, "/api/v1/tickets/" + ticketId, adminToken, null, 200).path("code").asInt())
                .isZero();
        JsonNode cancelCompleted = call(HttpMethod.POST, "/api/v1/tickets/" + ticketId + "/cancel", serviceToken,
                Map.of("reason", "误操作"), 409);
        assertThat(cancelCompleted.path("message").asText()).contains("已完成工单不能取消");
    }

    @Test
    @DisplayName("待确认状态可由客户退回重做，工单回到处理中")
    void customerCanReopenPendingConfirmTicket() throws Exception {
        String serviceToken = login("service01", PASSWORD);
        String engineerToken = login("engineer01", PASSWORD);
        String customerToken = login("customer01", PASSWORD);
        long ticketId = createTicket(serviceToken, CUSTOMER_ONE, DEVICE_OF_CUSTOMER_ONE);
        assign(serviceToken, ticketId, ENGINEER_ONE);
        call(HttpMethod.POST, "/api/v1/tickets/" + ticketId + "/accept", engineerToken, null, 200);
        call(HttpMethod.POST, "/api/v1/tickets/" + ticketId + "/process", engineerToken,
                Map.of("content", "处理完成"), 200);
        call(HttpMethod.POST, "/api/v1/tickets/" + ticketId + "/complete", engineerToken,
                Map.of("result", "已修复"), 200);

        JsonNode reopened = call(HttpMethod.POST, "/api/v1/tickets/" + ticketId + "/reopen", customerToken,
                Map.of("reason", "故障仍然存在"), 200);

        assertThat(reopened.path("data").path("status").asText()).isEqualTo("PROCESSING");
        assertThat(reopened.path("data").path("reopenCount").asInt()).isEqualTo(1);
        Set<Long> ignored = new HashSet<>();
        ignored.add(ADMIN);
        ignored.add(SERVICE);
    }
}

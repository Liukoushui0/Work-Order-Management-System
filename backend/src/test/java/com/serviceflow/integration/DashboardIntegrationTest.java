package com.serviceflow.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.serviceflow.service.StatisticsCacheService;
import com.serviceflow.service.TicketScheduledService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 运营统计与缓存集成测试：统计数值与数据库真实结果比对，并验证缓存命中与写操作后失效。
 */
class DashboardIntegrationTest extends BaseIntegrationTest {

    private static final long ENGINEER_ONE = 3L;
    private static final long CUSTOMER_ONE = 1L;
    private static final long DEVICE_OF_CUSTOMER_ONE = 1L;
    private static final long CATEGORY_HARDWARE = 1L;

    @Autowired
    private StatisticsCacheService cacheService;

    @Autowired
    private TicketScheduledService scheduledService;

    @BeforeEach
    void clearStatisticsCache() {
        cacheService.evictStatistics();
    }

    private Map<String, Object> ticketBody(String priority) {
        Map<String, Object> body = new HashMap<>();
        body.put("title", "统计集成测试工单");
        body.put("description", "用于统计断言");
        body.put("customerId", CUSTOMER_ONE);
        body.put("deviceId", DEVICE_OF_CUSTOMER_ONE);
        body.put("faultCategoryId", CATEGORY_HARDWARE);
        body.put("priority", priority);
        return body;
    }

    private long createTicket(String token, String priority) throws Exception {
        return call(HttpMethod.POST, "/api/v1/tickets", token, ticketBody(priority), 200)
                .path("data").path("id").asLong();
    }

    @Test
    @DisplayName("总览指标与数据库真实统计一致")
    void overviewMatchesDatabase() throws Exception {
        String adminToken = login("admin", PASSWORD);

        JsonNode overview = call(HttpMethod.GET, "/api/v1/dashboard/overview", adminToken, null, 200)
                .path("data");

        Long expectedTotal = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM service_ticket", Long.class);
        Long expectedOverdue = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM service_ticket WHERE status NOT IN ('COMPLETED','CANCELLED') "
                        + "AND deadline_time IS NOT NULL AND deadline_time < NOW()", Long.class);
        Long expectedCompleted = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM service_ticket WHERE status = 'COMPLETED'", Long.class);
        Long expectedOpen = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM service_ticket WHERE status NOT IN ('COMPLETED','CANCELLED')", Long.class);

        assertThat(overview.path("total").asLong()).isEqualTo(expectedTotal);
        assertThat(overview.path("overdueCount").asLong()).isEqualTo(expectedOverdue);
        assertThat(overview.path("completedCount").asLong()).isEqualTo(expectedCompleted);
        assertThat(overview.path("openCount").asLong()).isEqualTo(expectedOpen);
        assertThat(overview.path("scope").asText()).isEqualTo("ALL");

        long statusSum = 0;
        for (JsonNode item : overview.path("statusDistribution")) {
            statusSum += item.path("value").asLong();
            assertThat(item.path("name").asText()).isNotBlank();
        }
        assertThat(statusSum).isEqualTo(expectedTotal);
    }

    @Test
    @DisplayName("工程师只看到与自己相关的统计")
    void engineerStatisticsAreScoped() throws Exception {
        String engineerToken = login("engineer01", PASSWORD);

        JsonNode overview = call(HttpMethod.GET, "/api/v1/dashboard/overview", engineerToken, null, 200)
                .path("data");

        Long expectedTotal = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM service_ticket WHERE assignee_id = ?", Long.class, ENGINEER_ONE);
        assertThat(overview.path("total").asLong()).isEqualTo(expectedTotal);
        assertThat(overview.path("scope").asText()).isEqualTo("SELF");

        JsonNode workload = call(HttpMethod.GET, "/api/v1/dashboard/engineer-workload", engineerToken, null, 200)
                .path("data");
        assertThat(workload).hasSize(1);
        assertThat(workload.get(0).path("engineerId").asLong()).isEqualTo(ENGINEER_ONE);
    }

    @Test
    @DisplayName("客户角色无权访问统计")
    void customerCannotAccessDashboard() throws Exception {
        String customerToken = login("customer01", PASSWORD);

        JsonNode response = call(HttpMethod.GET, "/api/v1/dashboard/overview", customerToken, null, 403);

        assertThat(response.path("code").asInt()).isEqualTo(1101);
    }

    @Test
    @DisplayName("故障类型与优先级分布与数据库聚合一致")
    void distributionsMatchDatabase() throws Exception {
        String adminToken = login("admin", PASSWORD);

        JsonNode faults = call(HttpMethod.GET, "/api/v1/dashboard/fault-distribution", adminToken, null, 200)
                .path("data");
        Long hardwareCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM service_ticket WHERE fault_category_id = ?", Long.class, CATEGORY_HARDWARE);
        long hardwareFromApi = 0;
        for (JsonNode item : faults) {
            if (String.valueOf(CATEGORY_HARDWARE).equals(item.path("key").asText())) {
                hardwareFromApi = item.path("value").asLong();
                assertThat(item.path("name").asText()).isEqualTo("硬件故障");
            }
        }
        assertThat(hardwareFromApi).isEqualTo(hardwareCount);

        JsonNode priorities = call(HttpMethod.GET, "/api/v1/dashboard/priority-distribution", adminToken, null, 200)
                .path("data");
        Long urgentCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM service_ticket WHERE priority = 'URGENT'", Long.class);
        long urgentFromApi = 0;
        for (JsonNode item : priorities) {
            if ("URGENT".equals(item.path("key").asText())) {
                urgentFromApi = item.path("value").asLong();
                assertThat(item.path("name").asText()).isEqualTo("紧急");
            }
        }
        assertThat(urgentFromApi).isEqualTo(urgentCount);
    }

    @Test
    @DisplayName("工程师工作量统计包含已分派工程师并给出完成数")
    void engineerWorkloadIsConsistent() throws Exception {
        String adminToken = login("admin", PASSWORD);

        JsonNode workload = call(HttpMethod.GET, "/api/v1/dashboard/engineer-workload", adminToken, null, 200)
                .path("data");

        assertThat(workload.size()).isGreaterThanOrEqualTo(2);
        long sumTotal = 0;
        for (JsonNode item : workload) {
            sumTotal += item.path("total").asLong();
            assertThat(item.path("total").asLong())
                    .isEqualTo(item.path("completed").asLong() + item.path("open").asLong());
            assertThat(item.path("engineerName").asText()).isNotBlank();
        }
        Long assignedTotal = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM service_ticket WHERE assignee_id IS NOT NULL", Long.class);
        assertThat(sumTotal).isEqualTo(assignedTotal);
    }

    @Test
    @DisplayName("满意度统计在客户评价后反映真实评分")
    void satisfactionReflectsRealEvaluation() throws Exception {
        String serviceToken = login("service01", PASSWORD);
        String engineerToken = login("engineer01", PASSWORD);
        String customerToken = login("customer01", PASSWORD);
        long ticketId = createTicket(serviceToken, "HIGH");
        call(HttpMethod.POST, "/api/v1/tickets/" + ticketId + "/assign", serviceToken,
                Map.of("assigneeId", ENGINEER_ONE), 200);
        call(HttpMethod.POST, "/api/v1/tickets/" + ticketId + "/accept", engineerToken, null, 200);
        call(HttpMethod.POST, "/api/v1/tickets/" + ticketId + "/process", engineerToken,
                Map.of("content", "处理完成"), 200);
        call(HttpMethod.POST, "/api/v1/tickets/" + ticketId + "/complete", engineerToken,
                Map.of("result", "已修复"), 200);
        call(HttpMethod.POST, "/api/v1/tickets/" + ticketId + "/confirm", customerToken, Map.of(), 200);
        call(HttpMethod.POST, "/api/v1/tickets/" + ticketId + "/evaluation", customerToken,
                Map.of("score", 5, "content", "很满意"), 200);

        JsonNode satisfaction = call(HttpMethod.GET, "/api/v1/dashboard/satisfaction", serviceToken, null, 200)
                .path("data");

        Long expectedTotal = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM ticket_evaluation", Long.class);
        assertThat(satisfaction.path("total").asLong()).isEqualTo(expectedTotal);
        assertThat(satisfaction.path("avgScore").asDouble()).isBetween(1.0, 5.0);
        long distributionSum = 0;
        for (JsonNode item : satisfaction.path("distribution")) {
            distributionSum += item.path("value").asLong();
        }
        assertThat(distributionSum).isEqualTo(expectedTotal);
    }

    @Test
    @DisplayName("统计结果会命中缓存，写操作后缓存失效并反映新数据")
    void cacheIsHitAndInvalidatedOnWrite() throws Exception {
        String serviceToken = login("service01", PASSWORD);

        JsonNode first = call(HttpMethod.GET, "/api/v1/dashboard/overview", serviceToken, null, 200).path("data");
        assertThat(first.path("cached").asBoolean()).isFalse();
        long totalBefore = first.path("total").asLong();

        JsonNode second = call(HttpMethod.GET, "/api/v1/dashboard/overview", serviceToken, null, 200).path("data");
        assertThat(second.path("cached").asBoolean()).as("第二次应命中缓存").isTrue();
        assertThat(second.path("total").asLong()).isEqualTo(totalBefore);

        createTicket(serviceToken, "MEDIUM");

        JsonNode third = call(HttpMethod.GET, "/api/v1/dashboard/overview", serviceToken, null, 200).path("data");
        assertThat(third.path("cached").asBoolean()).as("写操作后缓存应已失效").isFalse();
        assertThat(third.path("total").asLong()).isEqualTo(totalBefore + 1);
    }

    @Test
    @DisplayName("趋势接口按指定区间返回数据点")
    void trendReturnsRequestedRange() throws Exception {
        String serviceToken = login("service01", PASSWORD);
        createTicket(serviceToken, "LOW");

        JsonNode points = call(HttpMethod.GET, "/api/v1/dashboard/trend?granularity=DAY", serviceToken, null, 200)
                .path("data");

        assertThat(points.size()).isEqualTo(30);
        JsonNode today = points.get(points.size() - 1);
        assertThat(today.path("created").asLong()).isGreaterThanOrEqualTo(1);
        assertThat(today.path("period").asText()).isEqualTo(java.time.LocalDate.now().toString());

        JsonNode monthly = call(HttpMethod.GET,
                "/api/v1/dashboard/trend?granularity=MONTH&startDate=" + java.time.LocalDate.now().withDayOfMonth(1)
                        + "&endDate=" + java.time.LocalDate.now(), serviceToken, null, 200).path("data");
        assertThat(monthly.size()).isEqualTo(1);
    }

    @Test
    @DisplayName("超期自动确认只处理超过阈值的待确认工单，并写入 SYSTEM 记录")
    void autoConfirmProcessesOnlyExpiredTickets() throws Exception {
        String serviceToken = login("service01", PASSWORD);
        long expiredId = createTicket(serviceToken, "MEDIUM");
        jdbcTemplate.update("UPDATE service_ticket SET status = 'PENDING_CONFIRM', finish_time = DATE_SUB(NOW(), "
                + "INTERVAL 100 HOUR), accept_time = DATE_SUB(NOW(), INTERVAL 120 HOUR) WHERE id = ?", expiredId);
        long freshId = createTicket(serviceToken, "MEDIUM");
        jdbcTemplate.update("UPDATE service_ticket SET status = 'PENDING_CONFIRM', finish_time = NOW() WHERE id = ?",
                freshId);

        int confirmed = scheduledService.autoConfirmExpiredTickets();

        assertThat(confirmed).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject("SELECT status FROM service_ticket WHERE id = ?", String.class,
                expiredId)).isEqualTo("COMPLETED");
        assertThat(jdbcTemplate.queryForObject("SELECT status FROM service_ticket WHERE id = ?", String.class,
                freshId)).isEqualTo("PENDING_CONFIRM");
        String operator = jdbcTemplate.queryForObject(
                "SELECT operator_name FROM ticket_process_record WHERE ticket_id = ? AND action = 'AUTO_CONFIRM'",
                String.class, expiredId);
        assertThat(operator).isEqualTo("SYSTEM");
    }
}

package com.serviceflow.integration;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;

/**
 * 评价、评论与附件集成测试：真实 MySQL + 本地文件存储。
 */
class TicketFeedbackIntegrationTest extends BaseIntegrationTest {

    private static final long ENGINEER_ONE = 3L;
    private static final long CUSTOMER_ONE = 1L;
    private static final long CUSTOMER_TWO = 2L;
    private static final long DEVICE_OF_CUSTOMER_ONE = 1L;
    private static final long DEVICE_OF_CUSTOMER_TWO = 4L;
    private static final long CATEGORY_HARDWARE = 1L;

    private Map<String, Object> ticketBody(long customerId, long deviceId) {
        Map<String, Object> body = new HashMap<>();
        body.put("title", "反馈模块集成测试工单");
        body.put("description", "用于验证评价、评论与附件");
        body.put("customerId", customerId);
        body.put("deviceId", deviceId);
        body.put("faultCategoryId", CATEGORY_HARDWARE);
        body.put("priority", "MEDIUM");
        return body;
    }

    private long createTicket(String token, long customerId, long deviceId) throws Exception {
        return call(HttpMethod.POST, "/api/v1/tickets", token, ticketBody(customerId, deviceId), 200)
                .path("data").path("id").asLong();
    }

    /**
     * 推进工单到"已完成"，返回工单 ID。
     */
    private long completeTicket(String serviceToken, String engineerToken, String customerToken,
                                long customerId, long deviceId) throws Exception {
        long ticketId = createTicket(serviceToken, customerId, deviceId);
        call(HttpMethod.POST, "/api/v1/tickets/" + ticketId + "/assign", serviceToken,
                Map.of("assigneeId", ENGINEER_ONE), 200);
        call(HttpMethod.POST, "/api/v1/tickets/" + ticketId + "/accept", engineerToken, null, 200);
        call(HttpMethod.POST, "/api/v1/tickets/" + ticketId + "/process", engineerToken,
                Map.of("content", "处理完成"), 200);
        call(HttpMethod.POST, "/api/v1/tickets/" + ticketId + "/complete", engineerToken,
                Map.of("result", "已修复"), 200);
        call(HttpMethod.POST, "/api/v1/tickets/" + ticketId + "/confirm", customerToken, Map.of(), 200);
        return ticketId;
    }

    private MvcResult uploadRaw(long ticketId, String token, String fileName, String contentType,
                                byte[] content, String bizType, int expectedStatus) throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", fileName, contentType, content);
        var request = multipart("/api/v1/tickets/" + ticketId + "/attachments").file(file);
        if (bizType != null) {
            request.param("bizType", bizType);
        }
        if (token != null) {
            request.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        }
        MvcResult result = mockMvc.perform(request).andReturn();
        assertThat(result.getResponse().getStatus()).isEqualTo(expectedStatus);
        return result;
    }

    private String sha256Of(byte[] content) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        StringBuilder builder = new StringBuilder();
        for (byte b : digest.digest(content)) {
            builder.append(String.format("%02x", b));
        }
        return builder.toString();
    }

    // ------------------------------------------------------------ 评价

    @Test
    @DisplayName("未完成工单不可评价，完成后可评价且不能重复评价")
    void evaluationRequiresCompletedTicketAndIsUnique() throws Exception {
        String serviceToken = login("service01", PASSWORD);
        String engineerToken = login("engineer01", PASSWORD);
        String customerToken = login("customer01", PASSWORD);

        long ticketId = createTicket(serviceToken, CUSTOMER_ONE, DEVICE_OF_CUSTOMER_ONE);
        JsonNode tooEarly = call(HttpMethod.POST, "/api/v1/tickets/" + ticketId + "/evaluation", customerToken,
                Map.of("score", 5), 409);
        assertThat(tooEarly.path("code").asInt()).isEqualTo(1501);

        call(HttpMethod.POST, "/api/v1/tickets/" + ticketId + "/assign", serviceToken,
                Map.of("assigneeId", ENGINEER_ONE), 200);
        call(HttpMethod.POST, "/api/v1/tickets/" + ticketId + "/accept", engineerToken, null, 200);
        call(HttpMethod.POST, "/api/v1/tickets/" + ticketId + "/process", engineerToken,
                Map.of("content", "处理完成"), 200);
        call(HttpMethod.POST, "/api/v1/tickets/" + ticketId + "/complete", engineerToken,
                Map.of("result", "已修复"), 200);

        JsonNode beforeConfirm = call(HttpMethod.POST, "/api/v1/tickets/" + ticketId + "/evaluation", customerToken,
                Map.of("score", 4), 409);
        assertThat(beforeConfirm.path("code").asInt()).isEqualTo(1501);

        call(HttpMethod.POST, "/api/v1/tickets/" + ticketId + "/confirm", customerToken, Map.of(), 200);
        JsonNode created = call(HttpMethod.POST, "/api/v1/tickets/" + ticketId + "/evaluation", customerToken,
                Map.of("score", 5, "content", "响应很快", "tags", "响应及时,一次修复",
                        "speedScore", 5, "qualityScore", 5, "attitudeScore", 4), 200);
        assertThat(created.path("data").path("score").asInt()).isEqualTo(5);
        assertThat(created.path("data").path("satisfaction").asText()).isEqualTo("VERY_SATISFIED");

        JsonNode duplicate = call(HttpMethod.POST, "/api/v1/tickets/" + ticketId + "/evaluation", customerToken,
                Map.of("score", 3), 409);
        assertThat(duplicate.path("code").asInt()).isEqualTo(1502);

        Integer evaluationRecords = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM ticket_process_record WHERE ticket_id = ? AND action = 'EVALUATE'",
                Integer.class, ticketId);
        assertThat(evaluationRecords).isEqualTo(1);
    }

    @Test
    @DisplayName("其他客户与被分派工程师不能评价工单")
    void evaluationIsRestrictedToOwningCustomer() throws Exception {
        String serviceToken = login("service01", PASSWORD);
        String engineerToken = login("engineer01", PASSWORD);
        String customerToken = login("customer01", PASSWORD);
        long ticketId = completeTicket(serviceToken, engineerToken, customerToken,
                CUSTOMER_ONE, DEVICE_OF_CUSTOMER_ONE);

        JsonNode otherCustomer = call(HttpMethod.POST, "/api/v1/tickets/" + ticketId + "/evaluation",
                login("customer02", PASSWORD), Map.of("score", 5), 403);
        assertThat(otherCustomer.path("code").asInt()).isEqualTo(1102);

        JsonNode engineer = call(HttpMethod.POST, "/api/v1/tickets/" + ticketId + "/evaluation", engineerToken,
                Map.of("score", 5), 403);
        assertThat(engineer.path("code").asInt()).isEqualTo(1101);
    }

    @Test
    @DisplayName("匿名评价对客服隐藏内容，评价人本人仍可见")
    void anonymousEvaluationHidesContentFromOthers() throws Exception {
        String serviceToken = login("service01", PASSWORD);
        String engineerToken = login("engineer01", PASSWORD);
        String customerToken = login("customer01", PASSWORD);
        long ticketId = completeTicket(serviceToken, engineerToken, customerToken,
                CUSTOMER_ONE, DEVICE_OF_CUSTOMER_ONE);

        call(HttpMethod.POST, "/api/v1/tickets/" + ticketId + "/evaluation", customerToken,
                Map.of("score", 2, "content", "等待时间偏长", "tags", "响应慢", "anonymous", true), 200);

        JsonNode asService = call(HttpMethod.GET, "/api/v1/tickets/" + ticketId + "/evaluation", serviceToken,
                null, 200);
        assertThat(asService.path("data").path("anonymous").asBoolean()).isTrue();
        assertThat(asService.path("data").path("content").isNull()
                || asService.path("data").path("content").isMissingNode()).isTrue();
        assertThat(asService.path("data").path("customerId").isNull()
                || asService.path("data").path("customerId").isMissingNode()).isTrue();

        JsonNode asCustomer = call(HttpMethod.GET, "/api/v1/tickets/" + ticketId + "/evaluation", customerToken,
                null, 200);
        assertThat(asCustomer.path("data").path("content").asText()).isEqualTo("等待时间偏长");
    }

    // ------------------------------------------------------------ 评论

    @Test
    @DisplayName("内部备注仅员工可见，客户评论强制为公开")
    void internalCommentsAreHiddenFromCustomer() throws Exception {
        String serviceToken = login("service01", PASSWORD);
        String customerToken = login("customer01", PASSWORD);
        long ticketId = createTicket(serviceToken, CUSTOMER_ONE, DEVICE_OF_CUSTOMER_ONE);

        call(HttpMethod.POST, "/api/v1/tickets/" + ticketId + "/comments", serviceToken,
                Map.of("content", "内部备注：该客户历史返修率较高", "internal", true), 200);
        call(HttpMethod.POST, "/api/v1/tickets/" + ticketId + "/comments", serviceToken,
                Map.of("content", "已安排工程师上门", "internal", false), 200);
        JsonNode customerComment = call(HttpMethod.POST, "/api/v1/tickets/" + ticketId + "/comments", customerToken,
                Map.of("content", "请问几点上门？", "internal", true), 200);
        assertThat(customerComment.path("data").path("internal").asBoolean()).isFalse();

        JsonNode asCustomer = call(HttpMethod.GET, "/api/v1/tickets/" + ticketId + "/comments", customerToken,
                null, 200);
        assertThat(asCustomer.path("data")).hasSize(2);
        asCustomer.path("data").forEach(node ->
                assertThat(node.path("internal").asBoolean()).isFalse());
        asCustomer.path("data").forEach(node ->
                assertThat(node.path("content").asText()).doesNotContain("历史返修率"));

        JsonNode asService = call(HttpMethod.GET, "/api/v1/tickets/" + ticketId + "/comments", serviceToken,
                null, 200);
        assertThat(asService.path("data")).hasSize(3);

        JsonNode detailAsCustomer = call(HttpMethod.GET, "/api/v1/tickets/" + ticketId, customerToken, null, 200);
        assertThat(detailAsCustomer.path("data").path("commentCount").asLong()).isEqualTo(2);
        JsonNode detailAsService = call(HttpMethod.GET, "/api/v1/tickets/" + ticketId, serviceToken, null, 200);
        assertThat(detailAsService.path("data").path("commentCount").asLong()).isEqualTo(3);
    }

    // ------------------------------------------------------------ 附件

    @Test
    @DisplayName("上传附件成功：文件名清洗、SHA-256 落库、可通过列表与下载取回")
    void uploadAndDownloadAttachment() throws Exception {
        String serviceToken = login("service01", PASSWORD);
        long ticketId = createTicket(serviceToken, CUSTOMER_ONE, DEVICE_OF_CUSTOMER_ONE);
        byte[] content = "故障现场照片内容".getBytes(StandardCharsets.UTF_8);

        MvcResult uploaded = uploadRaw(ticketId, serviceToken, "../../故障 照片.png", "image/png",
                content, "FAULT_PHOTO", 200);
        JsonNode body = body(uploaded);
        long attachmentId = body.path("data").path("id").asLong();
        assertThat(body.path("data").path("fileName").asText()).isEqualTo("故障 照片.png");
        assertThat(body.path("data").path("bizType").asText()).isEqualTo("FAULT_PHOTO");
        assertThat(body.path("data").path("downloadUrl").asText()).contains("/attachments/" + attachmentId);

        String sha256 = jdbcTemplate.queryForObject(
                "SELECT sha256 FROM ticket_attachment WHERE id = ?", String.class, attachmentId);
        assertThat(sha256).isEqualTo(sha256Of(content));
        String storedPath = jdbcTemplate.queryForObject(
                "SELECT file_path FROM ticket_attachment WHERE id = ?", String.class, attachmentId);
        assertThat(storedPath).doesNotContain("..").doesNotContain("故障");

        JsonNode list = call(HttpMethod.GET, "/api/v1/tickets/" + ticketId + "/attachments", serviceToken,
                null, 200);
        assertThat(list.path("data")).hasSize(1);

        MvcResult downloaded = mockMvc.perform(get("/api/v1/tickets/" + ticketId + "/attachments/"
                        + attachmentId + "/download")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + serviceToken))
                .andReturn();
        assertThat(downloaded.getResponse().getStatus()).isEqualTo(200);
        assertThat(downloaded.getResponse().getContentAsByteArray()).isEqualTo(content);
        assertThat(downloaded.getResponse().getHeader(HttpHeaders.CONTENT_DISPOSITION))
                .contains("filename*=UTF-8''");
    }

    @Test
    @DisplayName("附件类型白名单与大小上限由系统配置控制")
    void attachmentTypeAndSizeLimitsAreEnforced() throws Exception {
        String serviceToken = login("service01", PASSWORD);
        long ticketId = createTicket(serviceToken, CUSTOMER_ONE, DEVICE_OF_CUSTOMER_ONE);

        JsonNode wrongType = body(uploadRaw(ticketId, serviceToken, "malware.exe", "application/octet-stream",
                "MZ".getBytes(StandardCharsets.UTF_8), "OTHER", 400));
        assertThat(wrongType.path("code").asInt()).isEqualTo(1503);

        String original = jdbcTemplate.queryForObject(
                "SELECT config_value FROM sys_config WHERE config_key = 'attachment.max_size_mb'", String.class);
        try {
            jdbcTemplate.update("UPDATE sys_config SET config_value = '1' WHERE config_key = 'attachment.max_size_mb'");
            byte[] big = new byte[1024 * 1024 + 1024];
            JsonNode tooLarge = body(uploadRaw(ticketId, serviceToken, "big.png", "image/png", big, "OTHER", 400));
            assertThat(tooLarge.path("code").asInt()).isEqualTo(1504);
            assertThat(tooLarge.path("message").asText()).contains("上限");
        } finally {
            jdbcTemplate.update(
                    "UPDATE sys_config SET config_value = ? WHERE config_key = 'attachment.max_size_mb'", original);
        }
    }

    @Test
    @DisplayName("附件访问受工单权限约束：他人工单不可下载，附件必须属于该工单")
    void attachmentAccessIsRestricted() throws Exception {
        String serviceToken = login("service01", PASSWORD);
        String customerToken = login("customer01", PASSWORD);
        long ownTicket = createTicket(serviceToken, CUSTOMER_ONE, DEVICE_OF_CUSTOMER_ONE);
        long otherTicket = createTicket(serviceToken, CUSTOMER_TWO, DEVICE_OF_CUSTOMER_TWO);

        MvcResult uploaded = uploadRaw(otherTicket, serviceToken, "private.png", "image/png",
                "x".getBytes(StandardCharsets.UTF_8), "OTHER", 200);
        long attachmentId = body(uploaded).path("data").path("id").asLong();

        JsonNode downloadDenied = call(HttpMethod.GET, "/api/v1/tickets/" + otherTicket + "/attachments/"
                + attachmentId + "/download", customerToken, null, 403);
        assertThat(downloadDenied.path("code").asInt()).isEqualTo(1102);

        JsonNode listDenied = call(HttpMethod.GET, "/api/v1/tickets/" + otherTicket + "/attachments",
                customerToken, null, 403);
        assertThat(listDenied.path("code").asInt()).isEqualTo(1102);

        // 附件 ID 与工单不匹配时返回 1505
        JsonNode mismatch = call(HttpMethod.GET, "/api/v1/tickets/" + ownTicket + "/attachments/"
                + attachmentId + "/download", serviceToken, null, 404);
        assertThat(mismatch.path("code").asInt()).isEqualTo(1505);
    }

    @Test
    @DisplayName("删除附件：工程师无权限，客服可删除且物理文件被清理")
    void deleteAttachmentChecksPermissionAndRemovesFile() throws Exception {
        String serviceToken = login("service01", PASSWORD);
        String engineerToken = login("engineer01", PASSWORD);
        long ticketId = createTicket(serviceToken, CUSTOMER_ONE, DEVICE_OF_CUSTOMER_ONE);
        MvcResult uploaded = uploadRaw(ticketId, serviceToken, "to-delete.png", "image/png",
                "y".getBytes(StandardCharsets.UTF_8), "OTHER", 200);
        long attachmentId = body(uploaded).path("data").path("id").asLong();
        String storedPath = jdbcTemplate.queryForObject(
                "SELECT file_path FROM ticket_attachment WHERE id = ?", String.class, attachmentId);
        java.nio.file.Path physical = java.nio.file.Paths.get("storage/attachments")
                .toAbsolutePath().normalize().resolve(storedPath).normalize();
        assertThat(java.nio.file.Files.exists(physical)).isTrue();

        JsonNode denied = call(HttpMethod.DELETE, "/api/v1/tickets/" + ticketId + "/attachments/"
                + attachmentId, engineerToken, null, 403);
        assertThat(denied.path("code").asInt()).isEqualTo(1101);

        call(HttpMethod.DELETE, "/api/v1/tickets/" + ticketId + "/attachments/" + attachmentId, serviceToken,
                null, 200);
        Integer remaining = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM ticket_attachment WHERE ticket_id = ? AND deleted = 0", Integer.class,
                ticketId);
        assertThat(remaining).isZero();
        assertThat(java.nio.file.Files.exists(physical)).isFalse();
    }
}

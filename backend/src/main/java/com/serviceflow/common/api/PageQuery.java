package com.serviceflow.common.api;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import java.util.Set;

/**
 * 通用分页查询参数。所有列表接口的分页/排序参数都应继承或复用该类。
 */
@Schema(description = "通用分页查询参数")
public class PageQuery {

    public static final int DEFAULT_PAGE_NUM = 1;
    public static final int DEFAULT_PAGE_SIZE = 10;
    public static final int MAX_PAGE_SIZE = 100;

    @Schema(description = "页码，从 1 开始", example = "1", defaultValue = "1")
    @Min(value = 1, message = "页码不能小于 1")
    private Integer pageNum = DEFAULT_PAGE_NUM;

    @Schema(description = "每页条数，最大 100", example = "10", defaultValue = "10")
    @Min(value = 1, message = "每页条数不能小于 1")
    @Max(value = MAX_PAGE_SIZE, message = "每页条数不能大于 100")
    private Integer pageSize = DEFAULT_PAGE_SIZE;

    @Schema(description = "排序字段（白名单校验）", example = "createTime")
    private String sortField;

    @Schema(description = "排序方向：asc / desc", example = "desc")
    private String sortOrder = "desc";

    public Integer getPageNum() {
        return pageNum == null ? DEFAULT_PAGE_NUM : pageNum;
    }

    public void setPageNum(Integer pageNum) {
        this.pageNum = pageNum;
    }

    public Integer getPageSize() {
        return pageSize == null ? DEFAULT_PAGE_SIZE : pageSize;
    }

    public void setPageSize(Integer pageSize) {
        this.pageSize = pageSize;
    }

    public String getSortField() {
        return sortField;
    }

    public void setSortField(String sortField) {
        this.sortField = sortField;
    }

    public String getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(String sortOrder) {
        this.sortOrder = sortOrder;
    }

    /**
     * 校验排序字段是否在白名单内，防止 SQL 注入。
     *
     * @param allowedFields 允许排序的字段名（Java 属性名或数据库列名）
     * @return 校验通过返回 true
     */
    public boolean isSortFieldAllowed(Set<String> allowedFields) {
        return sortField == null || sortField.isBlank() || allowedFields.contains(sortField);
    }

    /**
     * 归一化排序方向，非 asc 一律按 desc 处理。
     */
    public String normalizedSortOrder() {
        return "asc".equalsIgnoreCase(sortOrder) ? "asc" : "desc";
    }
}

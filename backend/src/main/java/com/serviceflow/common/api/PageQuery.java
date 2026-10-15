package com.serviceflow.common.api;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import com.baomidou.mybatisplus.core.metadata.OrderItem;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.serviceflow.common.enums.ErrorCode;
import com.serviceflow.common.exception.BusinessException;

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
     * 子类声明允许排序的字段白名单，默认不允许任何字段排序。
     *
     * @return 允许的排序字段集合（Java 属性名）
     */
    public Set<String> allowedSortFields() {
        return Set.of();
    }

    /**
     * 校验排序字段是否在白名单内，防止通过排序参数注入 SQL。
     *
     * @return 校验通过返回 true
     */
    public boolean isSortFieldAllowed() {
        return sortField == null || sortField.isBlank() || allowedSortFields().contains(sortField);
    }

    /**
     * 把 Java 属性名转换为数据库列名（驼峰转下划线），用于排序。
     *
     * @param defaultField 未指定排序字段时使用的默认属性名
     * @return 数据库列名
     */
    public String resolvedSortColumn(String defaultField) {
        String field = (sortField == null || sortField.isBlank()) ? defaultField : sortField;
        return field.replaceAll("([a-z0-9])([A-Z])", "$1_$2").toLowerCase();
    }

    /**
     * 构造 MyBatis-Plus 分页对象，并应用经过白名单校验的排序字段。
     *
     * @param defaultSortField 未指定排序字段时使用的默认属性名
     * @param <T>              实体类型
     * @return 分页对象
     */
    public <T> Page<T> toPage(String defaultSortField) {
        if (!isSortFieldAllowed()) {
            throw BusinessException.of(ErrorCode.SORT_FIELD_INVALID);
        }
        Page<T> page = new Page<>(getPageNum(), getPageSize());
        String column = resolvedSortColumn(defaultSortField);
        page.addOrder("asc".equals(normalizedSortOrder()) ? OrderItem.asc(column) : OrderItem.desc(column));
        return page;
    }

    /**
     * 归一化排序方向，非 asc 一律按 desc 处理。
     */
    public String normalizedSortOrder() {
        return "asc".equalsIgnoreCase(sortOrder) ? "asc" : "desc";
    }
}

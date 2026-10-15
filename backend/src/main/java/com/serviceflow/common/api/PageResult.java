package com.serviceflow.common.api;

import com.baomidou.mybatisplus.core.metadata.IPage;

import java.util.Collections;
import java.util.List;
import java.util.function.Function;

/**
 * 统一分页响应结构。
 *
 * @param <T> 列表元素类型
 */
public class PageResult<T> {

    private final List<T> records;
    private final long total;
    private final long pageNum;
    private final long pageSize;
    private final long pages;

    public PageResult(List<T> records, long total, long pageNum, long pageSize) {
        this.records = records == null ? Collections.emptyList() : records;
        this.total = total;
        this.pageNum = pageNum;
        this.pageSize = pageSize;
        this.pages = pageSize <= 0 ? 0 : (total + pageSize - 1) / pageSize;
    }

    public static <T> PageResult<T> empty(int pageNum, int pageSize) {
        return new PageResult<>(Collections.emptyList(), 0L, pageNum, pageSize);
    }

    /**
     * 将 MyBatis-Plus 分页结果映射为统一响应结构。
     *
     * @param page    分页查询结果
     * @param mapper  实体到 VO 的转换函数
     */
    public static <E, T> PageResult<T> of(IPage<E> page, Function<E, T> mapper) {
        List<T> records = page.getRecords().stream().map(mapper).toList();
        return new PageResult<>(records, page.getTotal(), page.getCurrent(), page.getSize());
    }

    public List<T> getRecords() {
        return records;
    }

    public long getTotal() {
        return total;
    }

    public long getPageNum() {
        return pageNum;
    }

    public long getPageSize() {
        return pageSize;
    }

    public long getPages() {
        return pages;
    }
}

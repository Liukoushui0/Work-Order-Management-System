package com.serviceflow.common.api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class PageQueryTest {

    @Test
    @DisplayName("未传分页参数时使用默认值")
    void shouldUseDefaults() {
        PageQuery query = new PageQuery();

        assertThat(query.getPageNum()).isEqualTo(1);
        assertThat(query.getPageSize()).isEqualTo(10);
        assertThat(query.normalizedSortOrder()).isEqualTo("desc");
    }

    @Test
    @DisplayName("排序字段白名单校验可拦截非法字段")
    void shouldValidateSortFieldWhitelist() {
        PageQuery query = new PageQuery();
        Set<String> allowed = Set.of("createTime", "priority");

        assertThat(query.isSortFieldAllowed(allowed)).isTrue();

        query.setSortField("create_time; drop table sys_user");
        assertThat(query.isSortFieldAllowed(allowed)).isFalse();

        query.setSortField("priority");
        assertThat(query.isSortFieldAllowed(allowed)).isTrue();
    }

    @Test
    @DisplayName("排序方向仅 asc 生效，其余按 desc 处理")
    void shouldNormalizeSortOrder() {
        PageQuery query = new PageQuery();

        query.setSortOrder("ASC");
        assertThat(query.normalizedSortOrder()).isEqualTo("asc");

        query.setSortOrder("drop table");
        assertThat(query.normalizedSortOrder()).isEqualTo("desc");
    }
}

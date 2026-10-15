package com.serviceflow.common.api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

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
        UserQueryLike query = new UserQueryLike();

        assertThat(query.isSortFieldAllowed()).isTrue();

        query.setSortField("create_time; drop table sys_user");
        assertThat(query.isSortFieldAllowed()).isFalse();

        query.setSortField("priority");
        assertThat(query.isSortFieldAllowed()).isFalse();

        query.setSortField("createTime");
        assertThat(query.isSortFieldAllowed()).isTrue();
        assertThat(query.resolvedSortColumn("id")).isEqualTo("create_time");
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

    /** 测试用子类：只允许按 createTime 排序。 */
    private static class UserQueryLike extends PageQuery {
        @Override
        public java.util.Set<String> allowedSortFields() {
            return java.util.Set.of("createTime");
        }
    }
}

package com.serviceflow.support;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;

/**
 * 单元测试辅助：初始化 MyBatis-Plus 的实体元数据缓存。
 *
 * <p>Lambda 条件构造器（Wrappers.lambdaQuery/lambdaUpdate）需要实体对应的 TableInfo，
 * 该信息通常由容器启动时的 Mapper 扫描建立。纯单元测试没有 MyBatis 上下文，
 * 因此需要手动初始化，否则会抛出 "can not find lambda cache for this entity"。</p>
 */
public final class MybatisTestSupport {

    private MybatisTestSupport() {
    }

    public static void initTableInfo(Class<?>... entityClasses) {
        MybatisConfiguration configuration = new MybatisConfiguration();
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(configuration, "");
        for (Class<?> entityClass : entityClasses) {
            TableInfoHelper.initTableInfo(assistant, entityClass);
        }
    }
}

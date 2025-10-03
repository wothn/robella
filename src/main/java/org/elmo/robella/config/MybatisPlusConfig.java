package org.elmo.robella.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.autoconfigure.ConfigurationCustomizer;
import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.type.JdbcType;
import org.elmo.robella.handler.ModelCapabilityTypeHandler;
import org.elmo.robella.handler.SQLiteOffsetDateTimeTypeHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

@Configuration
@EnableTransactionManagement
public class MybatisPlusConfig {

    @Value("${spring.datasource.driver-class-name:}")
    private String driverClassName;

    @Bean
    public ConfigurationCustomizer configurationCustomizer() {
        return configuration -> {
            // 注册ModelCapabilityTypeHandler来处理List<ModelCapability>的序列化和反序列化
            configuration.getTypeHandlerRegistry().register(List.class, JdbcType.VARCHAR, new ModelCapabilityTypeHandler());
            
            // 只在使用 SQLite 时注册 SQLiteOffsetDateTimeTypeHandler
            // PostgreSQL 的 TIMESTAMP WITH TIME ZONE 可以直接映射到 OffsetDateTime，不需要自定义 TypeHandler
            if (isSQLite()) {
                configuration.getTypeHandlerRegistry().register(OffsetDateTime.class, JdbcType.VARCHAR, new SQLiteOffsetDateTimeTypeHandler());
            }
        };
    }

    /**
     * 检查是否使用 SQLite 数据库
     */
    private boolean isSQLite() {
        return driverClassName != null && driverClassName.contains("sqlite");
    }

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        // 根据数据库类型添加对应的分页插件
        DbType dbType = isSQLite() ? DbType.SQLITE : DbType.POSTGRE_SQL;
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(dbType));
        return interceptor;
    }

    @Bean
    public MetaObjectHandler metaObjectHandler() {
        return new MetaObjectHandler() {
            @Override
            public void insertFill(MetaObject metaObject) {
                this.strictInsertFill(metaObject, "createdAt", OffsetDateTime.class, OffsetDateTime.now(ZoneOffset.UTC));
            }

            @Override
            public void updateFill(MetaObject metaObject) {
                this.strictUpdateFill(metaObject, "updatedAt", OffsetDateTime.class, OffsetDateTime.now(ZoneOffset.UTC));
            }
        };
    }
}
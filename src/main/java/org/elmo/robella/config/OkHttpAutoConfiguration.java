package org.elmo.robella.config;

import lombok.RequiredArgsConstructor;
import okhttp3.ConnectionPool;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * OkHttp自动配置类
 * 为OkHttpUtils提供必要的OkHttpClient bean
 */
@Configuration
@RequiredArgsConstructor
public class OkHttpAutoConfiguration {

    private final OkHttpConfig okHttpConfig;

    @Bean
    public OkHttpClient okHttpClient() {
        OkHttpClient.Builder builder = new OkHttpClient.Builder();

        // 配置连接池
        ConnectionPool connectionPool = new ConnectionPool(
                okHttpConfig.getConnectionPool().getMaxIdleConnections(),
                okHttpConfig.getConnectionPool().getKeepAliveDuration().getSeconds(),
                TimeUnit.SECONDS
        );
        builder.connectionPool(connectionPool);

        // 配置超时
        builder.connectTimeout(okHttpConfig.getTimeout().getConnect().toMillis(), TimeUnit.MILLISECONDS);
        builder.readTimeout(okHttpConfig.getTimeout().getRead().toMillis(), TimeUnit.MILLISECONDS);
        builder.writeTimeout(okHttpConfig.getTimeout().getWrite().toMillis(), TimeUnit.MILLISECONDS);
        builder.callTimeout(okHttpConfig.getTimeout().getCall().toMillis(), TimeUnit.MILLISECONDS);

        // 配置日志拦截器
        if (okHttpConfig.getBuffer().isEnableLogging()) {
            HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
            loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BASIC);
            builder.addInterceptor(loggingInterceptor);
        }

        return builder.build();
    }
}
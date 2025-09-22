package org.elmo.robella.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.elmo.robella.config.OkHttpConfig;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

/**
 * OkHttp工具类
 * 提供统一的HTTP客户端调用方法
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OkHttpUtils {

    private final OkHttpClient okHttpClient;
    private final ObjectMapper objectMapper;
    private final OkHttpConfig config;

    /**
     * 发送GET请求
     *
     * @param url 请求URL
     * @return 响应内容
     */
    public String get(String url) throws IOException {
        return executeCall(new Request.Builder()
                .url(url)
                .get()
                .build());
    }

    /**
     * 发送GET请求（带请求头）
     *
     * @param url     请求URL
     * @param headers 请求头
     * @return 响应内容
     */
    public String get(String url, Map<String, String> headers) throws IOException {
        Request.Builder builder = new Request.Builder().url(url);
        addHeaders(builder, headers);
        return executeCall(builder.get().build());
    }

    /**
     * 发送POST请求（JSON格式）
     *
     * @param url  请求URL
     * @param body 请求体
     * @return 响应内容
     */
    public String postJson(String url, Object body) throws IOException {
        String jsonBody = objectMapper.writeValueAsString(body);
        RequestBody requestBody = RequestBody.create(jsonBody, MediaType.get("application/json; charset=utf-8"));
        return executeCall(new Request.Builder()
                .url(url)
                .post(requestBody)
                .build());
    }

    /**
     * 发送POST请求（JSON格式，带请求头）
     *
     * @param url     请求URL
     * @param body    请求体
     * @param headers 请求头
     * @return 响应内容
     */
    public String postJson(String url, Object body, Map<String, String> headers) throws IOException {
        String jsonBody = objectMapper.writeValueAsString(body);
        RequestBody requestBody = RequestBody.create(jsonBody, MediaType.get("application/json; charset=utf-8"));
        Request.Builder builder = new Request.Builder().url(url);
        addHeaders(builder, headers);
        return executeCall(builder.post(requestBody).build());
    }

    /**
     * 发送POST请求（表单格式）
     *
     * @param url  请求URL
     * @param form 表单数据
     * @return 响应内容
     */
    public String postForm(String url, Map<String, String> form) throws IOException {
        FormBody.Builder formBuilder = new FormBody.Builder();
        form.forEach(formBuilder::add);
        return executeCall(new Request.Builder()
                .url(url)
                .post(formBuilder.build())
                .build());
    }

    /**
     * 发送POST请求（表单格式，带请求头）
     *
     * @param url     请求URL
     * @param form    表单数据
     * @param headers 请求头
     * @return 响应内容
     */
    public String postForm(String url, Map<String, String> form, Map<String, String> headers) throws IOException {
        FormBody.Builder formBuilder = new FormBody.Builder();
        form.forEach(formBuilder::add);
        Request.Builder builder = new Request.Builder().url(url);
        addHeaders(builder, headers);
        return executeCall(builder.post(formBuilder.build()).build());
    }

    /**
     * 发送PUT请求（JSON格式）
     *
     * @param url  请求URL
     * @param body 请求体
     * @return 响应内容
     */
    public String putJson(String url, Object body) throws IOException {
        String jsonBody = objectMapper.writeValueAsString(body);
        RequestBody requestBody = RequestBody.create(jsonBody, MediaType.get("application/json; charset=utf-8"));
        return executeCall(new Request.Builder()
                .url(url)
                .put(requestBody)
                .build());
    }

    /**
     * 发送PUT请求（JSON格式，带请求头）
     *
     * @param url     请求URL
     * @param body    请求体
     * @param headers 请求头
     * @return 响应内容
     */
    public String putJson(String url, Object body, Map<String, String> headers) throws IOException {
        String jsonBody = objectMapper.writeValueAsString(body);
        RequestBody requestBody = RequestBody.create(jsonBody, MediaType.get("application/json; charset=utf-8"));
        Request.Builder builder = new Request.Builder().url(url);
        addHeaders(builder, headers);
        return executeCall(builder.put(requestBody).build());
    }

    /**
     * 发送DELETE请求
     *
     * @param url 请求URL
     * @return 响应内容
     */
    public String delete(String url) throws IOException {
        return executeCall(new Request.Builder()
                .url(url)
                .delete()
                .build());
    }

    /**
     * 发送DELETE请求（带请求头）
     *
     * @param url     请求URL
     * @param headers 请求头
     * @return 响应内容
     */
    public String delete(String url, Map<String, String> headers) throws IOException {
        Request.Builder builder = new Request.Builder().url(url);
        addHeaders(builder, headers);
        return executeCall(builder.delete().build());
    }

    /**
     * 发送PATCH请求（JSON格式）
     *
     * @param url  请求URL
     * @param body 请求体
     * @return 响应内容
     */
    public String patchJson(String url, Object body) throws IOException {
        String jsonBody = objectMapper.writeValueAsString(body);
        RequestBody requestBody = RequestBody.create(jsonBody, MediaType.get("application/json; charset=utf-8"));
        return executeCall(new Request.Builder()
                .url(url)
                .patch(requestBody)
                .build());
    }

    /**
     * 发送PATCH请求（JSON格式，带请求头）
     *
     * @param url     请求URL
     * @param body    请求体
     * @param headers 请求头
     * @return 响应内容
     */
    public String patchJson(String url, Object body, Map<String, String> headers) throws IOException {
        String jsonBody = objectMapper.writeValueAsString(body);
        RequestBody requestBody = RequestBody.create(jsonBody, MediaType.get("application/json; charset=utf-8"));
        Request.Builder builder = new Request.Builder().url(url);
        addHeaders(builder, headers);
        return executeCall(builder.patch(requestBody).build());
    }

    /**
     * 获取OkHttpClient实例（用于流式响应等特殊场景）
     */
    public OkHttpClient getClient() {
        return okHttpClient;
    }

    /**
     * 发送POST请求获取流式响应（Server-Sent Events）
     *
     * @param url     请求URL
     * @param body    请求体
     * @param headers 请求头
     * @return 流式响应字符串流
     */
    public Stream<String> postStream(String url, Object body, Map<String, String> headers) throws IOException {
        String jsonBody = objectMapper.writeValueAsString(body);
        RequestBody requestBody = RequestBody.create(jsonBody, MediaType.get("application/json; charset=utf-8"));

        Request.Builder builder = new Request.Builder()
                .url(url)
                .post(requestBody);

        if (headers != null) {
            headers.forEach(builder::addHeader);
        }

        Request request = builder.build();

        if (config.getBuffer().isEnableLogging()) {
            log.debug("Sending stream request to: {}", request.url());
            log.debug("Request headers: {}", request.headers());
        }

        // 创建自定义超时的OkHttpClient用于流式请求
        OkHttpClient streamClient = okHttpClient.newBuilder()
                .readTimeout(config.getTimeout().getRead().multipliedBy(5).toMillis(), TimeUnit.MILLISECONDS)
                .build();

        Response response = null;
        BufferedReader reader = null;
        
        try {
            response = streamClient.newCall(request).execute();

            if (!response.isSuccessful()) {
                String errorBody = response.body() != null ? response.body().string() : "No error body";
                throw new IOException("HTTP " + response.code() + ": " + errorBody);
            }

            if (response.body() == null) {
                throw new IOException("Response body is null");
            }

            // 明确指定UTF-8编码
            reader = new BufferedReader(new InputStreamReader(response.body().byteStream(), StandardCharsets.UTF_8));
            
            // 为了确保资源正确关闭，需要保存引用
            final Response finalResponse = response;
            final BufferedReader finalReader = reader;

            AtomicBoolean first = new AtomicBoolean(true);
            return reader.lines()
                    .peek(line -> {
                        if (first.compareAndSet(true, false)) {
                            log.info("First line actually arrived at {}", System.currentTimeMillis());
                        }
                    })
                    .onClose(() -> {
                        try {
                            finalReader.close();
                        } catch (IOException e) {
                            log.warn("Error closing reader: {}", e.getMessage());
                        }
                        try {
                            finalResponse.close();
                        } catch (Exception e) {
                            log.warn("Error closing response: {}", e.getMessage());
                        }
                    })
                    // 处理SSE格式，过滤空行和注释行
                    .filter(line -> line != null && !line.trim().isEmpty() && !line.trim().startsWith(":"));
        } catch (IOException e) {
            if (response != null) {
                try {
                    response.close();
                } catch (Exception ex) {
                    log.warn("Error closing response in exception handler: {}", ex.getMessage());
                }
            }
            throw e;
        }
    }

    /**
     * 执行HTTP请求（带重试机制）
     */
    private String executeCall(Request request) throws IOException {
        int attempt = 0;
        IOException lastException = null;

        while (attempt < config.getRetry().getMaxAttempts()) {
            try {
                if (config.getBuffer().isEnableLogging()) {
                    log.debug("Sending request to: {}", request.url());
                    log.debug("Request headers: {}", request.headers());
                    if (request.body() != null) {
                        log.debug("Request body: {}", request.body());
                    }
                }

                try (Response response = okHttpClient.newCall(request).execute()) {
                    String responseBody = Objects.requireNonNull(response.body()).string();

                    if (!response.isSuccessful()) {
                        log.warn("HTTP request failed with status: {}, response: {}", response.code(), responseBody);
                        throw new IOException("HTTP " + response.code() + ": " + responseBody);
                    }

                    if (config.getBuffer().isEnableLogging()) {
                        log.debug("Response received from: {}", request.url());
                        log.debug("Response headers: {}", response.headers());
                        log.debug("Response body: {}", responseBody.length() > 1000 ?
                                responseBody.substring(0, 1000) + "..." : responseBody);
                    }

                    return responseBody;
                }
            } catch (IOException e) {
                lastException = e;
                attempt++;

                if (attempt < config.getRetry().getMaxAttempts()) {
                    long delayMs = calculateDelay(attempt);
                    log.warn("Request failed (attempt {}/{}), retrying in {}ms: {}",
                            attempt, config.getRetry().getMaxAttempts(), delayMs, e.getMessage());

                    try {
                        Thread.sleep(delayMs);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new IOException("Request interrupted", ie);
                    }
                } else {
                    log.error("Request failed after {} attempts: {}", config.getRetry().getMaxAttempts(), e.getMessage());
                }
            }
        }

        throw lastException;
    }

    /**
     * 计算重试延迟时间
     */
    private long calculateDelay(int attempt) {
        long initialDelay = config.getRetry().getInitialDelay().toMillis();
        long maxDelay = config.getRetry().getMaxDelay().toMillis();

        // 指数退避算法
        long delay = initialDelay * (long) Math.pow(2, attempt - 1);
        return Math.min(delay, maxDelay);
    }

    /**
     * 添加请求头
     */
    private void addHeaders(Request.Builder builder, Map<String, String> headers) {
        if (headers != null) {
            headers.forEach(builder::addHeader);
        }
    }
}
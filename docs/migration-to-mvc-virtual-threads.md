# Robella 项目迁移计划：从 Spring WebFlux 迁移到 Spring Boot MVC + 虚拟线程

## 1. 概述

本文档详细说明了将 Robella AI API 网关项目从 Spring WebFlux + R2DBC 迁移到 Spring Boot MVC + 虚拟线程 + 传统 JDBC 的完整计划。

### 1.1 当前架构
- **Web框架**: Spring WebFlux (响应式)
- **数据库**: R2DBC (响应式数据库访问)
- **并发模型**: Project Reactor (Mono/Flux)
- **HTTP客户端**: WebClient (响应式)

### 1.2 目标架构
- **Web框架**: Spring Boot MVC (阻塞式)
- **数据库**: MyBatis-Plus (传统阻塞式)
- **并发模型**: 虚拟线程 (Java 21+)
- **HTTP客户端**: OkHttp (阻塞式)

## 2. 迁移优势

### 2.1 性能优势
- **简化开发**: 阻塞式编程模型更直观，减少学习曲线
- **线程效率**: 虚拟线程提供接近 WebFlux 的吞吐量，同时保持简单性
- **调试友好**: 传统调用栈更容易调试和排查问题

### 2.2 维护优势
- **更广泛的生态**: 更多的库和工具支持传统 MVC
- **人才储备**: 更容易找到熟悉 Spring MVC 的开发人员
- **代码可读性**: 阻塞式代码通常更易理解和维护

## 3. 迁移策略

### 3.1 分阶段迁移
采用渐进式迁移策略，确保系统在迁移过程中保持可用性：

1. **基础设施层**: 数据库访问层
2. **服务层**: 业务逻辑层
3. **控制层**: API 接口层
4. **客户端层**: 外部服务调用

### 3.2 风险控制
- no，破釜沉舟

## 4. 详细迁移步骤

### 4.1 第一阶段：依赖和配置修改

#### 4.1.1 修改 pom.xml
```xml
<!-- 移除 WebFlux 依赖 -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-webflux</artifactId>
</dependency>

<!-- 添加 Spring Web MVC 依赖 -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>

<!-- 移除 R2DBC 依赖 -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-r2dbc</artifactId>
</dependency>
<dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>r2dbc-postgresql</artifactId>
</dependency>
<dependency>
    <groupId>io.r2dbc</groupId>
    <artifactId>r2dbc-pool</artifactId>
</dependency>

<!-- 添加 MyBatis-Plus 依赖 -->
<dependency>
    <groupId>com.baomidou</groupId>
    <artifactId>mybatis-plus-boot-starter</artifactId>
    <version>3.5.5</version>
</dependency>
<dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>postgresql</artifactId>
</dependency>

<!-- 移除 Reactor Test 依赖 -->
<dependency>
    <groupId>io.projectreactor</groupId>
    <artifactId>reactor-test</artifactId>
    <scope>test</scope>
</dependency>

<!-- 添加 OkHttp 客户端 -->
<dependency>
    <groupId>com.squareup.okhttp3</groupId>
    <artifactId>okhttp</artifactId>
    <version>4.12.0</version>
</dependency>
<dependency>
    <groupId>com.squareup.okhttp3</groupId>
    <artifactId>logging-interceptor</artifactId>
    <version>4.12.0</version>
</dependency>
```

#### 4.1.2 修改 application.yml
```yaml
server:
  port: 10032

spring:
  application:
    name: robella
  # 启用虚拟线程
  threads:
    virtual:
      enabled: true

  # MyBatis-Plus 配置
  datasource:
    url: jdbc:postgresql://localhost:5432/robella
    username: ${POSTGRES_USERNAME:postgres}
    password: ${POSTGRES_PASSWORD:password}
    driver-class-name: org.postgresql.Driver
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      idle-timeout: 30000
      max-lifetime: 60000
      connection-timeout: 30000
      connection-test-query: SELECT 1

  # MyBatis-Plus 配置
  mybatis-plus:
    configuration:
      map-underscore-to-camel-case: true
      log-impl: org.apache.ibatis.logging.stdout.StdOutImpl
    global-config:
      db-config:
        id-type: auto
        logic-delete-field: deleted
        logic-delete-value: 1
        logic-not-delete-value: 0
    mapper-locations: classpath*:/mapper/**/*.xml

  # 移除 WebFlux 相关配置
  # webflux:
  #   multipart:
  #     max-in-memory-size: 32MB

  # 文件上传配置 (MVC)
  servlet:
    multipart:
      max-file-size: 256MB
      max-request-size: 256MB

  # Jackson 配置
  jackson:
    default-property-inclusion: non_null
    serialization:
      indent-output: false

# 移除 R2DBC 配置，保留其他配置
robella:
  # 其他配置保持不变...
```

### 4.2 第二阶段：数据库访问层迁移

#### 4.2.1 Repository 接口改造
所有 Repository 接口需要从 `R2dbcRepository` 改为 `BaseMapper`：



#### 4.2.2 Entity 类注解修改
所有实体类需要添加 MyBatis-Plus 注解：

**示例改造：**
```java
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("models")
public class Model {
    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("model_key")
    private String modelKey;

    private String name;
    private String description;
    private String organization;
    private Boolean published = false;

    @TableField("created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @TableField("updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    @TableField(fill = FieldFill.UPDATE)
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
```

#### 4.2.3 创建 MyBatis-Plus 配置类
```java
import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@EnableTransactionManagement
public class MybatisPlusConfig {

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        // 添加分页插件
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.POSTGRESQL));
        return interceptor;
    }
}
```

### 4.3 第三阶段：服务层迁移

#### 4.3.1 RoutingService 改造
**当前 RoutingService.java (简化版):**
```java
@Service
public class RoutingService {
    public Mono<List<VendorModel>> selectAllEnabledVendors(String modelKey) {
        return modelRepository.findByModelKey(modelKey)
            .flatMapMany(model -> vendorModelRepository.findByModelIdAndEnabledTrue(model.getId()))
            .collectList();
    }

    public Mono<ClientWithProvider> routeAndClient(String clientModelKey) {
        return selectVendorWithLoadBalancing(clientModelKey)
            .flatMap(vendorModel -> providerService.findById(vendorModel.getProviderId())
                .map(provider -> {
                    ApiClient client = clientFactory.getClient(provider.getEndpointType());
                    return new ClientWithProvider(client, provider, vendorModel);
                })
            );
    }
}
```

**改造后 RoutingService.java:**
```java
@Service
public class RoutingService {
    @Autowired
    private ModelMapper modelMapper;
    @Autowired
    private VendorModelMapper vendorModelMapper;

    public List<VendorModel> selectAllEnabledVendors(String modelKey) {
        Model model = modelMapper.findByModelKey(modelKey);
        if (model == null) {
            return Collections.emptyList();
        }
        return vendorModelMapper.findByModelIdAndEnabledTrue(model.getId());
    }

    public Optional<ClientWithProvider> routeAndClient(String clientModelKey) {
        return selectVendorWithLoadBalancing(clientModelKey)
            .flatMap(vendorModel -> providerService.findById(vendorModel.getProviderId())
                .map(provider -> {
                    ApiClient client = clientFactory.getClient(provider.getEndpointType());
                    return new ClientWithProvider(client, provider, vendorModel);
                })
            );
    }

    public Optional<VendorModel> selectVendorWithLoadBalancing(String modelKey) {
        List<VendorModel> candidates = selectAllEnabledVendors(modelKey);
        if (candidates.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(loadBalancer.select(candidates));
    }
}
```

#### 4.3.2 UnifiedService 改造
**当前 UnifiedService.java (简化版):**
```java
@Service
public class UnifiedService {
    public Mono<ModelListResponse> listModels() {
        return modelRepository.findByPublishedTrue()
            .map(model -> {
                ModelInfo modelInfo = new ModelInfo();
                modelInfo.setId(model.getModelKey());
                modelInfo.setObject("model");
                modelInfo.setOwnedBy(model.getOrganization() != null ? model.getOrganization() : "robella");
                return modelInfo;
            })
            .collectList()
            .map(modelInfos -> {
                ModelListResponse response = new ModelListResponse();
                response.setObject("list");
                response.setData(modelInfos);
                return response;
            });
    }

    public Mono<UnifiedChatResponse> sendChatRequestWithClient(UnifiedChatRequest request, RoutingService.ClientWithProvider clientWithProvider) {
        return clientWithProvider.getClient().chatCompletion(request, clientWithProvider.getProvider());
    }

    public Flux<UnifiedStreamChunk> sendStreamRequestWithClient(UnifiedChatRequest request, RoutingService.ClientWithProvider clientWithProvider) {
        return clientWithProvider.getClient().streamChatCompletion(request, clientWithProvider.getProvider())
                .filter(Objects::nonNull);
    }
}
```

**改造后 UnifiedService.java:**
```java
@Service
public class UnifiedService {
    @Autowired
    private ModelMapper modelMapper;

    public ModelListResponse listModels() {
        List<Model> models = modelMapper.findByPublishedTrue();
        List<ModelInfo> modelInfos = models.stream()
            .map(model -> {
                ModelInfo modelInfo = new ModelInfo();
                modelInfo.setId(model.getModelKey());
                modelInfo.setObject("model");
                modelInfo.setOwnedBy(model.getOrganization() != null ? model.getOrganization() : "robella");
                return modelInfo;
            })
            .collect(Collectors.toList());

        ModelListResponse response = new ModelListResponse();
        response.setObject("list");
        response.setData(modelInfos);
        return response;
    }

    public UnifiedChatResponse sendChatRequestWithClient(UnifiedChatRequest request, RoutingService.ClientWithProvider clientWithProvider) {
        return clientWithProvider.getClient().chatCompletion(request, clientWithProvider.getProvider());
    }

    public List<UnifiedStreamChunk> sendStreamRequestWithClient(UnifiedChatRequest request, RoutingService.ClientWithProvider clientWithProvider) {
        return clientWithProvider.getClient().streamChatCompletion(request, clientWithProvider.getProvider())
                .stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }
}
```

### 4.4 第四阶段：控制器层迁移

#### 4.4.1 OpenAIController 改造
**当前 OpenAIController.java (简化版):**
```java
@RestController
@RequestMapping("/v1")
public class OpenAIController {
    @PostMapping(value = "/chat/completions", produces = { MediaType.APPLICATION_JSON_VALUE, MediaType.TEXT_EVENT_STREAM_VALUE })
    public Mono<ResponseEntity<?>> chatCompletions(@RequestBody @Valid ChatCompletionRequest request) {
        String originalModelName = request.getModel();

        return Mono.deferContextual(ctx -> {
            String requestId = ctx.getOrDefault("requestId", UUID.randomUUID().toString());

            return routingService.routeAndClient(originalModelName)
                    .flatMap(clientWithProvider -> {
                        UnifiedChatRequest unifiedRequest = openAIEndpointTransform.endpointToUnifiedRequest(request);
                        unifiedRequest.setEndpointType("openai");
                        unifiedRequest.setModel(clientWithProvider.getVendorModel().getVendorModelKey());

                        if (Boolean.TRUE.equals(request.getStream())) {
                            Flux<String> sseStream = unifiedToOpenAIStreamTransformer.transform(
                                unifiedService.sendStreamRequestWithClient(unifiedRequest, clientWithProvider)
                                    .contextWrite(/* ... */),
                                requestId)
                                .mapNotNull(chunk -> JsonUtils.toJson(chunk))
                                .concatWith(Flux.just("[DONE]"));
                            return Mono.just(ResponseEntity.ok()
                                    .contentType(MediaType.TEXT_EVENT_STREAM)
                                    .body(sseStream)
                            );
                        } else {
                            return unifiedService.sendChatRequestWithClient(unifiedRequest, clientWithProvider)
                                    .contextWrite(/* ... */)
                                    .map(response -> ResponseEntity.ok().body(response));
                        }
                    });
        });
    }

    @GetMapping("/models")
    public Mono<ResponseEntity<ModelListResponse>> listModels() {
        return unifiedService.listModels()
                .map(response -> ResponseEntity.ok().body(response));
    }
}
```

**改造后 OpenAIController.java:**
```java
@RestController
@RequestMapping("/v1")
public class OpenAIController {
    @PostMapping(value = "/chat/completions", produces = { MediaType.APPLICATION_JSON_VALUE, MediaType.TEXT_EVENT_STREAM_VALUE })
    public ResponseEntity<?> chatCompletions(@RequestBody @Valid ChatCompletionRequest request) {
        String originalModelName = request.getModel();
        String requestId = UUID.randomUUID().toString();

        Optional<RoutingService.ClientWithProvider> clientWithProviderOpt = routingService.routeAndClient(originalModelName);

        if (clientWithProviderOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        RoutingService.ClientWithProvider clientWithProvider = clientWithProviderOpt.get();
        UnifiedChatRequest unifiedRequest = openAIEndpointTransform.endpointToUnifiedRequest(request);
        unifiedRequest.setEndpointType("openai");
        unifiedRequest.setModel(clientWithProvider.getVendorModel().getVendorModelKey());

        if (Boolean.TRUE.equals(request.getStream())) {
            // 流式响应处理需要特殊处理
            List<UnifiedStreamChunk> chunks = unifiedService.sendStreamRequestWithClient(unifiedRequest, clientWithProvider);
            Flux<String> sseStream = unifiedToOpenAIStreamTransformer.transform(chunks, requestId)
                    .mapNotNull(chunk -> JsonUtils.toJson(chunk))
                    .concatWith(Flux.just("[DONE]"));

            return ResponseEntity.ok()
                    .contentType(MediaType.TEXT_EVENT_STREAM)
                    .body(sseStream);
        } else {
            UnifiedChatResponse response = unifiedService.sendChatRequestWithClient(unifiedRequest, clientWithProvider);
            return ResponseEntity.ok().body(response);
        }
    }

    @GetMapping("/models")
    public ResponseEntity<ModelListResponse> listModels() {
        ModelListResponse response = unifiedService.listModels();
        return ResponseEntity.ok().body(response);
    }
}
```

### 4.5 第五阶段：客户端层迁移

#### 4.5.1 ApiClient 接口改造
**当前 ApiClient 接口 (概念性):**
```java
public interface ApiClient {
    Mono<UnifiedChatResponse> chatCompletion(UnifiedChatRequest request, Provider provider);
    Flux<UnifiedStreamChunk> streamChatCompletion(UnifiedChatRequest request, Provider provider);
}
```

**改造后 ApiClient 接口:**
```java
public interface ApiClient {
    UnifiedChatResponse chatCompletion(UnifiedChatRequest request, Provider provider);
    List<UnifiedStreamChunk> streamChatCompletion(UnifiedChatRequest request, Provider provider);
}
```

#### 4.5.2 HTTP 客户端迁移
创建基于 OkHttp 的 HTTP 客户端：

```java
@Service
public class OpenAIHttpClient implements ApiClient {
    private final OkHttpClient okHttpClient;

    public OpenAIHttpClient() {
        OkHttpClient.Builder builder = new OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS);

        // 添加日志拦截器（可选）
        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
        loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
        builder.addInterceptor(loggingInterceptor);

        this.okHttpClient = builder.build();
    }

    @Override
    public UnifiedChatResponse chatCompletion(UnifiedChatRequest request, Provider provider) {
        String url = provider.getBaseUrl() + "/chat/completions";

        OpenAIChatRequest openAIRequest = transformToOpenAIRequest(request);
        String jsonBody = JsonUtils.toJson(openAIRequest);

        RequestBody body = RequestBody.create(
            jsonBody,
            MediaType.get("application/json; charset=utf-8")
        );

        Request httpRequest = new Request.Builder()
            .url(url)
            .post(body)
            .addHeader("Authorization", "Bearer " + provider.getApiKey())
            .addHeader("Content-Type", "application/json")
            .build();

        try (Response response = okHttpClient.newCall(httpRequest).execute()) {
            if (!response.isSuccessful()) {
                throw new ExternalServiceException("HTTP error: " + response.code());
            }

            String responseBody = response.body().string();
            OpenAIChatResponse openAIResponse = JsonUtils.fromJson(responseBody, OpenAIChatResponse.class);
            return transformToUnifiedResponse(openAIResponse);
        } catch (IOException e) {
            throw new ExternalServiceException("Failed to call OpenAI API", e);
        }
    }

    @Override
    public List<UnifiedStreamChunk> streamChatCompletion(UnifiedChatRequest request, Provider provider) {
        String url = provider.getBaseUrl() + "/chat/completions";

        OpenAIChatRequest openAIRequest = transformToOpenAIRequest(request);
        openAIRequest.setStream(true);
        String jsonBody = JsonUtils.toJson(openAIRequest);

        RequestBody body = RequestBody.create(
            jsonBody,
            MediaType.get("application/json; charset=utf-8")
        );

        Request httpRequest = new Request.Builder()
            .url(url)
            .post(body)
            .addHeader("Authorization", "Bearer " + provider.getApiKey())
            .addHeader("Content-Type", "application/json")
            .build();

        List<UnifiedStreamChunk> chunks = new ArrayList<>();

        try (Response response = okHttpClient.newCall(httpRequest).execute()) {
            if (!response.isSuccessful()) {
                throw new ExternalServiceException("HTTP error: " + response.code());
            }

            InputStream inputStream = response.body().byteStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("data: ") && !line.equals("data: [DONE]")) {
                    String json = line.substring(6);
                    OpenAIChatChunk chunk = JsonUtils.fromJson(json, OpenAIChatChunk.class);
                    chunks.add(transformToUnifiedChunk(chunk));
                }
            }
        } catch (IOException e) {
            throw new ExternalServiceException("Failed to process streaming response", e);
        }

        return chunks;
    }
}
```

### 4.6 第六阶段：异常处理和过滤器

#### 4.6.1 全局异常处理
```java
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BaseBusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(BaseBusinessException ex) {
        ErrorResponse error = new ErrorResponse(ex.getErrorCode(), ex.getMessage());
        return ResponseEntity.status(ex.getHttpStatus()).body(error);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getAllErrors().stream()
            .map(DefaultMessageSourceResolvable::getDefaultMessage)
            .collect(Collectors.joining(", "));
        ErrorResponse error = new ErrorResponse("VALIDATION_ERROR", message);
        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
        ErrorResponse error = new ErrorResponse("INTERNAL_ERROR", "Internal server error");
        return ResponseEntity.internalServerError().body(error);
    }
}
```

#### 4.6.2 认证过滤器
```java
@Component
public class AuthenticationFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        String authHeader = httpRequest.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            try {
                // 验证 JWT token
                if (jwtService.validateToken(token)) {
                    String username = jwtService.extractUsername(token);
                    // 设置用户信息到请求属性
                    httpRequest.setAttribute("username", username);
                }
            } catch (Exception e) {
                // Token 无效，继续处理但不设置用户信息
            }
        }

        chain.doFilter(request, response);
    }
}
```

### 4.7 第七阶段：配置和工具类

#### 4.7.1 OkHttp 配置
```java
@Configuration
public class OkHttpConfig {

    @Bean
    public OkHttpClient okHttpClient() {
        OkHttpClient.Builder builder = new OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .connectionPool(new ConnectionPool(10, 5, TimeUnit.MINUTES))
            .retryOnConnectionFailure(true);

        // 添加日志拦截器
        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
        loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
        builder.addInterceptor(loggingInterceptor);

        // 添加重试拦截器
        builder.addInterceptor(new RetryInterceptor(3));

        return builder.build();
    }

    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        return mapper;
    }
}
```

#### 4.7.2 线程池配置
```java
@Configuration
public class ThreadConfig {

    @Bean
    public Executor virtualThreadExecutor() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }

    @Bean
    public ExecutorService virtualThreadExecutorService() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }
}
```

## 5. 完成的迁移步骤

### 5.1 WebClient 到 OkHttp 迁移 (已完成)

#### 5.1.1 依赖配置
- OkHttp 依赖已在 pom.xml 中添加 (版本 4.12.0)
- 创建了 `OkHttpProperties` 配置类
- 创建了 `OkHttpConfig` 配置类，支持连接池、超时、重试等配置

#### 5.1.2 服务层迁移
- 创建了 `OkHttpService` 服务，提供 HTTP 请求执行功能
- 支持同步请求和流式请求 (Server-Sent Events)
- 实现了响应式类型 (Mono/Flux) 到阻塞式调用的桥接

#### 5.1.3 客户端迁移
- `OpenAIClient`: 已从 WebClient 迁移到 OkHttp
- `AnthropicClient`: 已从 WebClient 迁移到 OkHttp
- `GitHubOAuthService`: 已从 WebClient 迁移到 OkHttp

#### 5.1.4 配置文件更新
- 更新了 `application.yml`，从 `robella.webclient` 改为 `robella.okhttp`
- 适配了 OkHttp 的配置项结构

### 5.1.5 保留的响应式接口
- `ApiClient` 接口仍返回 `Mono<UnifiedChatResponse>` 和 `Flux<UnifiedStreamChunk>`
- 使用 `OkHttpService` 在内部桥接阻塞式调用到响应式类型
- 这种设计允许逐步迁移，保持现有 API 兼容性

## 6. 测试策略

### 5.1 单元测试
- 所有 Mapper 层测试
- 服务层逻辑测试
- 工具类测试

### 5.2 集成测试
- MyBatis-Plus 集成测试
- OkHttp 客户端集成测试
- 完整 API 端到端测试

### 5.3 性能测试
- 虚拟线程性能对比测试
- OkHttp 连接池性能测试
- MyBatis-Plus 查询性能测试

## 6. 部署策略

### 6.1 蓝绿部署
1. 部署新版本到预生产环境
2. 进行全面的测试和验证
3. 切换流量到新版本
4. 保留旧版本以便快速回滚

### 6.2 灰度发布
1. 首先发布到内部测试环境
2. 逐步扩大用户范围
3. 监控性能指标和错误率
4. 全量发布

## 7. 风险评估

### 7.1 技术风险
- **性能回退**: 虚拟线程性能可能不如预期
- **内存泄漏**: 传统阻塞式编程可能导致内存泄漏
- **连接池问题**: 数据库连接池配置不当

### 7.2 业务风险
- **服务中断**: 迁移过程中可能出现服务不可用
- **数据丢失**: 数据迁移过程中可能出现数据一致性问题
- **兼容性问题**: API 兼容性可能受到影响

### 7.3 风险缓解
- 完整的备份策略
- 详细的测试计划
- 快速回滚机制
- 监控和告警系统

## 8. 时间线

### 8.1 准备阶段 (1-2 周)
- 环境搭建
- 依赖分析
- 测试准备

### 8.2 开发阶段 (4-6 周)
- 基础设施迁移
- 数据库层迁移
- 服务层迁移
- 控制器层迁移

### 8.3 测试阶段 (2-3 周)
- 单元测试
- 集成测试
- 性能测试

### 8.4 部署阶段 (1 周)
- 预生产部署
- 生产部署
- 监控优化

**总计**: 8-12 周

## 9. 成功标准

### 9.1 性能指标
- 响应时间不超过 WebFlux 版本的 120%
- 吞吐量不低于 WebFlux 版本的 90%
- 内存使用量不超过 WebFlux 版本的 110%

### 9.2 功能指标
- 所有现有 API 100% 兼容
- 所有业务功能正常运行
- 错误率低于 0.1%

### 9.3 维护指标
- 代码覆盖率不低于 80%
- 技术债务评级良好
- 开发效率提升 20% 以上

## 10. 后续优化

### 10.1 性能优化
- MyBatis-Plus 查询优化
- OkHttp 连接池调优
- 缓存策略优化

### 10.2 功能增强
- 添加 OkHttp 拦截器功能
- 实现 MyBatis-Plus 自动填充
- 增强错误处理和重试机制

### 10.3 架构演进
- 考虑微服务拆分
- 实现更好的配置管理
- 增强安全防护机制

---

**注意**: 本迁移计划需要根据实际项目情况和团队资源进行调整。建议在迁移前进行详细的技术评估和风险分析。
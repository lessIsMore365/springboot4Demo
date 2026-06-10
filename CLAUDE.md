
# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview
This is a Spring Boot 4 demonstration project showcasing virtual thread support with PostgreSQL database integration. The project uses Spring Boot 4.1.0-M3 (milestone release) with Java 25.

## Quick Start

```bash
# 1. 复制配置文件模板
cp src/main/resources/application-example.yml src/main/resources/application.yml

# 2. 编辑 application.yml，填入你的数据库/Redis 密码
#    - spring.datasource.password
#    - spring.redis.password

# 3. 首次运行前，将 application.yml 中 spring.sql.init.mode 改为 always（自动建表）
#    之后改回 never，避免重启时清空数据

# 4. 启动应用
mvn spring-boot:run
```

## Build and Development Commands

### Building the project
```bash
mvn clean compile
```

### Running the application
```bash
mvn spring-boot:run
```

### Running tests
```bash
mvn test
```

To run a single test class:
```bash
mvn test -Dtest=VirtualThreadTest
```

### Package the application
```bash
mvn clean package
```

## API 接口文档

详见 [docs/api.md](docs/api.md)

## Architecture and Key Components

### Virtual Thread Configuration
The project configures virtual threads in multiple ways:

#### Java Configuration (`VirtualThreadConfig.java`)
1. **Tomcat request processing** - `VirtualThreadConfig.protocolHandlerVirtualThreadExecutor()` configures Tomcat to use virtual threads per request
2. **Spring TaskExecutor** - `VirtualThreadConfig.taskExecutor()` provides a `TaskExecutorAdapter` wrapper around virtual thread executor for `@Async` annotations

#### YAML Configuration (`application.yml`)
- `spring.threads.virtual.enabled: true` - Enables virtual thread support globally (Spring Boot 4 feature)
- Tomcat connection optimization for virtual threads (higher max-connections, smaller accept-count)

### Database Configuration
The project is configured to connect to a local PostgreSQL database:

#### Connection Settings
- **URL**: `jdbc:postgresql://127.0.0.1:5432/postgres`
- **Username**: `xz` (configurable via `DB_USERNAME` env var)
- **Password**: configured via `DB_PASSWORD` env var
- **Driver**: `org.postgresql.Driver`

#### HikariCP Pool Optimization for Virtual Threads
- `allow-virtual-thread-pool: true` - Enables virtual thread-aware connection pool
- `maximum-pool-size: 50` - Reduced pool size since virtual threads are lightweight
- Connection timeout and lifecycle optimized for virtual threads

### Project Structure
- `src/main/java/org/example/DemoApplication.java` - Main Spring Boot application class
- `src/main/java/org/example/config/` - Configuration classes
  - `VirtualThreadConfig.java` - Virtual thread Java configuration
  - `MyBatisPlusConfig.java` - MyBatis Plus configuration (pagination, optimistic lock, auto-fill)
  - `AlipayConfig.java` - 支付宝配置（RSA2签名/验签）
  - `WechatPayConfig.java` - 微信支付配置（APIv3签名/HMAC-SHA256验签）
- `src/main/java/org/example/redis/` - **Redis 数据服务模块**（分层抽象）
  - `core/RedisKeyNamespace.java` - Key 命名空间枚举（12 种前缀 + 默认 TTL）
  - `core/RedisKeyGenerator.java` - 统一 Key 生成器
  - `service/RedisOps.java` - 底层操作接口（set/get/delete/scan/pipeline/ping）
  - `service/RedisCache.java` - 中间层缓存接口（Cache-Aside + 类型安全 + 统计）
  - `service/RedisLock.java` - 分布式锁接口（Lua 可重入）
  - `service/RedisRateLimiter.java` - 限流接口（Token Bucket）
  - `service/impl/RedisOpsImpl.java` - SCAN 游标迭代 + Pipeline 批量
  - `service/impl/RedisCacheImpl.java` - Cache-Aside + ObjectMapper 序列化 + 命中率统计
  - `service/impl/RedisLockImpl.java` - Lua HSET 可重入锁（续期/多实例安全）
  - `service/impl/RedisRateLimiterImpl.java` - Lua Token Bucket 原子实现
  - `util/LuaScripts.java` - 集中 Lua 脚本常量
  - `monitor/RedisMetrics.java` - 缓存命中/失效/写入数 AtomicLong 计数器
  - `monitor/RedisHealth.java` - 定时健康检查（每 60s）
  - `config/RedisConfiguration.java` - 统一配置（替代旧 RedisConfig）
- `src/main/java/org/example/entity/` - Entity classes
  - `User.java`, `Role.java`, `Permission.java`, `UserRole.java`, `RolePermission.java`
  - `PaymentOrder.java` - 支付订单实体
  - `PaymentConfig.java` - 支付配置实体（Web 端实时生效）
  - `ReconciliationRecord.java` - 对帐记录实体
  - `ReconciliationDetail.java` - 对帐明细实体
- `src/main/java/org/example/mapper/` - MyBatis Plus mapper interfaces
  - `UserMapper.java`, `RoleMapper.java`, `PermissionMapper.java`, `UserRoleMapper.java`, `RolePermissionMapper.java`
  - `PaymentOrderMapper.java`, `PaymentConfigMapper.java`, `ReconciliationRecordMapper.java`, `ReconciliationDetailMapper.java`
- `src/main/java/org/example/service/` - Service interfaces
  - `UserService.java`, `RoleService.java`, `PermissionService.java`
  - `CaptchaService.java` - 验证码服务接口
  - `PaymentService.java` - 支付服务接口
  - `ReconciliationService.java` - 对帐服务接口
- `src/main/java/org/example/service/impl/` - Service implementations
  - `CaptchaServiceImpl.java` - 验证码实现（RedisCache + ObjectMapper）
  - `PaymentServiceImpl.java` - 支付宝/微信支付实现（RSA2签名、APIv3认证、直接HTTP调用）
  - `ReconciliationServiceImpl.java` - 对帐实现（逐笔比对、差异识别、统计汇总）
- `src/main/java/org/example/payment/service/PaymentConfigService.java` - 支付配置服务（缓存+热重载）
- `src/main/java/org/example/payment/controller/PaymentConfigController.java` - 支付配置 REST API
- `src/main/java/org/example/controller/` - REST controllers
  - `HelloController.java`, `DemoController.java`, `DatabaseTestController.java`
  - `RedisTestController.java`, `AuthController.java`, `CaptchaController.java`
  - `UserController.java`, `RoleController.java`, `PermissionController.java`
  - `PaymentController.java` - 支付端点
  - `ReconciliationController.java` - 对帐端点
- `src/main/java/org/example/scheduler/` - Scheduled tasks
  - `ReconciliationScheduler.java` - 每日自动对帐（支付宝2:00，微信3:00，健康监控每30分钟）
- `src/main/java/org/example/ai/` - **AI 模块**（多模型对话/代码评审/RAG/用量统计）
  - `config/AiConfig.java` - AI ObjectMapper 配置
  - `controller/AiController.java` - AI 对话/Chat2SQL/巡检/模型列表
  - `controller/McpController.java` - MCP JSON-RPC 服务端点
  - `controller/AiChatHistoryController.java` - 对话历史管理
  - `controller/AiUsageController.java` - AI 用量统计
  - `controller/AiCodeReviewController.java` - AI 代码评审
  - `controller/RagController.java` - RAG 知识库管理
  - `function/AiFunction.java` - Function Calling 接口
  - `function/AiFunctionRegistry.java` - 函数注册中心
  - `function/MonitorFunctions.java` - 监控类函数（JVM/在线用户/字典）
  - `function/PaymentFunctions.java` - 支付类函数（订单查询）
  - `model/` - ChatMessage/ChatRequest/ChatResponse 数据模型
  - `provider/AiModelProvider.java` - 模型提供商接口
  - `provider/DeepSeekProvider.java` - DeepSeek 实现
  - `provider/QwenProvider.java` - 通义千问实现
  - `provider/KimiProvider.java` - Kimi（月之暗面）实现
  - `provider/GlmProvider.java` - 智谱GLM 实现
  - `provider/AiModelRouter.java` - 模型路由器
  - `service/AiChatService.java` - 对话服务接口
  - `service/impl/AiChatServiceImpl.java` - 对话服务实现（SSE流式+Function Calling+历史+用量）
  - `service/AiInspectService.java` - AI 智能巡检
  - `service/Chat2SqlService.java` - 自然语言转 SQL
  - `service/AiChatHistoryService.java` - 对话历史持久化
  - `service/AiUsageService.java` - AI 用量记录和统计
  - `service/RagService.java` - RAG 知识库（文档解析/切片/检索/问答）
  - `service/AiCodeReviewService.java` - AI 代码评审
- `src/main/java/org/example/entity/` - 新增 AI 实体
  - `AiApiUsage.java` / `AiChatSession.java` / `AiChatHistory.java`
  - `AiKnowledgeBase.java` / `AiKnowledgeDoc.java` / `AiKnowledgeChunk.java`
- `src/main/java/org/example/mapper/` - 新增 AI Mapper
  - `AiApiUsageMapper.java` / `AiChatSessionMapper.java` / `AiChatHistoryMapper.java`
  - `AiKnowledgeBaseMapper.java` / `AiKnowledgeDocMapper.java` / `AiKnowledgeChunkMapper.java`
- `src/main/java/org/example/security/` - Spring Security 配置
  - `SecurityConfig.java` - 主安全配置（JWT 资源服务器 + HTTP Basic）
  - `RedisTokenStoreConfig.java` - Redis OAuth2 token 存储（含 JavaTimeModule 序列化）
  - `AuthorizationServerConfig.java` - OAuth2 授权服务器配置（密码模式 + JWT 签名 + RSA 密钥）
  - `filter/CaptchaValidationFilter.java` - 验证码校验过滤器（集成到 OAuth2 token 端点）
  - `service/CustomUserDetailsService.java` - 自定义用户详情服务
  - `authentication/` - 自定义密码模式认证
    - `OAuth2PasswordAuthenticationToken.java` - 密码模式认证令牌
    - `OAuth2PasswordAuthenticationConverter.java` - HTTP 请求 → 认证令牌转换器
    - `OAuth2PasswordAuthenticationProvider.java` - 密码模式认证提供者（用户认证 → JWT 生成 → Redis 存储）
- `src/main/java/org/springframework/security/` - Spring Security 7.x 兼容桥接
  - `config/annotation/ObjectPostProcessor.java` - ObjectPostProcessor 包路径桥接
  - `web/util/matcher/RequestVariablesExtractor.java` - 已移除接口桥接
  - `web/util/matcher/AntPathRequestMatcher.java` - 已移除类桥接（基于 AntPathMatcher）
- `src/main/resources/application.yml` - 应用配置
- `src/main/resources/schema.sql` - 数据库表结构（用户/RBAC/支付/对帐共10张表）
- `src/main/resources/data.sql` - 初始数据

### Dependencies
- Spring Boot 4.1.0-M3 with `spring-boot-starter-web`
- `spring-boot-starter-data-jpa` for database access
- `spring-boot-starter-data-redis` for Redis connectivity
- `spring-boot-starter-security` + `spring-boot-starter-oauth2-resource-server` for Security
- `spring-security-oauth2-authorization-server` 1.5.1 for OAuth2 Authorization Server
- `jackson-datatype-jsr310` for Java 8 date/time serialization in Redis
- `spring-boot-starter-validation` for request validation
- `mybatis-plus-spring-boot3-starter` 3.5.10 for ORM
- `postgresql` driver for PostgreSQL connectivity
- `lombok` for boilerplate reduction
- Uses milestone repository: `https://repo.spring.io/milestone`

## Important Notes

1. **Spring Boot 4 Milestone Release** - This project uses Spring Boot 4.1.0-M3, which is a milestone release.

2. **Java 25** - The project requires Java 25. Virtual threads are a standard feature in Java 21+.

3. **Virtual Threads** - Virtual threads are configured via both Java config and YAML. All `@Async` methods use the `taskExecutor` virtual thread executor.

4. **Thread Information** - The `/demo/hello` endpoint and all service layer logs print thread info to confirm virtual thread usage.

5. **Database Integration** - PostgreSQL with HikariCP pool optimized for virtual threads. Password is hardcoded for demonstration only.

6. **RBAC** - Complete RBAC with User/Role/Permission entities and many-to-many associations.

7. **Captcha** - 点击汉字顺序验证码，使用 Java 2D API 生成 350×180 PNG 图片。随机散布 2 个目标汉字 + 2~3 个干扰汉字，含贝塞尔干扰线、噪点、随机旋转。字符坐标通过 `RedisCache` + ObjectMapper 序列化存储（5分钟有效期），支持文本验证和坐标验证（容差 40px）。

8. **OAuth2 认证** - 基于 Spring Authorization Server 1.5.1。支持 password、client_credentials、refresh_token、authorization_code 四种授权模式。客户端凭据通过 BCrypt 哈希存储（web-client:secret / api-client:api-secret）。JWT 使用 RSA 2048 密钥对签名，JWK Set 暴露在 `/oauth2/jwks`。Token 通过 Redis 持久化支持分布式部署。自定义密码模式集成验证码校验，验证码参数（captcha_key + captcha_code）在 CaptchaValidationFilter 中校验。包含 3 个 Spring Security 7.x 兼容桥接类：ObjectPostProcessor、AntPathRequestMatcher、RequestVariablesExtractor。

9. **Payment Module** - 支付宝页面支付 + 微信Native扫码支付。使用 RSA2 签名直接调用 REST API，无 SDK 依赖。支付参数（密钥、网关、通知地址等）支持 Web 端实时配置，无需重启。所有支付操作使用虚拟线程。

10. **Reconciliation** - 每日凌晨自动对帐。逐笔比对订单号/金额/状态，识别 MATCH/MISMATCH/LOCAL_ONLY/REMOTE_ONLY 四类差异。对帐结果持久化到数据库，支持历史查询和统计分析。

11. **Security** - 支付回调使用 RSA2/SHA256withRSA 验签，微信支付使用 APIv3 签名认证。密钥通过环境变量注入，无硬编码。公开端点：`/api/auth/*`、`/api/payment/notify/**`、`/api/ai/**`（除 `/api/ai/config/**`）、`/api/monitor/db/health`、`/api/monitor/server/health`、`/.well-known/**`。

12. **JVM Monitor** - JVM 监控端点（`/api/monitor/jvm/*`），基于 `java.lang.management` MXBeans 提供堆内存、虚拟线程/平台线程统计、GC 详情（含 Full GC 事件历史 + 暂停时间 p50/p95/p99 分布）、线程转储等实时数据，无需外部依赖。GC 监控含 10 条异常检测规则 + JMX NotificationListener 实时事件捕获。**新增**：`/memory/history` 时间序列堆内存采样（5s 间隔，30 分钟历史）、`/memory/chart` ECharts 实时曲线可视化页面。所有监控端点需要认证（chart 页面公开，通过 `?token=` 传参认证）。

12a. **Server JVM Process Monitor** - 服务器 JVM 进程监控端点（`/api/monitor/jvm/processes/*`），基于 JDK 命令行工具（`jps`、`jstat`、`jcmd`）通过 `ProcessBuilder` 发现并监控本服务器上所有 Java 进程。使用 `CompletableFuture` 异步读取进程输出避免管道缓冲区死锁，5 秒超时保护。提供进程列表、详情（堆内存池/GC/线程/VM 标志/OS 信息）、线程转储和 ECharts 多进程仪表盘。进程列表需要认证，chart 页面公开（`?token=` 认证）。

13. **Database Monitor** - 数据库监控端点（`/api/monitor/db/*`），基于 HikariCP `HikariPoolMXBean` + JMX 提供连接池实时统计（活跃/空闲/等待连接数），PostgreSQL `pg_stat_user_tables` 表统计分析（行数/大小/扫描/增删改），连接延迟检测，**慢 SQL 统计**（MyBatis 拦截器自动采集，按 SQL 模板聚合，含最近慢 SQL 明细）。health 端点公开，其余需要认证。

14. **Server Monitor** - 服务器监控端点（`/api/monitor/server/*`），跨平台（Linux/macOS）采集操作系统级指标。Linux 通过 `/proc` 文件系统获取 CPU、内存、网络统计，macOS 通过 `sysctl`/`netstat`/`ps` 等命令获取。health 端点公开，其余需要认证。

15. **OperLog** - `@Log` 注解 + AOP 切面自动记录操作日志（操作人/IP/参数/结果/耗时），支持分页查询和定期清理。

16. **Online User** - 通过 `RedisOps.scanToSet()` 非阻塞扫描 OAuth2 token 获取在线用户列表，支持强制下线（删除 Redis 中 authorization 及关联 token 索引）。替代旧版 KEYS 命令，避免阻塞 Redis。

17. **Dict Management** - 字典类型/数据 CRUD，Redis 缓存（24h TTL），增删改自动刷新对应类型缓存。预置 6 个字典类型 + 19 条字典数据。

17b. **Menu Management** — 基于 RBAC 的菜单权限管理。`sys_menu` 表存储菜单树结构（M=目录/C=菜单/F=按钮），`sys_role_menu` 存储角色菜单分配。`GET /api/menus/user` 根据当前用户的角色返回可见菜单树，前端直接渲染侧边栏。admin 全菜单，user 仅支付管理+监控管理。

17c. **Department Management** — 树形部门管理（`sys_dept` 表），支持 CRUD + 上级部门选择。`sys_dept` 新增 `default_role_id` 字段，新成员加入时自动赋予部门默认角色。`sys_role` 新增 `data_scope` 字段（1=全部/2=自定义/3=本部门/4=本部门及子部门/5=仅本人），`sys_user` 新增 `dept_id` 字段。

17d. **Data Scope Enforcement** — MyBatis Plus `DataPermissionInterceptor` 根据用户角色 `data_scope` 在 SQL 层面自动过滤 `sys_user` 查询（scope 1=全部, 3=本部门, 4=本部门及子部门, 5=仅本人）。Spring HandlerInterceptor 请求前装载 ThreadLocal 上下文，请求后自动清理。

17e. **Multi-Role Overlay & Auto-Assign** — 用户可拥有多个角色，权限/菜单/数据范围取并集（data_scope 取最小值=最宽松）。创建用户时根据 `deptId` 自动赋予部门默认角色，与传入角色取并集。演示数据：zhangsan/lisi（技术部）、wangwu（市场部）、zhaoliu（测试组长，3 角色叠加）、maqi（财务部），密码均为 `password`。详见 CLAUDE.md §12g5。

18. **SpringDoc** - 交互式 API 文档（Swagger UI），支持 Bearer JWT 认证，公开端点无需认证即可访问。

19. **AI Module** — 参考 Pig AI 设计，集成多模型 AI 能力（DeepSeek/通义千问/Kimi/智谱GLM）。提供商参数支持 Web 端实时配置无需重启。功能包括：
    - 流式对话（SSE）+ Function Calling（JVM/在线用户/字典/支付订单）
    - 智能巡检（AI 分析监控指标）
    - Chat2SQL（自然语言转 SQL，安全限制）
    - 代码评审（空指针/SQL注入/并发安全/资源泄漏检测）
    - 对话历史持久化（会话 + 消息）
    - Token 用量统计（按模型/用户/时间，含成本估算）
    - RAG 知识库（文档上传→Apache Tika 解析→切片→关键词检索→LLM 问答）
    - MCP 协议服务（JSON-RPC，暴露业务函数给外部 Agent）

20. **Database Tables** - `schema.sql` 自动创建 23 张表（启动时 `mode: always`）:
    - `sys_user`, `sys_role`, `sys_permission`, `sys_user_role`, `sys_role_permission`
    - `sys_menu`, `sys_role_menu`, `sys_dept`
    - `sys_oper_log`, `sys_dict_type`, `sys_dict_data`
    - `payment_order`, `payment_notify_log`, `payment_config`, `reconciliation_record`, `reconciliation_detail`
    - `ai_api_usage`, `ai_chat_session`, `ai_chat_history`
    - `ai_knowledge_base`, `ai_knowledge_doc`, `ai_knowledge_chunk`
    
    初始数据（`data.sql`）:
    - 默认用户: `admin` / `user`（密码均为 `password`，BCrypt 加密），admin 所属总公司，user 所属研发组
    - 默认角色: `ROLE_ADMIN` / `ROLE_USER`（管理员 data_scope=全部，普通用户 data_scope=本部门）
    - 6 个部门（总公司 → 技术部/市场部/财务部，技术部 → 研发组/测试组）
    - 14 个权限（用户/角色/权限管理 + 分配权限）
    - 13 条菜单（系统管理/支付管理/监控管理三大目录，admin 全菜单，user 仅后两项）
    - 5 条示例支付订单，2 条示例对帐记录，3 条示例对帐明细
    
    **注意**: 默认密码仅用于演示，生产环境请更换。

21. **Redis 数据服务** — 三层抽象架构替代旧版 `StringRedisTemplate` 直接注入：
    - **RedisOps**（底层）— 封装 SCAN 游标迭代（替代 KEYS）、Pipeline 批量操作、连接复用、异步变体
    - **RedisCache**（中间层）— Cache-Aside 模式 + ObjectMapper 自动序列化 + 命中/失效统计（`/api/redis/stats` 暴露）
    - **RedisLock**（分布式锁）— Lua HSET 可重入锁 + 续期（`instanceId=host:port` 多实例安全）
    - **RedisRateLimiter**（限流）— Lua Token Bucket 原子实现，供 `@RateLimit` 切面复用
    - **RedisKeyNamespace** — 枚举管理 12 种 key 前缀 + 默认 TTL，编译时检查
    - **迁移完成**: Captcha/Dict/OnlineUser/RateLimitAspect/IdempotentAspect/DistributedLock/TokenRevocationFilter/RedisTokenStoreConfig 共 8 处调用方已全部切换至新抽象层

## Development Workflow
When adding new features or modifying virtual thread configuration:
- Update `VirtualThreadConfig.java` for thread pool changes
- Add new controllers in the `controller` package
- Test virtual thread behavior by checking thread names in output
- Ensure compatibility with Spring Boot 4 milestone APIs

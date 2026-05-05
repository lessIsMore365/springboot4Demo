
# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview
This is a Spring Boot 4 demonstration project showcasing virtual thread support with PostgreSQL database integration. The project uses Spring Boot 4.1.0-M3 (milestone release) with Java 21.

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

---

## API 接口文档

所有接口返回统一 JSON 格式：`{"success": true/false, "data": ..., "message": "...", "timestamp": 1234567890}`

### 通用说明
- 基础路径: `http://localhost:8080`
- 认证方式: OAuth2 (password grant) + JWT
- 公开端点无需认证，受保护端点需携带 `Authorization: Bearer <token>`
- 虚拟线程端点带 `/async` 后缀，异步返回 `CompletableFuture`

---

### 1. 基础端点

#### `GET /hello`
返回 "Hello Spring Boot 4!"（Hello 控制器）

**响应示例:**
```json
"Hello Spring Boot 4!"
```

---

#### `GET /demo/hello`
返回 "hello virtual thread"，控制台打印线程信息用于确认虚拟线程是否生效

**响应示例:**
```json
"hello virtual thread"
```

---

### 2. 数据库端点

#### `GET /db/test`
测试 PostgreSQL 连接，返回版本信息

**响应示例:**
```json
{
  "success": true,
  "message": "PostgreSQL连接成功",
  "databaseVersion": "PostgreSQL 16.0",
  "timestamp": 1700000000000
}
```

#### `GET /db/health`
数据库健康检查

---

### 3. 认证端点（公开）

#### `POST /api/auth/register`
用户注册

**请求体:**
```json
{
  "username": "newuser",
  "password": "password123",
  "email": "newuser@example.com",
  "age": 25,
  "remark": "新用户",
  "roles": "ROLE_USER"
}
```

**响应示例:**
```json
{
  "success": true,
  "message": "用户注册成功",
  "userId": 1002,
  "username": "newuser",
  "timestamp": 1700000000000
}
```

#### `GET /api/auth/me`
获取当前登录用户信息（需要认证）

**响应示例:**
```json
{
  "success": true,
  "user": {
    "id": 1000,
    "username": "admin",
    "email": "admin@example.com",
    "age": 30,
    "roles": "ROLE_ADMIN",
    "enabled": true,
    "accountNonLocked": true,
    "accountNonExpired": true,
    "credentialsNonExpired": true,
    "createTime": "2026-01-01T00:00:00",
    "updateTime": "2026-01-01T00:00:00",
    "lastLoginTime": "2026-05-03T08:00:00",
    "remark": "系统管理员"
  },
  "authorities": ["ROLE_ADMIN"],
  "timestamp": 1700000000000
}
```

#### `GET /api/auth/health`
认证服务健康检查

---

### 4. 验证码端点（公开）

#### `GET /api/auth/captcha`
获取图形验证码，返回 Base64 PNG 图片 + captchaKey（5分钟有效期）

**响应示例:**
```json
{
  "success": true,
  "data": {
    "captchaKey": "abc123-def456-789",
    "captchaImage": "data:image/png;base64,iVBORw0KGgo...",
    "expireIn": 300
  },
  "message": "验证码获取成功"
}
```

#### `POST /api/auth/captcha/verify`
验证验证码

**请求体:**
```json
{
  "captchaKey": "abc123-def456-789",
  "captchaCode": "A3xK"
}
```

**响应示例:**
```json
{
  "success": true,
  "message": "验证码验证通过"
}
```

#### `POST /oauth2/token`
OAuth2 登录（password grant），需额外提交 `captcha_key` + `captcha_code` 参数

**请求参数 (application/x-www-form-urlencoded):**
```
grant_type=password
username=admin
password=password
captcha_key=abc123-def456-789
captcha_code=A3xK
```

**响应示例:**
```json
{
  "access_token": "eyJhbGciOi...",
  "refresh_token": "abc...",
  "token_type": "Bearer",
  "expires_in": 7200
}
```

---

### 5. 用户管理端点（需要认证）

| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| `GET` | `/api/users` | 分页查询用户 | 认证 |
| `GET` | `/api/users/async` | 异步分页查询（虚拟线程） | 认证 |
| `POST` | `/api/users` | 创建用户 | ADMIN |
| `POST` | `/api/users/async` | 异步创建用户（虚拟线程） | ADMIN |
| `POST` | `/api/users/batch` | 批量创建测试用户 `?count=10` | ADMIN |
| `POST` | `/api/users/batch/async` | 异步批量创建（虚拟线程） | ADMIN |
| `GET` | `/api/users/stats` | 用户统计（总数） | 认证 |
| `GET` | `/api/users/stats/async` | 异步统计（虚拟线程） | 认证 |
| `GET` | `/api/users/performance` | 数据库性能测试 | 认证 |
| `GET` | `/api/users/concurrent-test` | 并发测试 `?concurrentCount=5` | 认证 |
| `GET` | `/api/users/health` | 健康检查 | 公开 |

**分页查询请求:** `GET /api/users?page=1&size=10`

**分页查询响应:**
```json
{
  "success": true,
  "data": [{ "id": 1000, "username": "admin", ... }],
  "pagination": {
    "page": 1,
    "size": 10,
    "total": 2,
    "pages": 1
  },
  "timestamp": 1700000000000
}
```

---

### 6. 角色管理端点（需要认证）

| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| `GET` | `/api/roles` | 分页查询角色 | 认证 |
| `GET` | `/api/roles/async` | 异步分页查询（虚拟线程） | 认证 |
| `POST` | `/api/roles` | 创建角色 | ADMIN |
| `POST` | `/api/roles/async` | 异步创建角色（虚拟线程） | ADMIN |
| `POST` | `/api/roles/batch` | 批量创建测试角色 `?count=5` | ADMIN |
| `POST` | `/api/roles/batch/async` | 异步批量创建（虚拟线程） | ADMIN |
| `GET` | `/api/roles/stats` | 角色统计 | 认证 |
| `GET` | `/api/roles/stats/async` | 异步统计（虚拟线程） | 认证 |
| `GET` | `/api/roles/code/{code}` | 根据编码查询角色 | 认证 |
| `GET` | `/api/roles/user/{userId}` | 获取用户的角色列表 | 认证 |
| `POST` | `/api/roles/assign` | 为用户分配角色 | ADMIN |
| `POST` | `/api/roles/assign/async` | 异步分配角色（虚拟线程） | ADMIN |
| `GET` | `/api/roles/check?userId=1&roleCode=ROLE_ADMIN` | 检查用户是否有某角色 | 认证 |
| `POST` | `/api/roles/permissions/assign` | 为角色分配权限 | ADMIN |
| `GET` | `/api/roles/{roleId}/permissions` | 获取角色的权限ID列表 | 认证 |
| `GET` | `/api/roles/health` | 健康检查 | 公开 |

**创建角色请求:**
```json
{
  "name": "测试角色",
  "code": "ROLE_TEST",
  "description": "测试用角色",
  "sortOrder": 10,
  "enabled": true
}
```

**分配角色请求:**
```json
{
  "userId": 1000,
  "roleIds": [1, 2]
}
```

**分配权限请求:**
```json
{
  "roleId": 1,
  "permissionIds": [101, 102, 103]
}
```

---

### 7. 权限管理端点（需要认证）

| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| `GET` | `/api/permissions` | 分页查询权限 | 认证 |
| `GET` | `/api/permissions/async` | 异步分页查询（虚拟线程） | 认证 |
| `POST` | `/api/permissions` | 创建权限 | ADMIN |
| `POST` | `/api/permissions/async` | 异步创建权限（虚拟线程） | ADMIN |
| `POST` | `/api/permissions/batch` | 批量创建测试权限 `?count=10` | ADMIN |
| `POST` | `/api/permissions/batch/async` | 异步批量创建（虚拟线程） | ADMIN |
| `GET` | `/api/permissions/stats` | 权限统计 | 认证 |
| `GET` | `/api/permissions/stats/async` | 异步统计（虚拟线程） | 认证 |
| `GET` | `/api/permissions/code/{code}` | 根据编码查询权限 | 认证 |
| `GET` | `/api/permissions/type/{type}` | 根据类型查询权限列表 | 认证 |
| `GET` | `/api/permissions/parent/{parentId}` | 根据父级ID查询子权限 | 认证 |
| `GET` | `/api/permissions/user/{userId}` | 获取用户的权限列表 | 认证 |
| `GET` | `/api/permissions/role/{roleId}` | 获取角色的权限列表 | 认证 |
| `GET` | `/api/permissions/check?userId=1&permissionCode=user:read` | 检查用户是否有某权限 | 认证 |
| `GET` | `/api/permissions/check-url?userId=1&url=/api/users&method=GET` | 检查用户是否有某URL权限 | 认证 |
| `GET` | `/api/permissions/health` | 健康检查 | 公开 |

**创建权限请求:**
```json
{
  "name": "查看订单",
  "code": "order:read",
  "type": "API",
  "description": "查看订单列表权限",
  "url": "/api/orders",
  "method": "GET",
  "sortOrder": 5,
  "enabled": true
}
```

---

### 8. Redis 端点（公开）

| 方法 | 路径 | 说明 |
|------|------|------|
| `GET` | `/api/redis/test` | 测试 Redis 连接 |
| `GET` | `/api/redis/test/async` | 异步测试（虚拟线程） |
| `POST` | `/api/redis/set` | 设置键值对 |
| `POST` | `/api/redis/set/async` | 异步设置（虚拟线程） |
| `GET` | `/api/redis/get/{key}` | 获取键值 |
| `GET` | `/api/redis/get/{key}/async` | 异步获取（虚拟线程） |
| `DELETE` | `/api/redis/delete/{key}` | 删除键 |
| `GET` | `/api/redis/exists/{key}` | 检查键是否存在 |
| `POST` | `/api/redis/expire/{key}?timeout=60&timeUnit=SECONDS` | 设置过期时间 |
| `GET` | `/api/redis/info` | 获取 Redis 信息 |
| `GET` | `/api/redis/stats` | 获取 Redis 统计 |
| `GET` | `/api/redis/keys?pattern=*` | 按模式查询键 |
| `POST` | `/api/redis/hash/{key}/{field}` | 设置哈希字段值 |
| `GET` | `/api/redis/hash/{key}/{field}` | 获取哈希字段值 |
| `POST` | `/api/redis/list/{key}/lpush` | 列表左推入 |
| `GET` | `/api/redis/list/{key}?start=0&end=-1` | 列表范围查询 |
| `POST` | `/api/redis/set/{key}` | 集合添加元素 |
| `POST` | `/api/redis/performance/batch-set?count=100` | 批量性能测试 |
| `GET` | `/api/redis/concurrent-test?concurrentCount=10` | 并发测试（虚拟线程） |
| `GET` | `/api/redis/health` | 健康检查 |
| `DELETE` | `/api/redis/flush` | 清空当前数据库（需要 ADMIN） |

**设置键值请求:**
```json
{
  "key": "test_key",
  "value": "hello redis",
  "timeout": 300,
  "timeUnit": "SECONDS"
}
```

---

### 9. 支付端点

#### `POST /api/payment/create`
创建支付订单（支付宝/微信）

**请求体:**
```json
{
  "subject": "测试商品",
  "body": "商品描述",
  "amount": 99.00,
  "paymentMethod": "ALIPAY"
}
```
`paymentMethod` 可选值: `ALIPAY` / `WECHAT`

**支付宝响应示例:**
```json
{
  "success": true,
  "data": {
    "orderNo": "AL20260503001",
    "amount": 99.00,
    "paymentMethod": "ALIPAY",
    "status": "PENDING",
    "payForm": "<form id=\"alipayForm\" action=\"https://openapi.alipay.com/gateway.do\" method=\"POST\">...</form>"
  },
  "timestamp": 1700000000000
}
```

**微信支付响应示例:**
```json
{
  "success": true,
  "data": {
    "orderNo": "WX20260503001",
    "amount": 199.00,
    "paymentMethod": "WECHAT",
    "status": "PENDING",
    "codeUrl": "weixin://wxpay/bizpayurl?pr=wxabc123..."
  },
  "timestamp": 1700000000000
}
```

#### `POST /api/payment/create/async`
异步创建支付订单（虚拟线程），请求体同上，返回 `CompletableFuture`

#### `POST /api/payment/notify/alipay`（公开）
支付宝异步支付通知回调，由支付宝服务器调用，参数为 `application/x-www-form-urlencoded`

**支付宝回调参数:** `out_trade_no`, `trade_no`, `trade_status`, `total_amount`, `buyer_id`, `sign`, `sign_type` 等

**响应:** 返回字符串 `"success"` 或 `"failure"`

#### `POST /api/payment/notify/wechat`（公开）
微信支付异步通知回调，由微信服务器调用

**请求头:**
```
Wechatpay-Signature: xxx
Wechatpay-Serial: xxx
Wechatpay-Nonce: xxx
Wechatpay-Timestamp: 1700000000
```

**响应:**
```json
{"code": "SUCCESS", "message": "OK"}
```
或
```json
{"code": "FAIL", "message": "签名验证失败"}
```

#### `GET /api/payment/order/{orderNo}`
查询订单状态

**响应示例:**
```json
{
  "success": true,
  "data": {
    "id": 5001,
    "orderNo": "AL20260503001",
    "paymentMethod": "ALIPAY",
    "amount": 99.00,
    "subject": "测试商品-支付宝",
    "body": "支付宝支付测试订单",
    "status": "SUCCESS",
    "tradeNo": "2026050322001000000000000001",
    "buyerId": "2088000000000001",
    "paidTime": "2026-05-03T10:30:00",
    "refundAmount": 0.00,
    "createTime": "2026-05-03T10:00:00"
  },
  "timestamp": 1700000000000
}
```

**订单状态说明:** `PENDING`(待支付) / `SUCCESS`(已支付) / `CLOSED`(已关闭) / `REFUND`(已退款)

#### `POST /api/payment/order/{orderNo}/close`
关闭未支付订单

**响应示例:**
```json
{
  "success": true,
  "message": "订单已关闭",
  "timestamp": 1700000000000
}
```

#### `POST /api/payment/refund`
申请退款

**请求体:**
```json
{
  "orderNo": "AL20260503001",
  "amount": 99.00,
  "reason": "用户申请退款"
}
```

**响应示例:**
```json
{
  "success": true,
  "data": {
    "success": true,
    "refundTradeNo": "TRD20260503103000001",
    "amount": 99.00
  },
  "timestamp": 1700000000000
}
```

#### `GET /api/payment/orders?page=1&size=10`
分页查询支付订单

#### `GET /api/payment/health`
支付服务健康检查

**响应示例:**
```json
{
  "status": "UP",
  "service": "支付服务",
  "supportedMethods": ["ALIPAY", "WECHAT"],
  "timestamp": 1700000000000
}
```

---

### 10. 对帐端点

#### `POST /api/reconciliation/run`
手动触发对帐

**请求体:**
```json
{
  "date": "2026-05-02",
  "paymentMethod": "ALIPAY"
}
```
`date` 默认昨天，`paymentMethod` 可选 `ALIPAY` / `WECHAT`，默认 `ALIPAY`

**响应示例（对帐一致）:**
```json
{
  "success": true,
  "data": {
    "id": 6003,
    "reconDate": "2026-05-02",
    "paymentMethod": "ALIPAY",
    "localTotalAmount": 398.00,
    "remoteTotalAmount": 398.00,
    "localCount": 2,
    "remoteCount": 2,
    "diffAmount": 0.00,
    "diffCount": 0,
    "status": "SUCCESS",
    "summary": "对帐完成: 本地2笔/¥398.00, 平台2笔/¥398.00, 一致2笔, 金额不符0笔, 本地独有0笔, 平台独有0笔, 差额¥0.00"
  },
  "message": "对帐一致",
  "timestamp": 1700000000000
}
```

**响应示例（存在差异）:**
```json
{
  "success": true,
  "data": {
    "status": "DIFF",
    "diffAmount": 0.01,
    "diffCount": 1,
    "summary": "对帐完成: 本地3笔/¥498.00, 平台4笔/¥598.00, 一致2笔, 金额不符1笔, 本地独有0笔, 平台独有1笔, 差额¥100.01"
  },
  "message": "存在差异，请查看详情",
  "timestamp": 1700000000000
}
```

**对帐状态:** `SUCCESS`(对帐一致) / `DIFF`(存在差异) / `ERROR`(对帐异常)

#### `POST /api/reconciliation/run/async`
异步对帐（虚拟线程），请求/响应同上

#### `GET /api/reconciliation/records?page=1&size=10`
分页查询对帐记录

#### `GET /api/reconciliation/records/{id}`
查询单条对帐记录详情

#### `GET /api/reconciliation/details/{reconRecordId}`
查询对帐明细（逐笔比对结果）

**响应示例:**
```json
{
  "success": true,
  "data": [
    {
      "id": 7001,
      "reconRecordId": 6001,
      "reconDate": "2026-05-01",
      "orderNo": "AL20260503001",
      "tradeNo": "2026050322001000000000000001",
      "localAmount": 99.00,
      "remoteAmount": 99.00,
      "localStatus": "SUCCESS",
      "remoteStatus": "SUCCESS",
      "diffType": "MATCH",
      "diffDesc": "一致"
    },
    {
      "id": 7004,
      "reconRecordId": 6001,
      "orderNo": "REMOTE_abc123def456",
      "tradeNo": "TRD_REMOTE_abc123",
      "localAmount": 0.00,
      "remoteAmount": 50.00,
      "localStatus": "-",
      "remoteStatus": "SUCCESS",
      "diffType": "REMOTE_ONLY",
      "diffDesc": "本地无此订单"
    }
  ],
  "summary": {
    "total": 4,
    "match": 2,
    "mismatch": 1,
    "localOnly": 0,
    "remoteOnly": 1
  },
  "timestamp": 1700000000000
}
```

**差异类型说明:**
| diffType | 含义 |
|----------|------|
| `MATCH` | 一致（金额、状态完全匹配） |
| `MISMATCH` | 金额或状态不符 |
| `LOCAL_ONLY` | 仅本地存在，平台无此订单 |
| `REMOTE_ONLY` | 仅平台存在，本地无此订单 |

#### `GET /api/reconciliation/stats?startDate=2026-05-01&endDate=2026-05-05`
对帐统计（按日期范围）

**响应示例:**
```json
{
  "success": true,
  "data": {
    "totalRecords": 4,
    "successCount": 3,
    "diffCount": 1,
    "errorCount": 0,
    "totalDiffAmount": 0.01,
    "startDate": "2026-05-01",
    "endDate": "2026-05-05"
  },
  "timestamp": 1700000000000
}
```

#### `GET /api/reconciliation/health`
对帐服务健康检查

**响应示例:**
```json
{
  "status": "UP",
  "service": "对帐服务",
  "schedule": "每日凌晨2:00自动对帐",
  "supportedMethods": ["ALIPAY", "WECHAT"],
  "timestamp": 1700000000000
}
```

---

### 11. 自动对帐调度

系统内置自动对帐定时任务：

| 时间 | 任务 | 说明 |
|------|------|------|
| 每天凌晨 **2:00** | 支付宝对帐 | 自动拉取前一日支付宝帐单与本地订单比对 |
| 每天凌晨 **3:00** | 微信支付对帐 | 自动拉取前一日微信支付帐单与本地订单比对 |
| 每 **30 分钟** | 健康监控 | 打印调度器运行状态日志 |

对帐逻辑：
1. 获取本地已支付订单（`payment_order` 表，`status=SUCCESS`）
2. 模拟拉取平台对帐单（实际部署对接支付宝/微信对帐单下载接口）
3. 逐笔比对订单号、金额、状态
4. 生成 `reconciliation_record`（汇总）和 `reconciliation_detail`（明细）
5. 对帐结果通过日志输出，差异数据可通过 API 查询

---

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
- **Username**: `xz`
- **Password**: `252511`
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
  - `RedisConfig.java` - Redis connection and template configuration
  - `AlipayConfig.java` - 支付宝配置（RSA2签名/验签）
  - `WechatPayConfig.java` - 微信支付配置（APIv3签名/HMAC-SHA256验签）
- `src/main/java/org/example/entity/` - Entity classes
  - `User.java`, `Role.java`, `Permission.java`, `UserRole.java`, `RolePermission.java`
  - `PaymentOrder.java` - 支付订单实体
  - `ReconciliationRecord.java` - 对帐记录实体
  - `ReconciliationDetail.java` - 对帐明细实体
- `src/main/java/org/example/mapper/` - MyBatis Plus mapper interfaces
  - `UserMapper.java`, `RoleMapper.java`, `PermissionMapper.java`, `UserRoleMapper.java`, `RolePermissionMapper.java`
  - `PaymentOrderMapper.java`, `ReconciliationRecordMapper.java`, `ReconciliationDetailMapper.java`
- `src/main/java/org/example/service/` - Service interfaces
  - `UserService.java`, `RoleService.java`, `PermissionService.java`, `RedisService.java`
  - `CaptchaService.java` - 验证码服务接口
  - `PaymentService.java` - 支付服务接口
  - `ReconciliationService.java` - 对帐服务接口
- `src/main/java/org/example/service/impl/` - Service implementations
  - `RedisServiceImpl.java`, `CaptchaServiceImpl.java`
  - `PaymentServiceImpl.java` - 支付宝/微信支付实现（RSA2签名、APIv3认证、直接HTTP调用）
  - `ReconciliationServiceImpl.java` - 对帐实现（逐笔比对、差异识别、统计汇总）
- `src/main/java/org/example/controller/` - REST controllers
  - `HelloController.java`, `DemoController.java`, `DatabaseTestController.java`
  - `RedisTestController.java`, `AuthController.java`, `CaptchaController.java`
  - `UserController.java`, `RoleController.java`, `PermissionController.java`
  - `PaymentController.java` - 支付端点
  - `ReconciliationController.java` - 对帐端点
- `src/main/java/org/example/scheduler/` - Scheduled tasks
  - `ReconciliationScheduler.java` - 每日自动对帐（支付宝2:00，微信3:00，健康监控每30分钟）
- `src/main/java/org/example/security/` - Spring Security configuration
  - `SecurityConfig.java` - 主安全配置（OAuth2 + JWT + HTTP Basic）
  - `RedisTokenStoreConfig.java` - Redis OAuth2 token存储
  - `AuthorizationServerConfig.java` - OAuth2授权服务器配置
  - `filter/CaptchaValidationFilter.java` - 验证码校验过滤器
  - `service/CustomUserDetailsService.java` - 自定义用户详情服务
- `src/main/resources/application.yml` - 应用配置
- `src/main/resources/schema.sql` - 数据库表结构（用户/RBAC/支付/对帐共10张表）
- `src/main/resources/data.sql` - 初始数据

### Dependencies
- Spring Boot 4.1.0-M3 with `spring-boot-starter-web`
- `spring-boot-starter-data-jpa` for database access
- `spring-boot-starter-data-redis` for Redis connectivity
- `spring-boot-starter-security` + `spring-security-oauth2-authorization-server` for OAuth2
- `spring-boot-starter-validation` for request validation
- `mybatis-plus-spring-boot3-starter` 3.5.10 for ORM
- `postgresql` driver for PostgreSQL connectivity
- `lombok` for boilerplate reduction
- Uses milestone repository: `https://repo.spring.io/milestone`

## Important Notes

1. **Spring Boot 4 Milestone Release** - This project uses Spring Boot 4.1.0-M3, which is a milestone release.

2. **Java 21** - The project requires Java 21. Virtual threads are a standard feature in Java 21+.

3. **Virtual Threads** - Virtual threads are configured via both Java config and YAML. All `@Async` methods use the `taskExecutor` virtual thread executor.

4. **Thread Information** - The `/demo/hello` endpoint and all service layer logs print thread info to confirm virtual thread usage.

5. **Database Integration** - PostgreSQL with HikariCP pool optimized for virtual threads. Password is hardcoded for demonstration only.

6. **RBAC** - Complete RBAC with User/Role/Permission entities and many-to-many associations.

7. **Captcha** - 图形验证码使用 Java 2D API 生成 PNG，Redis 存储（5分钟有效期），4位字符含干扰线/噪点。

8. **Redis Token Storage** - OAuth2 tokens 存储在 Redis，支持分布式部署。

9. **Payment Module** - 支付宝页面支付 + 微信Native扫码支付。使用 RSA2 签名直接调用 REST API，无 SDK 依赖。所有支付操作使用虚拟线程。

10. **Reconciliation** - 每日凌晨自动对帐。逐笔比对订单号/金额/状态，识别 MATCH/MISMATCH/LOCAL_ONLY/REMOTE_ONLY 四类差异。对帐结果持久化到数据库，支持历史查询和统计分析。

11. **Security** - 支付回调使用 RSA2/SHA256withRSA 验签，微信支付使用 APIv3 签名认证。密钥通过环境变量注入，无硬编码。

12. **Database Tables** - `schema.sql` 自动创建 10 张表（启动时 `mode: always`）:
    - `sys_user`, `sys_role`, `sys_permission`, `sys_user_role`, `sys_role_permission`
    - `payment_order`, `reconciliation_record`, `reconciliation_detail`
    
    初始数据（`data.sql`）:
    - 默认用户: `admin` / `user`（密码均为 `password`，BCrypt 加密）
    - 默认角色: `ROLE_ADMIN` / `ROLE_USER`
    - 14 个权限（用户/角色/权限管理 + 分配权限）
    - 5 条示例支付订单，2 条示例对帐记录，3 条示例对帐明细
    
    **注意**: 默认密码仅用于演示，生产环境请更换。

## Development Workflow
When adding new features or modifying virtual thread configuration:
- Update `VirtualThreadConfig.java` for thread pool changes
- Add new controllers in the `controller` package
- Test virtual thread behavior by checking thread names in output
- Ensure compatibility with Spring Boot 4 milestone APIs

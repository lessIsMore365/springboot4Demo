
# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview
This is a Spring Boot 4 demonstration project showcasing virtual thread support with PostgreSQL database integration. The project uses Spring Boot 4.1.0-M3 (milestone release) with Java 25.

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
- 认证方式: Spring Authorization Server (OAuth2 password grant + JWT)
- **公开端点**（无需认证）：`/api/auth/register`、`/api/auth/captcha`、`/api/auth/captcha/verify`、`/api/auth/health`、`/api/payment/notify/**`、`/.well-known/**`
- **获取 Token**：`POST /oauth2/token` 使用 Basic Auth（Client ID + Client Secret）+ `grant_type=password` + 验证码
- **访问受保护端点**：携带 `Authorization: Bearer <access_token>`
- 虚拟线程端点带 `/async` 后缀，异步返回 `CompletableFuture`

### OAuth2 客户端凭据

| 客户端 | Client ID | Client Secret | 支持的 Grant Type |
|--------|-----------|---------------|-------------------|
| Web 客户端 | `web-client` | `secret` | password, client_credentials, refresh_token, authorization_code |
| API 客户端 | `api-client` | `api-secret` | password, client_credentials, refresh_token |

### 认证流程

```
1. GET /api/auth/captcha  →  获取 captchaKey + 识别图片中的汉字
2. POST /oauth2/token     →  Basic Auth (web-client:secret) + grant_type=password + 用户凭据 + 验证码
3. 返回 access_token + refresh_token
4. Authorization: Bearer <access_token> → 访问受保护接口
5. 使用 refresh_token 刷新 access_token（无需验证码）
```

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

### 3. 认证端点

#### `POST /api/auth/register`（公开）
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

#### `POST /oauth2/token`（公开，Basic Auth）
OAuth2 令牌端点，使用 Spring Authorization Server。**需要 Basic Auth 请求头**（Client ID + Client Secret）。

**密码模式（password grant）— 用户登录:**

```
POST /oauth2/token
Authorization: Basic base64(web-client:secret)
Content-Type: application/x-www-form-urlencoded

grant_type=password&username=admin&password=password&captcha_key=abc123&captcha_code=春天
```

**curl 示例:**
```bash
# 1. 获取验证码
CAPTCHA=$(curl -s http://localhost:8080/api/auth/captcha)
CAPTCHA_KEY=$(echo "$CAPTCHA" | jq -r '.data.captchaKey')
CAPTCHA_CODE="春天"  # 根据图片识别

# 2. 登录获取 token
curl -s -X POST http://localhost:8080/oauth2/token \
  -u "web-client:secret" \
  -d "grant_type=password" \
  -d "username=admin" \
  -d "password=password" \
  -d "captcha_key=$CAPTCHA_KEY" \
  -d "captcha_code=$CAPTCHA_CODE"
```

**响应示例:**
```json
{
  "access_token": "eyJraWQiOiI...",
  "refresh_token": "NCbpFspfq6...",
  "token_type": "Bearer",
  "expires_in": 3599,
  "scope": "read openid profile write"
}
```

**刷新令牌（refresh_token grant）— 无需验证码:**

```bash
curl -s -X POST http://localhost:8080/oauth2/token \
  -u "web-client:secret" \
  -d "grant_type=refresh_token" \
  -d "refresh_token=NCbpFspfq6..."
```

**客户端凭据模式（client_credentials grant）— 服务间调用:**

```bash
curl -s -X POST http://localhost:8080/oauth2/token \
  -u "api-client:api-secret" \
  -d "grant_type=client_credentials"
```

**Token 配置:**
| 客户端 | Access Token 有效期 | Refresh Token 有效期 | Refresh Token 复用 |
|--------|-------------------|---------------------|-------------------|
| web-client | 1 小时 | 7 天 | 不复用（每次都发新 token） |
| api-client | 2 小时 | 30 天 | 可复用 |

#### `GET /api/auth/me`（需要认证）
获取当前登录用户信息

**请求头:** `Authorization: Bearer <access_token>`

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
  "authorities": [
    {"authority": "FACTOR_BEARER"},
    {"authority": "SCOPE_read"},
    {"authority": "SCOPE_write"},
    {"authority": "SCOPE_openid"},
    {"authority": "SCOPE_profile"}
  ],
  "timestamp": 1700000000000
}
```

#### `GET /api/auth/health`（公开）
认证服务健康检查

---

### 4. 验证码端点（公开）

点击汉字顺序验证码：图片上随机散布 4~5 个汉字，用户需按提示顺序依次点击正确汉字。支持文本和坐标两种验证方式。

#### `GET /api/auth/captcha`
获取点击汉字顺序验证码，返回 Base64 PNG 图片（350×180）+ 提示文字 + 字符坐标（5分钟有效期）

**响应示例:**
```json
{
  "success": true,
  "data": {
    "captchaKey": "abc123def456",
    "captchaImage": "data:image/png;base64,iVBORw0KGgo...",
    "promptText": "请依次点击：春天",
    "charCount": 2,
    "imageWidth": 350,
    "imageHeight": 180,
    "expireIn": 300
  },
  "message": "验证码获取成功"
}
```

#### `POST /api/auth/captcha/verify`
文本验证验证码（用户输入汉字序列，兼容 OAuth2 密码登录流程）

**请求体:**
```json
{
  "captchaKey": "abc123def456",
  "captchaCode": "春天"
}
```

**响应示例:**
```json
{
  "success": true,
  "message": "验证码验证通过"
}
```

#### `POST /api/auth/captcha/verify-position`
坐标验证验证码（前端收集用户点击坐标提交验证，容差半径 40px）

**请求体:**
```json
{
  "captchaKey": "abc123def456",
  "positions": [
    {"x": 80, "y": 90},
    {"x": 220, "y": 100}
  ]
}
```

**响应示例:**
```json
{
  "success": true,
  "message": "验证码验证通过"
}
```

#### 与 OAuth2 认证的衔接
验证码参数（`captcha_key` + `captcha_code`）在 `POST /oauth2/token` (password grant) 时提交，需同时携带 Basic Auth 请求头。详见 [3. 认证端点](#3-认证端点)。

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

### 8. Redis 端点（需要认证）

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

### 12. JVM 监控端点（需要认证）

JVM 实时监控端点，提供堆内存、线程（虚拟线程 vs 平台线程）、GC、线程转储等数据，可用于构建前端监控面板。

#### `GET /api/monitor/jvm/overview`
JVM 综合概览，一次调用获取全部关键指标

**响应示例:**
```json
{
  "success": true,
  "data": {
    "jvmName": "OpenJDK 64-Bit Server VM",
    "jvmVersion": "23.0.1",
    "uptimeMs": 3600000,
    "uptimeFormatted": "1h 0m 0s",
    "availableProcessors": 12,
    "systemLoadAverage": 2.5,
    "memory": {
      "heapUsed": 268435456,
      "heapMax": 4294967296,
      "heapCommitted": 536870912,
      "heapUsagePercent": 6.25,
      "nonHeapUsed": 134217728,
      "nonHeapCommitted": 150994944,
      "metaspaceUsed": 100663296,
      "metaspaceMax": 268435456,
      "pools": [
        {"name": "G1 Eden Space", "used": 134217728, "committed": 268435456, "max": -1, "usagePercent": 0},
        {"name": "G1 Survivor Space", "used": 8388608, "committed": 16777216, "max": -1, "usagePercent": 0},
        {"name": "G1 Old Gen", "used": 125829120, "committed": 251658240, "max": 4294967296, "usagePercent": 2.93},
        {"name": "Metaspace", "used": 100663296, "committed": 117440512, "max": 268435456, "usagePercent": 37.5},
        {"name": "CodeCache", "used": 20971520, "committed": 22020096, "max": 251658240, "usagePercent": 8.33},
        {"name": "Compressed Class Space", "used": 12582912, "committed": 13631488, "max": 1073741824, "usagePercent": 1.17}
      ]
    },
    "threads": {
      "currentCount": 42,
      "virtualCount": 18,
      "platformCount": 24,
      "daemonCount": 20,
      "peakCount": 52,
      "totalStarted": 523
    },
    "gc": {
      "collectors": [
        {
          "name": "G1 Young Generation",
          "collectionCount": 15,
          "collectionTimeMs": 320,
          "avgTimeMs": 21.3,
          "collectionsPerHour": 12.5,
          "memoryPoolNames": ["G1 Eden Space", "G1 Survivor Space", "G1 Old Gen"],
          "lastGc": {
            "id": 15,
            "startTime": 27417,
            "endTime": 27422,
            "durationMs": 5,
            "elapsedSinceMs": 125000,
            "pools": {
              "G1 Eden Space": {
                "usedBefore": 35651584,
                "usedAfter": 0,
                "committed": 37748736,
                "max": 37748736,
                "freedBytes": 35651584,
                "usageBeforePercent": 94.4,
                "usageAfterPercent": 0.0
              },
              "G1 Old Gen": {
                "usedBefore": 36807912,
                "usedAfter": 37572248,
                "committed": 44040192,
                "max": 4294967296,
                "freedBytes": -764336,
                "usageBeforePercent": 0.86,
                "usageAfterPercent": 0.87
              }
            }
          }
        }
      ],
      "warnings": {
        "hasWarning": false,
        "severity": "NORMAL",
        "warnings": []
      }
    }
  },
  "timestamp": 1700000000000
}
```

**字段说明:**
| 字段 | 说明 |
|------|------|
| `memory.heapUsed` | 堆内存已使用 (bytes) |
| `memory.heapMax` | 堆内存最大值 (bytes)，`-1` 表示无上限 |
| `memory.heapUsagePercent` | 堆内存使用率 (%) |
| `memory.pools` | 各内存池详情（G1 Eden/Survivor/Old Gen、Metaspace、CodeCache 等） |
| `threads.virtualCount` | 虚拟线程数（名称以 `VirtualThread[` 开头） |
| `threads.platformCount` | 平台线程数 |
| `threads.peakCount` | 线程峰值（自 JVM 启动以来） |
| `gc.collectors` | GC 收集器详情列表 |
| `gc.collectors[].avgTimeMs` | 平均每次 GC 耗时 (ms) |
| `gc.collectors[].collectionsPerHour` | GC 频率（次/小时） |
| `gc.collectors[].memoryPoolNames` | 该收集器管理的内存池 |
| `gc.collectors[].lastGc` | 最近一次 GC 快照（null 表示暂无） |
| `gc.collectors[].lastGc.durationMs` | 本次 GC 暂停时间 (ms) |
| `gc.collectors[].lastGc.pools` | 各内存池 GC 前后使用量变化 |
| `gc.collectors[].lastGc.pools.{name}.freedBytes` | 该内存池释放的字节数（负数=增加） |
| `gc.warnings.hasWarning` | 是否存在 GC 异常 |
| `gc.warnings.severity` | 严重级别: `NORMAL` / `WARN` / `SEVERE` |
| `gc.warnings.warnings` | 异常告警详情列表 |

---

#### `GET /api/monitor/jvm/memory`
内存详情 - 含堆/非堆 + 各内存池 + 物理内存快照

**响应示例:**
```json
{
  "success": true,
  "data": {
    "summary": {
      "heapUsed": 268435456,
      "heapMax": 4294967296,
      "heapCommitted": 536870912,
      "heapUsagePercent": 6.25,
      "nonHeapUsed": 134217728,
      "nonHeapCommitted": 150994944,
      "metaspaceUsed": 100663296,
      "metaspaceMax": 268435456,
      "pools": [
        {"name": "G1 Eden Space", "used": 134217728, "committed": 268435456, "max": -1, "usagePercent": 0},
        {"name": "Metaspace", "used": 100663296, "committed": 117440512, "max": 268435456, "usagePercent": 37.5}
      ]
    },
    "snapshot": {
      "totalPhysical": 34359738368,
      "freePhysical": 8589934592,
      "totalSwap": 10737418240,
      "freeSwap": 5368709120
    }
  },
  "timestamp": 1700000000000
}
```

---

#### `GET /api/monitor/jvm/threads`
线程详情 - 虚拟线程 vs 平台线程统计 + CPU Top 20 线程 + 状态分布

**响应示例:**
```json
{
  "success": true,
  "data": {
    "summary": {
      "currentCount": 42,
      "virtualCount": 18,
      "platformCount": 24,
      "daemonCount": 20,
      "peakCount": 52,
      "totalStarted": 523
    },
    "topCpuThreads": [
      {
        "id": 42,
        "name": "VirtualThread[#42]/runnable@ForkJoinPool-1-worker-2",
        "state": "RUNNABLE",
        "virtual": true,
        "cpuTimeMs": 1250,
        "stackTrace": "    at org.example.DemoApplication.main(DemoApplication.java:15)\n    at java.base/jdk.internal.reflect..."
      }
    ],
    "stateDistribution": [
      {"state": "RUNNABLE", "count": 12},
      {"state": "WAITING", "count": 15},
      {"state": "TIMED_WAITING", "count": 8},
      {"state": "BLOCKED", "count": 1}
    ]
  },
  "timestamp": 1700000000000
}
```

**状态分布说明:**
| 状态 | 说明 |
|------|------|
| `RUNNABLE` | 正在运行或可运行 |
| `WAITING` | 无限期等待（Object.wait、LockSupport.park） |
| `TIMED_WAITING` | 限时等待（Thread.sleep、LockSupport.parkNanos） |
| `BLOCKED` | 等待获取锁（synchronized） |
| `TERMINATED` | 已终止 |

---

#### `GET /api/monitor/jvm/thread-dump`
线程转储 - 全部线程及堆栈跟踪（最多 50 帧），用于排查死锁、线程泄漏

**响应示例:**
```json
{
  "success": true,
  "data": {
    "totalCount": 42,
    "virtualCount": 18,
    "platformCount": 24,
    "threads": [
      {
        "id": 1,
        "name": "main",
        "state": "RUNNABLE",
        "virtual": false,
        "cpuTimeMs": 0,
        "stackTrace": "    at java.base/java.lang.Thread.sleep(Native Method)\n    at org.example.DemoApplication.main(DemoApplication.java:15)\n    ... 5 more"
      },
      {
        "id": 42,
        "name": "VirtualThread[#42]/runnable@ForkJoinPool-1-worker-2",
        "state": "RUNNABLE",
        "virtual": true,
        "cpuTimeMs": 0,
        "stackTrace": "    at org.example.service.impl.PaymentServiceImpl.createPayment(PaymentServiceImpl.java:42)\n    ... 8 more"
      }
    ]
  },
  "timestamp": 1700000000000
}
```

---

#### `GET /api/monitor/jvm/gc`
GC 详情 - 各收集器详细指标 + 最近 GC 内存变化 + 异常告警

**响应示例（正常）:**
```json
{
  "success": true,
  "data": {
    "collectors": [
      {
        "name": "G1 Young Generation",
        "collectionCount": 15,
        "collectionTimeMs": 320,
        "avgTimeMs": 21.3,
        "collectionsPerHour": 12.5,
        "memoryPoolNames": ["G1 Eden Space", "G1 Survivor Space", "G1 Old Gen"],
        "lastGc": {
          "id": 15,
          "startTime": 27417,
          "endTime": 27422,
          "durationMs": 5,
          "elapsedSinceMs": 125000,
          "pools": {
            "G1 Eden Space": {
              "usedBefore": 35651584,
              "usedAfter": 0,
              "committed": 37748736,
              "max": 37748736,
              "freedBytes": 35651584,
              "usageBeforePercent": 94.4,
              "usageAfterPercent": 0.0
            },
            "G1 Old Gen": {
              "usedBefore": 36807912,
              "usedAfter": 37572248,
              "committed": 44040192,
              "max": 4294967296,
              "freedBytes": -764336,
              "usageBeforePercent": 0.86,
              "usageAfterPercent": 0.87
            }
          }
        }
      },
      {
        "name": "G1 Old Generation",
        "collectionCount": 0,
        "collectionTimeMs": 0,
        "avgTimeMs": 0.0,
        "collectionsPerHour": 0.0,
        "memoryPoolNames": ["G1 Eden Space", "G1 Survivor Space", "G1 Old Gen"],
        "lastGc": null
      }
    ],
    "warnings": {
      "hasWarning": false,
      "severity": "NORMAL",
      "warnings": []
    },
    "timestamp": 1700000000000
  },
  "timestamp": 1700000000000
}
```

**响应示例（存在异常 GC）:**
```json
{
  "success": true,
  "data": {
    "collectors": [
      {
        "name": "G1 Young Generation",
        "collectionCount": 1520,
        "collectionTimeMs": 18240,
        "avgTimeMs": 12.0,
        "collectionsPerHour": 180.5,
        "memoryPoolNames": ["G1 Eden Space", "G1 Survivor Space", "G1 Old Gen"],
        "lastGc": {
          "id": 1520,
          "durationMs": 8,
          "elapsedSinceMs": 5000,
          "pools": {
            "G1 Old Gen": {
              "usedBefore": 3145728000,
              "usedAfter": 2936012800,
              "committed": 4294967296,
              "max": 4294967296,
              "freedBytes": 209715200,
              "usageBeforePercent": 73.2,
              "usageAfterPercent": 68.4
            }
          }
        }
      },
      {
        "name": "G1 Old Generation",
        "collectionCount": 45,
        "collectionTimeMs": 135000,
        "avgTimeMs": 3000.0,
        "collectionsPerHour": 5.3,
        "memoryPoolNames": ["G1 Eden Space", "G1 Survivor Space", "G1 Old Gen"],
        "lastGc": {
          "id": 45,
          "durationMs": 3200,
          "elapsedSinceMs": 30000,
          "pools": {
            "G1 Old Gen": {
              "usedBefore": 4080218931,
              "usedAfter": 3865470566,
              "committed": 4294967296,
              "max": 4294967296,
              "freedBytes": 214748365,
              "usageBeforePercent": 95.0,
              "usageAfterPercent": 90.0
            }
          }
        }
      }
    ],
    "warnings": {
      "hasWarning": true,
      "severity": "SEVERE",
      "warnings": [
        "Full GC 较频繁: G1 Old Generation 收集器 5.3 次/小时（阈值 >5），已收集 45 次，建议检查堆内存配置",
        "Full GC 平均暂停时间长: G1 Old Generation 平均 3000.0 ms/次（阈值 >2000 ms），可能导致请求超时",
        "Full GC 效果不佳: G1 Old Generation 回收后 G1 Old Gen 使用率仍达 90.0%（已用 3.6GB/4.0GB），总释放 2.7GB，建议增大堆内存"
      ]
    },
    "timestamp": 1700000000000
  },
  "timestamp": 1700000000000
}
```

**字段说明:**

| 字段 | 说明 |
|------|------|
| `collectors[].avgTimeMs` | 平均每次 GC 耗时 (ms) |
| `collectors[].collectionsPerHour` | GC 频率（次/小时，基于 JVM 运行时间） |
| `collectors[].memoryPoolNames` | 该收集器管理的内存池名称 |
| `collectors[].lastGc` | 最近一次 GC 快照，`null` 表示该收集器尚未执行过 GC |
| `lastGc.durationMs` | 本次 GC 暂停时间 (ms) |
| `lastGc.elapsedSinceMs` | 距本次 GC 已过时间 (ms) |
| `lastGc.pools.{pool}.freedBytes` | 该内存池释放的字节数（负数表示增加） |
| `lastGc.pools.{pool}.usageBeforePercent` | GC 前使用率 (%) |
| `lastGc.pools.{pool}.usageAfterPercent` | GC 后使用率 (%) |

**异常 GC 告警规则:**

| 告警条件 | 严重级别 | 说明 |
|----------|----------|------|
| Young GC > 120 次/小时 | WARN | 对象分配过快或 Eden 区过小 |
| Full GC > 5 次/小时 | WARN | 可能存在内存泄漏或堆不足 |
| Full GC > 10 次/小时 | SEVERE | 频繁 Full GC，严重影响性能 |
| Young GC 平均 > 200ms | WARN | 新生代 GC 耗时偏高 |
| Young GC 平均 > 500ms | WARN | 新生代 GC 严重耗时 |
| Full GC 平均 > 2000ms | WARN | Full GC 暂停过长，可能导致请求超时 |
| GC 总开销 > 10% | WARN | GC 占用过多 CPU 时间 |
| GC 总开销 > 20% | SEVERE | GC 严重拖慢应用 |
| Full GC 后老年代 > 85% | WARN | 回收效果差，建议增大堆或排查泄漏 |
| 距上次 GC > 10 分钟 | WARN | GC 可能已停止工作 |

> **注意**: JVM 启动不足 5 分钟时，频率类告警自动抑制（启动阶段 GC 自然频繁）。G1 Concurrent GC 等并发标记不计入 Full GC 告警。

---

### 12b. 数据库监控端点（需要认证）

数据库监控端点，提供连接池状态、表空间统计、延迟测试等实时数据。

#### `GET /api/monitor/db/overview`
数据库综合概览 — DB 元信息 + 连接池状态 + 健康检查

**响应示例:**
```json
{
  "success": true,
  "data": {
    "db": {
      "productName": "PostgreSQL",
      "productVersion": "14.20",
      "driverName": "PostgreSQL JDBC Driver",
      "driverVersion": "42.7.10",
      "jdbcUrl": "jdbc:postgresql://127.0.0.1:5432/postgres",
      "username": "xz",
      "defaultTransactionIsolation": "READ_COMMITTED",
      "supportsBatchUpdates": true,
      "supportsSavepoints": true,
      "supportsStoredProcedures": true,
      "maxConnections": 100,
      "catalog": "postgres",
      "schema": "public"
    },
    "pool": {
      "poolName": "HikariPool-1",
      "activeConnections": 2,
      "idleConnections": 8,
      "totalConnections": 10,
      "threadsAwaitingConnection": 0,
      "maxPoolSize": 50,
      "minIdle": 10,
      "usagePercent": 4.0,
      "connectionTimeoutMs": 30000,
      "idleTimeoutMs": 600000,
      "maxLifetimeMs": 1800000,
      "keepaliveTimeMs": 30000
    },
    "health": "HEALTHY",
    "timestamp": 1700000000000
  },
  "timestamp": 1700000000000
}
```

**字段说明:**
| 字段 | 说明 |
|------|------|
| `db.productName` | 数据库产品名 |
| `db.maxConnections` | 数据库允许的最大连接数 |
| `db.defaultTransactionIsolation` | 默认事务隔离级别 |
| `pool.activeConnections` | 活跃连接数 |
| `pool.idleConnections` | 空闲连接数 |
| `pool.threadsAwaitingConnection` | 等待获取连接的线程数 |
| `pool.usagePercent` | 连接池使用率 = active / max |
| `health` | 健康状态: `HEALTHY` / `UNHEALTHY` / `DOWN: ...` |

---

#### `GET /api/monitor/db/pool`
连接池详情 — 含累计统计（超时次数、创建总数等）

**响应示例:**
```json
{
  "success": true,
  "data": {
    "pool": {
      "poolName": "HikariPool-1",
      "activeConnections": 2,
      "idleConnections": 8,
      "totalConnections": 10,
      "threadsAwaitingConnection": 0,
      "maxPoolSize": 50,
      "minIdle": 10,
      "usagePercent": 4.0,
      "connectionTimeoutMs": 30000,
      "idleTimeoutMs": 600000,
      "maxLifetimeMs": 1800000,
      "keepaliveTimeMs": 30000
    },
    "cumulative": {
      "totalConnectionsCreated": 15,
      "totalConnectionsClosed": 5,
      "totalConnectionTimeouts": 0,
      "totalFailedValidations": 0
    },
    "uptimeMs": 3600000,
    "uptimeFormatted": "1h 0m 0s"
  },
  "timestamp": 1700000000000
}
```

---

#### `GET /api/monitor/db/tables`
表统计 — 所有用户表的行数、空间占用（表/索引/总计）、扫描统计、增删改统计、VACUUM/ANALYZE 时间

**响应示例:**
```json
{
  "success": true,
  "data": [
    {
      "schemaName": "public",
      "tableName": "payment_order",
      "tableType": "TABLE",
      "rowCountEstimate": 5,
      "totalSize": "112 kB",
      "tableSize": "16 kB",
      "indexSize": "96 kB",
      "indexCount": 4,
      "seqScans": 915,
      "idxScans": 352,
      "nTupIns": 5,
      "nTupUpd": 1,
      "nTupDel": 0,
      "nLiveTup": 5,
      "nDeadTup": 0,
      "lastVacuum": "-",
      "lastAnalyze": "-"
    }
  ],
  "timestamp": 1700000000000
}
```

**字段说明:**
| 字段 | 说明 |
|------|------|
| `rowCountEstimate` | 估计行数（`n_live_tup`，MVCC 近似值） |
| `totalSize` | 总占用空间（表 + 索引） |
| `tableSize` | 表数据占用空间 |
| `indexSize` | 索引占用空间 |
| `indexCount` | 索引数量 |
| `seqScans` | 全表扫描次数 |
| `idxScans` | 索引扫描次数 |
| `nTupIns/Upd/Del` | 累计插入/更新/删除行数 |
| `nLiveTup` | 活跃行数 |
| `nDeadTup` | 死行数（待 VACUUM 回收） |
| `lastVacuum` | 上次 VACUUM 时间 |
| `lastAnalyze` | 上次 ANALYZE 时间 |

> **注意**: `nDeadTup` 过高说明需要 VACUUM，可能导致表膨胀影响性能。表按总空间降序排列。

---

#### `GET /api/monitor/db/latency`
连接延迟测试 — 获取连接 + 有效性验证耗时

**响应示例:**
```json
{
  "success": true,
  "data": {
    "latencyMs": 0,
    "valid": true,
    "timeoutMs": 5,
    "result": "OK (0ms)"
  },
  "timestamp": 1700000000000
}
```

---

#### `GET /api/monitor/db/health`（公开）
数据库监控服务健康检查

**响应示例:**
```json
{
  "success": true,
  "data": {
    "status": "UP",
    "service": "数据库监控服务",
    "timestamp": 1700000000000
  },
  "timestamp": 1700000000000
}
```

---

### 13. Java 21+ 新特性演示端点（学习用，需要认证）

Spring Boot 4 / Java 21+ 五大新特性交互式演示。所有端点**需要认证**（Bearer Token），返回 JSON。

---

#### 13.1 虚拟线程 (Virtual Thread)

基础路径: `/java21/virtual-thread`

| 方法 | 路径 | 说明 |
|------|------|------|
| `GET` | `/java21/virtual-thread/info` | 查看当前请求线程信息（是否虚拟线程） |
| `GET` | `/java21/virtual-thread/create-virtual?count=10000` | 批量创建虚拟线程 |
| `GET` | `/java21/virtual-thread/create-platform?count=200` | 批量创建平台线程（对比，上限 500） |
| `GET` | `/java21/virtual-thread/compare?vCount=10000&pCount=200` | 虚拟线程 vs 平台线程性能对比 |
| `GET` | `/java21/virtual-thread/massive?count=100000` | 海量虚拟线程测试（最大 100 万） |
| `GET` | `/java21/virtual-thread/pinning` | 线程 pinning 检测（synchronized） |
| `GET` | `/java21/virtual-thread/async` | @Async 虚拟线程异步执行 |
| `GET` | `/java21/virtual-thread/builder-api` | Thread.ofVirtual() 链式 API 演示 |

**响应示例 (`/info`):**
```json
{
  "threadName": "VirtualThread[#42]/runnable@ForkJoinPool-1-worker-2",
  "threadId": 42,
  "isVirtual": true,
  "isDaemon": true,
  "priority": 5,
  "threadGroup": "VirtualThreads"
}
```

**响应示例 (`/massive?count=100000`):**
```json
{
  "totalTasks": 100000,
  "completedTasks": 100000,
  "elapsedMs": 32,
  "avgTaskTimeUs": 0
}
```

**响应示例 (`/compare?vCount=10000&pCount=200`):**
```json
{
  "virtualThreadResult": {
    "threadType": "VIRTUAL",
    "requestedCount": 10000,
    "actualCompleted": 10000,
    "elapsedMs": 2,
    "throughputPerSecond": 5000000
  },
  "platformThreadResult": {
    "threadType": "PLATFORM",
    "requestedCount": 200,
    "actualCompleted": 200,
    "elapsedMs": 5,
    "note": "平台线程受 OS 限制，创建大量平台线程可能导致 OOM"
  },
  "summary": "虚拟线程: 10000个/2ms, 平台线程: 200个/5ms"
}
```

**响应示例 (`/pinning`):**
```json
{
  "taskCount": 100,
  "elapsedMs": 115,
  "note": "synchronized 块内 sleep 会导致虚拟线程 pinning（JDK 24+ 已修复）",
  "pinningEffect": "如果每个任务串行执行需要 1000ms（100 × 10ms），实际耗时 115ms"
}
```

**对比端点:**

| 方法 | 路径 | 说明 |
|------|------|------|
| `GET` | `/java21/virtual-thread/compare-traditional?taskCount=100&sleepMs=50` | 传统固定线程池 vs 虚拟线程并发处理对比 |

**对比响应示例 (`/compare-traditional?taskCount=100&sleepMs=50`):**
```json
{
  "taskCount": 100,
  "sleepPerTaskMs": 50,
  "threadPool": {
    "approach": "Executors.newFixedThreadPool(50) + submit",
    "poolSize": 50,
    "elapsedMs": 102,
    "note": "100 个任务排队在 50 个线程上，串行等待"
  },
  "virtualThread": {
    "approach": "Thread.ofVirtual().start() — 每个任务一个虚拟线程",
    "elapsedMs": 51,
    "note": "100 个虚拟线程并发执行，无排队"
  },
  "speedup": "2.0x 加速"
}
```

---

#### 13.2 结构化并发 (Structured Concurrency)

基础路径: `/java21/structured-concurrency`

> 需要 `--enable-preview`（Java 23 预览特性 JEP 480）

| 方法 | 路径 | 说明 |
|------|------|------|
| `GET` | `/java21/structured-concurrency/user-orders?userId=1` | ShutdownOnFailure：并行获取用户+订单 |
| `GET` | `/java21/structured-concurrency/weather?city=北京` | ShutdownOnSuccess：多源竞速返回最先结果 |
| `GET` | `/java21/structured-concurrency/payment?orderId=1001` | 支付+通知并行处理 |
| `GET` | `/java21/structured-concurrency/error-handling` | throwIfFailed(Function) 自定义异常包装 |
| `GET` | `/java21/structured-concurrency/timeout` | joinUntil 超时控制演示 |

**响应示例 (`/user-orders?userId=1`):**
```json
{
  "userName": "用户#1",
  "totalOrders": 2,
  "mode": "ShutdownOnFailure - 任一任务失败则整体失败",
  "orders": [
    { "orderId": 1001, "amount": 99.90, "status": "已完成", "createTime": "2026-05-08T10:21:17" },
    { "orderId": 1002, "amount": 199.00, "status": "待发货", "createTime": "2026-05-08T22:21:17" }
  ]
}
```

**响应示例 (`/weather?city=北京`):**
```json
{
  "weather": { "source": "气象局C", "city": "北京", "temperature": 25, "condition": "晴" },
  "mode": "ShutdownOnSuccess - 取最先返回的结果，其他任务自动取消"
}
```

**响应示例 (`/error-handling`):**
```json
{
  "error": "业务执行失败，捕获 1 个抑制异常",
  "suppressedCount": 1,
  "mode": "ShutdownOnFailure.throwIfFailed(Function) 自定义异常包装"
}
```

**响应示例 (`/timeout`):**
```json
{
  "result": "超时",
  "note": "joinUntil(100ms) 在 100ms 后抛出 TimeoutException",
  "mode": "超时控制确保不会无限等待"
}
```

**对比端点:**

| 方法 | 路径 | 说明 |
|------|------|------|
| `GET` | `/java21/structured-concurrency/compare-traditional?userId=1` | 传统 CompletableFuture.allOf() vs StructuredTaskScope |
| `GET` | `/java21/structured-concurrency/compare-race?city=北京` | 传统 CompletableFuture.anyOf() vs ShutdownOnSuccess |

**对比响应示例 (`/compare-traditional?userId=1`):**
```json
{
  "traditional": {
    "approach": "CompletableFuture.allOf() + 手动 join/get",
    "code": "CompletableFuture.supplyAsync(task1);\nCompletableFuture.supplyAsync(task2);\nCompletableFuture.allOf(f1, f2).join();\nf1.get(); f2.get();",
    "result": "用户=用户#1, 订单数=2",
    "elapsedMs": 124
  },
  "structuredConcurrency": {
    "approach": "StructuredTaskScope.ShutdownOnFailure",
    "code": "try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {\n    Subtask<T> t1 = scope.fork(task1);\n    scope.join(); scope.throwIfFailed();\n}",
    "result": "用户=用户#1, 订单数=2",
    "elapsedMs": 122
  },
  "keyDifferences": [
    "结构化并发: try-with-resources 自动清理 → 无资源泄漏",
    "结构化并发: 任一失败 → 其他任务自动取消",
    "传统做法: 需手动处理异常和取消 → 容易遗漏"
  ]
}
```

---

#### 13.3 作用域值 (Scoped Value)

基础路径: `/java21/scoped-value`

> 需要 `--enable-preview`（Java 23 预览特性 JEP 481）

| 方法 | 路径 | 说明 |
|------|------|------|
| `GET` | `/java21/scoped-value/basic` | ScopedValue 基本用法 |
| `GET` | `/java21/scoped-value/isolation` | 作用域隔离：嵌套覆盖 + 离开解绑 |
| `GET` | `/java21/scoped-value/request-context?userId=1&username=admin&role=ROLE_ADMIN` | 模拟 Web Filter → 深层调用链访问 |
| `GET` | `/java21/scoped-value/compare-tl` | ScopedValue vs ThreadLocal 性能对比（10 万次） |
| `GET` | `/java21/scoped-value/fallback` | orElse / orElseThrow 降级方法 |
| `GET` | `/java21/scoped-value/multi` | 同时绑定多个 ScopedValue |

**响应示例 (`/basic`):**
```json
{
  "boundUserId": 1000,
  "boundUsername": "admin",
  "isBound": true,
  "pattern": "ScopedValue.where(SV, value).call(() -> { SV.get() })"
}
```

**响应示例 (`/isolation`):**
```json
{
  "outsideBound": false,
  "note": "离开 ScopedValue.where().run() 后，值自动解绑（isBound=false）",
  "nestingNote": "嵌套 where() 会覆盖值，离开嵌套后恢复外层值"
}
```

**响应示例 (`/request-context?userId=1&username=demo&role=ROLE_USER`):**
```json
{
  "user": { "userId": 1, "username": "demo", "role": "ROLE_USER" },
  "traceId": "trace-1746756877606",
  "serviceLayerResult": "Service 层已处理 (user=demo)",
  "repositoryLayerResult": "Repository 层已处理 (traceId=trace-1746756877606)",
  "pattern": "Filter 设置 ScopedValue → Controller → Service → Repository 任意深度都可访问"
}
```

**响应示例 (`/compare-tl`):**
```json
{
  "scopedValueReadNs": 1200000,
  "threadLocalReadNs": 900000,
  "note": "ScopedValue 不可变（安全）、自动清理（无泄漏），ThreadLocal 需手动 remove()（易泄漏）"
}
```

**响应示例 (`/fallback`):**
```json
{
  "orElseDefaultUser": { "userId": 0, "username": "anonymous", "role": "ROLE_GUEST" },
  "orElseThrowResult": "用户未登录",
  "isBoundOutside": false
}
```

**对比端点:**

| 方法 | 路径 | 说明 |
|------|------|------|
| `GET` | `/java21/scoped-value/compare-traditional` | 传统 ThreadLocal vs ScopedValue 深度对比（含内存泄漏演示） |

**对比响应示例 (`/compare-traditional`):**
```json
{
  "threadLocal": {
    "approach": "ThreadLocal.set()/get() + 手动 remove()",
    "code": "threadLocal.set(value); threadLocal.get(); threadLocal.remove(); // 必须！",
    "elapsedNs": 1800000,
    "leakRisk": "ThreadLocal 值仍然存在: leak-99（线程复用会导致旧值残留）"
  },
  "scopedValue": {
    "approach": "ScopedValue.where().run() 自动生命周期",
    "code": "ScopedValue.where(SV, value).run(() -> { var v = SV.get(); });",
    "elapsedNs": 2100000,
    "leakRisk": "自动解绑，无泄漏风险"
  },
  "keyDifferences": [
    "ThreadLocal: 线程生命周期绑定 → 线程池中易泄漏",
    "ScopedValue: 词法作用域绑定 → 离开即消失",
    "ThreadLocal: 可变 (set) → 任意位置可修改 → 难以追踪",
    "ScopedValue: 不可变 → 只能在 where() 时设置一次"
  ]
}
```

---

#### 13.4 模式匹配 (Pattern Matching)

基础路径: `/java21/pattern-matching`

| 方法 | 路径 | 说明 |
|------|------|------|
| `GET` | `/java21/pattern-matching/area?shape=circle&dim1=5` | switch + record pattern 计算面积 |
| `GET` | `/java21/pattern-matching/describe?value=hello` | instanceof 类型模式匹配 |
| `GET` | `/java21/pattern-matching/categorize?shape=circle&dim1=150` | guarded pattern (when 子句) |
| `GET` | `/java21/pattern-matching/nested` | 嵌套 record pattern 解构 |
| `GET` | `/java21/pattern-matching/api-response?type=success` | sealed interface 实际场景：API 响应 |
| `GET` | `/java21/pattern-matching/unnamed?shape=circle&dim1=10` | unnamed pattern (_) |

**查询参数说明:**
| 参数 | 可选值 | 说明 |
|------|--------|------|
| `shape` | `circle` / `rectangle` / `triangle` | 图形类型 |
| `dim1` | 数字 | 圆=半径, 矩形=宽, 三角形=底 |
| `dim2` | 数字 | 矩形=高, 三角形=高 |
| `value` | 任意 | 自动识别为数字/布尔/字符串 |
| `type` | `success` / `error` | API 响应类型 |

**响应示例 (`/area?shape=circle&dim1=5`):**
```json
{
  "shapeType": "Circle",
  "shapeDetails": "Circle[radius=5.0]",
  "area": 78.54,
  "pattern": "switch + record pattern: Circle(var r) -> π*r²"
}
```

**响应示例 (`/describe?value=42`):**
```json
{
  "input": "42",
  "inputType": "java.lang.Integer",
  "description": "整数: 42"
}
```

**响应示例 (`/categorize?shape=circle&dim1=150`):**
```json
{
  "shape": "Circle[radius=150.0]",
  "category": "巨型圆",
  "pattern": "guarded pattern: case Circle(var r) when r > 100 -> \"巨型圆\""
}
```

**分类规则：**
| 条件 | 分类 |
|------|------|
| `Circle(r) when r > 100` | 巨型圆 |
| `Circle(r) when r > 10` | 大圆 |
| `Circle(r)` | 小圆 |
| `Rectangle(w,h) when w==h` | 正方形 |
| `Rectangle(w,h) when w>2h or h>2w` | 长条形矩形 |
| `Rectangle(w,h)` | 标准矩形 |
| `Triangle(b,h) when b==h` | 等腰直角三角形 |
| `Triangle _` | 普通三角形 |

**响应示例 (`/nested`):**
```json
{
  "input": "ColoredLine[line=Line[start=Point[x=0, y=0], end=Point[x=3, y=4]], color=红色]",
  "result": "红色 色线段: (0,0)→(3,4)",
  "pattern": "嵌套 record pattern: ColoredLine(Line(Point(var x1, var y1), Point(var x2, var y2)), var color)"
}
```

**响应示例 (`/api-response?type=success`):**
```json
{
  "type": "SUCCESS",
  "data": { "id": 1, "name": "test" },
  "message": "操作成功"
}
```

**响应示例 (`/api-response?type=error`):**
```json
{
  "type": "ERROR",
  "code": 404,
  "error": "资源未找到",
  "details": ["ID 不存在", "请检查输入"],
  "hint": "含 2 条详细信息"
}
```

**对比端点:**

| 方法 | 路径 | 说明 |
|------|------|------|
| `GET` | `/java21/pattern-matching/compare-area?shape=circle&dim1=5` | 传统 if-else+instanceof+转型 vs switch record pattern |
| `GET` | `/java21/pattern-matching/compare-describe?value=hello` | 传统 if-else 链 vs switch 类型模式 |
| `GET` | `/java21/pattern-matching/compare-api-response?type=error` | 传统 if-else/Visitor vs sealed+record pattern |

**对比响应示例 (`/compare-area?shape=circle&dim1=5`):**
```json
{
  "traditional": {
    "approach": "if-else + instanceof + 强制转型",
    "code": "if (shape instanceof Circle) {\n    Circle c = (Circle) shape;\n    return Math.PI * c.radius() * c.radius();\n} else if ...",
    "area": 78.54,
    "problems": ["需要 instanceof 检查 + 强制转型，重复代码", "编译器不检查穷举性"]
  },
  "patternMatching": {
    "approach": "switch + record pattern",
    "code": "switch (shape) {\n    case Circle(var r) -> Math.PI * r * r;\n    case Rectangle(var w, var h) -> w * h;\n}",
    "area": 78.54,
    "advantages": ["一步完成类型判断 + 解构", "sealed class 保证穷举性"]
  }
}
```

---

#### 13.5 Record 全生态化

基础路径: `/java21/record`

| 方法 | 路径 | 说明 |
|------|------|------|
| `GET` | `/java21/record/dto` | Record 作为不可变 DTO |
| `GET` | `/java21/record/generic` | 泛型 Record `PageResult<T>` |
| `GET` | `/java21/record/validation` | Compact constructor 参数验证 |
| `GET` | `/java21/record/nested` | 嵌套 Record 深层访问 |
| `GET` | `/java21/record/streams` | Record 在 Stream/分组/排序中的使用 |
| `GET` | `/java21/record/serialization` | JSON 序列化往返一致性 |
| `GET` | `/java21/record/implements` | Record 实现接口多态使用 |
| `GET` | `/java21/record/methods` | Record 自定义实例/静态方法 |
| `GET` | `/java21/record/local` | 方法内 Local Record（临时数据结构） |

**响应示例 (`/dto`):**
```json
{
  "product": { "id": 1, "name": "MacBook Pro", "price": 14999.00, "category": "电子产品" },
  "toString": "ProductDTO[id=1, name=MacBook Pro, price=14999.00, category=电子产品]",
  "equals": true,
  "note": "Record 自动生成: 规范构造器、访问器、equals、hashCode、toString"
}
```

**响应示例 (`/generic`):**
```json
{
  "page": {
    "items": [
      { "id": 1, "name": "MacBook", "price": 14999, "category": "电子" },
      { "id": 2, "name": "iPhone", "price": 8999, "category": "电子" }
    ],
    "page": 1,
    "size": 10,
    "total": 100
  },
  "total": 100,
  "note": "泛型 Record PageResult<T> 保留类型信息，可安全使用"
}
```

**响应示例 (`/validation`):**
```json
{
  "validEmail": "admin@example.com",
  "invalidEmailResult": "验证失败: 无效邮箱: invalid-email",
  "note": "Compact constructor 中验证，确保 Record 对象始终有效（不可变 + 有效 = 安全）"
}
```

**响应示例 (`/nested`):**
```json
{
  "customer": {
    "id": 100, "name": "张三",
    "email": { "value": "zhangsan@example.com" },
    "address": { "city": "北京", "street": "长安街 100 号", "zipCode": "100000" }
  },
  "city": "北京",
  "email": "zhangsan@example.com",
  "note": "嵌套 Record 通过 .a().b().c() 链式访问，深层不可变"
}
```

**响应示例 (`/streams`):**
```json
{
  "summaries": [
    { "category": "电子", "count": 3, "totalPrice": 28997 },
    { "category": "配件", "count": 2, "totalPrice": 2148 }
  ],
  "topCategory": "热销品类: 电子 (销量:3, 金额:28997)",
  "note": "Record 作为 Stream 中间类型 → 不可变、语义清晰、支持 pattern matching 解构"
}
```

**响应示例 (`/serialization`):**
```json
{
  "original": {
    "id": 1, "name": "李四",
    "email": { "value": "lisi@example.com" },
    "address": { "city": "上海", "street": "南京路 200 号", "zipCode": "200000" }
  },
  "json": "{\"id\":1,\"name\":\"李四\",\"email\":{\"value\":\"lisi@example.com\"},\"address\":{\"city\":\"上海\",\"street\":\"南京路 200 号\",\"zipCode\":\"200000\"}}",
  "roundtripEquals": true,
  "note": "Jackson 2.12+ 原生支持 Record 序列化/反序列化，无需额外注解"
}
```

**响应示例 (`/local`):**
```json
{
  "scores": [{ "name": "Alice", "score": 95 }, { "name": "Bob", "score": 87 }, { "name": "Charlie", "score": 92 }],
  "ranked": [{ "name": "Alice", "score": 95 }, { "name": "Charlie", "score": 92 }, { "name": "Bob", "score": 87 }],
  "note": "方法内定义的 Local Record，适合临时数据结构 — 不要为一次性使用污染类层级"
}
```

**对比端点:**

| 方法 | 路径 | 说明 |
|------|------|------|
| `GET` | `/java21/record/compare-pojo` | 传统 POJO（~40 行）vs Record（1 行）深度对比 |

**对比响应示例 (`/compare-pojo`):**
```json
{
  "pojo": {
    "definition": "public class ProductPOJO { private final Long id; ... }",
    "linesOfCode": "~40 行",
    "hashCode": 123456,
    "note": "需要手写/IDE 生成: 构造器、getter、equals、hashCode、toString"
  },
  "record": {
    "definition": "record ProductRecord(Long id, String name, ...) {}",
    "linesOfCode": "1 行",
    "hashCode": 123456,
    "note": "自动生成: 规范构造器、访问器方法、equals、hashCode、toString"
  },
  "comparison": {
    "immutability": "POJO 可用 final 字段模拟 | Record 天生不可变",
    "boilerplate": "POJO ~40行样板代码 | Record 1行",
    "serialization": "POJO 需注解 | Record Jackson 2.12+ 原生支持",
    "threadSafety": "POJO 需自行保证 | Record 不可变 → 天然线程安全"
  }
}
```

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

7. **Captcha** - 点击汉字顺序验证码，使用 Java 2D API 生成 350×180 PNG 图片。随机散布 2 个目标汉字 + 2~3 个干扰汉字，含贝塞尔干扰线、噪点、随机旋转。Redis 存储字符坐标（5分钟有效期），支持文本验证和坐标验证（容差 40px）。

8. **OAuth2 认证** - 基于 Spring Authorization Server 1.5.1。支持 password、client_credentials、refresh_token、authorization_code 四种授权模式。客户端凭据通过 BCrypt 哈希存储（web-client:secret / api-client:api-secret）。JWT 使用 RSA 2048 密钥对签名，JWK Set 暴露在 `/oauth2/jwks`。Token 通过 Redis 持久化支持分布式部署。自定义密码模式集成验证码校验，验证码参数（captcha_key + captcha_code）在 CaptchaValidationFilter 中校验。包含 3 个 Spring Security 7.x 兼容桥接类：ObjectPostProcessor、AntPathRequestMatcher、RequestVariablesExtractor。

9. **Payment Module** - 支付宝页面支付 + 微信Native扫码支付。使用 RSA2 签名直接调用 REST API，无 SDK 依赖。所有支付操作使用虚拟线程。

10. **Reconciliation** - 每日凌晨自动对帐。逐笔比对订单号/金额/状态，识别 MATCH/MISMATCH/LOCAL_ONLY/REMOTE_ONLY 四类差异。对帐结果持久化到数据库，支持历史查询和统计分析。

11. **Security** - 支付回调使用 RSA2/SHA256withRSA 验签，微信支付使用 APIv3 签名认证。密钥通过环境变量注入，无硬编码。

12. **JVM Monitor** - JVM 监控端点（`/api/monitor/jvm/*`），基于 `java.lang.management` MXBeans 提供堆内存、虚拟线程/平台线程统计、GC 详情、线程转储等实时数据，无需外部依赖。所有监控端点需要认证。

13. **Database Tables** - `schema.sql` 自动创建 10 张表（启动时 `mode: always`）:
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

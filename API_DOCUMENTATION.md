# Spring Boot 4 RBAC系统 API接口文档

## 项目概述
这是一个基于Spring Boot 4.1.0-M3（里程碑版本）和Java 21的演示项目，展示了虚拟线程支持与PostgreSQL数据库集成。项目实现了完整的RBAC（基于角色的访问控制）权限管理系统。

### 基础信息
- **项目地址**: http://localhost:8080
- **技术栈**: Spring Boot 4.1.0-M3, Java 21, MyBatis Plus, PostgreSQL, Spring Security, Redis
- **虚拟线程**: 全面支持Java 21虚拟线程，优化了数据库连接池和HTTP请求处理
- **数据库**: PostgreSQL (配置于 `jdbc:postgresql://127.0.0.1:5432/postgres`)

### 运行方式
```bash
# 编译项目
mvn clean compile

# 运行应用
mvn spring-boot:run

# 运行测试
mvn test

# 打包应用
mvn clean package
```

## 认证和授权说明

### 认证方式
系统支持两种认证方式：
1. **HTTP Basic认证**：用于测试和调试
2. **OAuth2/JWT认证**：生产环境推荐使用（已配置但未实现详细逻辑）

### 安全配置
- 所有API端点默认需要认证（除公开端点外）
- 使用无状态Session（SessionCreationPolicy.STATELESS）
- 禁用CSRF（REST API无状态）
- 密码加密：BCryptPasswordEncoder

### 公开端点
以下端点无需认证：
- `/api/auth/**` - 认证相关端点（注册、健康检查）
- `/api/roles/health` - 角色服务健康检查
- `/api/permissions/health` - 权限服务健康检查
- `/oauth2/**` - OAuth2端点
- `/.well-known/**` - OpenID Connect配置
- `/hello` - Hello端点
- `/demo/hello` - 虚拟线程演示端点
- `/db/**` - 数据库测试端点

### 权限注解
- `@PreAuthorize("hasRole('ADMIN')")` - 需要管理员角色
- `@PreAuthorize("hasRole('USER')")` - 需要用户角色
- `@PreAuthorize("hasPermission(...)")` - 需要特定权限

## API端点详细说明

### 1. 认证模块 (AuthController)
**基础路径**: `/api/auth`

#### 用户注册
```http
POST /api/auth/register
```
- **描述**: 注册新用户
- **认证**: 不需要
- **请求体**:
  ```json
  {
    "username": "string",        // 用户名（必填）
    "password": "string",        // 密码（必填）
    "email": "string@example.com", // 邮箱（可选）
    "age": 25,                   // 年龄（可选）
    "roles": "ROLE_USER",        // 角色（可选，逗号分隔）
    "remark": "备注信息"         // 备注（可选）
  }
  ```
- **成功响应**:
  ```json
  {
    "success": true,
    "message": "用户注册成功",
    "userId": 1002,
    "username": "newuser",
    "timestamp": 1672531200000
  }
  ```
- **错误响应**:
  ```json
  {
    "success": false,
    "message": "用户名已存在",
    "timestamp": 1672531200000
  }
  ```

#### 获取当前用户信息
```http
GET /api/auth/me
```
- **描述**: 获取当前认证用户的信息
- **认证**: 需要（任何认证用户）
- **成功响应**:
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
      "createTime": "2024-01-01T00:00:00",
      "updateTime": "2024-01-01T00:00:00",
      "lastLoginTime": "2024-01-01T12:00:00",
      "remark": "系统管理员"
    },
    "authorities": ["ROLE_ADMIN"],
    "timestamp": 1672531200000
  }
  ```

#### 健康检查
```http
GET /api/auth/health
```
- **描述**: 认证服务健康检查
- **认证**: 不需要
- **响应**:
  ```json
  {
    "status": "UP",
    "service": "authentication",
    "timestamp": 1672531200000
  }
  ```

### 2. 用户管理模块 (UserController)
**基础路径**: `/api/users`

#### 创建用户
```http
POST /api/users
```
- **描述**: 创建新用户（需要管理员权限）
- **认证**: 需要，且需ROLE_ADMIN角色
- **请求体**: User实体JSON
- **响应**:
  ```json
  {
    "success": true,
    "message": "用户创建成功",
    "data": { /* User对象 */ },
    "timestamp": 1672531200000
  }
  ```

#### 异步创建用户
```http
POST /api/users/async
```
- **描述**: 异步创建用户（使用虚拟线程）
- **认证**: 需要，且需ROLE_ADMIN角色

#### 批量创建测试用户
```http
POST /api/users/batch
```
- **描述**: 批量创建测试用户
- **认证**: 需要，且需ROLE_ADMIN角色
- **参数**: `count` (默认10) - 要创建的用户数量

#### 异步批量创建用户
```http
POST /api/users/batch/async
```
- **描述**: 异步批量创建用户（使用虚拟线程）
- **认证**: 需要，且需ROLE_ADMIN角色
- **参数**: `count`

#### 分页查询用户
```http
GET /api/users
```
- **描述**: 分页查询用户列表
- **认证**: 需要（任何认证用户）
- **参数**: 
  - `page` (默认1) - 页码
  - `size` (默认10) - 每页大小
- **响应**:
  ```json
  {
    "success": true,
    "data": [ /* User对象数组 */ ],
    "pagination": {
      "page": 1,
      "size": 10,
      "total": 150,
      "pages": 15
    },
    "timestamp": 1672531200000
  }
  ```

#### 异步分页查询用户
```http
GET /api/users/async
```
- **描述**: 异步分页查询用户（使用虚拟线程）
- **认证**: 需要（任何认证用户）
- **参数**: `page`, `size`

#### 获取用户统计信息
```http
GET /api/users/stats
```
- **描述**: 获取用户统计信息
- **认证**: 需要（任何认证用户）
- **响应**:
  ```json
  {
    "success": true,
    "stats": {
      "totalUsers": 152
    },
    "timestamp": 1672531200000
  }
  ```

#### 异步获取用户统计信息
```http
GET /api/users/stats/async
```
- **描述**: 异步获取用户统计信息（使用虚拟线程）
- **认证**: 需要（任何认证用户）

#### 数据库性能测试
```http
GET /api/users/performance
```
- **描述**: 数据库性能测试
- **认证**: 需要（任何认证用户）

#### 并发测试
```http
GET /api/users/concurrent-test
```
- **描述**: 模拟多个并发请求测试
- **认证**: 需要（任何认证用户）
- **参数**: `concurrentCount` (默认5) - 并发数

#### 健康检查
```http
GET /api/users/health
```
- **描述**: 用户服务健康检查
- **认证**: 需要（任何认证用户）
- **响应**:
  ```json
  {
    "status": "UP",
    "database": "PostgreSQL with MyBatis Plus",
    "userCount": 152,
    "message": "用户服务运行正常",
    "timestamp": 1672531200000
  }
  ```

### 3. 角色管理模块 (RoleController)
**基础路径**: `/api/roles`

#### 创建角色
```http
POST /api/roles
```
- **描述**: 创建新角色（需要管理员权限）
- **认证**: 需要，且需ROLE_ADMIN角色

#### 异步创建角色
```http
POST /api/roles/async
```
- **描述**: 异步创建角色（使用虚拟线程）
- **认证**: 需要，且需ROLE_ADMIN角色

#### 批量创建测试角色
```http
POST /api/roles/batch
```
- **描述**: 批量创建测试角色
- **认证**: 需要，且需ROLE_ADMIN角色
- **参数**: `count` (默认5)

#### 异步批量创建角色
```http
POST /api/roles/batch/async
```
- **描述**: 异步批量创建角色（使用虚拟线程）
- **认证**: 需要，且需ROLE_ADMIN角色

#### 分页查询角色
```http
GET /api/roles
```
- **描述**: 分页查询角色列表
- **认证**: 需要（任何认证用户）
- **参数**: `page` (默认1), `size` (默认10)

#### 异步分页查询角色
```http
GET /api/roles/async
```
- **描述**: 异步分页查询角色（使用虚拟线程）
- **认证**: 需要（任何认证用户）

#### 获取角色统计信息
```http
GET /api/roles/stats
```
- **描述**: 获取角色统计信息
- **认证**: 需要（任何认证用户）

#### 根据编码查询角色
```http
GET /api/roles/code/{code}
```
- **描述**: 根据角色编码查询角色
- **认证**: 需要（任何认证用户）
- **路径参数**: `code` - 角色编码

#### 为用户分配角色
```http
POST /api/roles/assign
```
- **描述**: 为用户分配角色（需要管理员权限）
- **认证**: 需要，且需ROLE_ADMIN角色
- **请求体**:
  ```json
  {
    "userId": 1001,
    "roleIds": [1, 2]
  }
  ```

#### 获取用户的角色列表
```http
GET /api/roles/user/{userId}
```
- **描述**: 获取指定用户的角色列表
- **认证**: 需要（任何认证用户）
- **路径参数**: `userId`

#### 检查用户是否拥有某个角色
```http
GET /api/roles/check
```
- **描述**: 检查用户是否拥有指定角色
- **认证**: 需要（任何认证用户）
- **参数**: `userId`, `roleCode`

#### 为角色分配权限
```http
POST /api/roles/permissions/assign
```
- **描述**: 为角色分配权限（需要管理员权限）
- **认证**: 需要，且需ROLE_ADMIN角色
- **请求体**:
  ```json
  {
    "roleId": 1,
    "permissionIds": [101, 102, 103]
  }
  ```

#### 获取角色的权限ID列表
```http
GET /api/roles/{roleId}/permissions
```
- **描述**: 获取指定角色的权限ID列表
- **认证**: 需要（任何认证用户）
- **路径参数**: `roleId`

#### 健康检查
```http
GET /api/roles/health
```
- **描述**: 角色服务健康检查
- **认证**: 不需要（公开端点）
- **响应**:
  ```json
  {
    "status": "UP",
    "database": "PostgreSQL with MyBatis Plus",
    "roleCount": 20,
    "message": "角色服务运行正常",
    "timestamp": 1672531200000
  }
  ```

### 4. 权限管理模块 (PermissionController)
**基础路径**: `/api/permissions`

#### 创建权限
```http
POST /api/permissions
```
- **描述**: 创建新权限（需要管理员权限）
- **认证**: 需要，且需ROLE_ADMIN角色

#### 异步创建权限
```http
POST /api/permissions/async
```
- **描述**: 异步创建权限（使用虚拟线程）
- **认证**: 需要，且需ROLE_ADMIN角色

#### 批量创建测试权限
```http
POST /api/permissions/batch
```
- **描述**: 批量创建测试权限
- **认证**: 需要，且需ROLE_ADMIN角色
- **参数**: `count` (默认10)

#### 异步批量创建权限
```http
POST /api/permissions/batch/async
```
- **描述**: 异步批量创建权限（使用虚拟线程）
- **认证**: 需要，且需ROLE_ADMIN角色

#### 分页查询权限
```http
GET /api/permissions
```
- **描述**: 分页查询权限列表
- **认证**: 需要（任何认证用户）
- **参数**: `page` (默认1), `size` (默认10)

#### 异步分页查询权限
```http
GET /api/permissions/async
```
- **描述**: 异步分页查询权限（使用虚拟线程）
- **认证**: 需要（任何认证用户）

#### 获取权限统计信息
```http
GET /api/permissions/stats
```
- **描述**: 获取权限统计信息
- **认证**: 需要（任何认证用户）

#### 根据编码查询权限
```http
GET /api/permissions/code/{code}
```
- **描述**: 根据权限编码查询权限
- **认证**: 需要（任何认证用户）
- **路径参数**: `code`

#### 根据类型查询权限列表
```http
GET /api/permissions/type/{type}
```
- **描述**: 根据权限类型查询权限列表
- **认证**: 需要（任何认证用户）
- **路径参数**: `type` - 权限类型（API、MENU、BUTTON、DATA）

#### 根据用户ID查询权限列表
```http
GET /api/permissions/user/{userId}
```
- **描述**: 根据用户ID查询权限列表（包括角色关联的权限）
- **认证**: 需要（任何认证用户）
- **路径参数**: `userId`

#### 根据角色ID查询权限列表
```http
GET /api/permissions/role/{roleId}
```
- **描述**: 根据角色ID查询权限列表
- **认证**: 需要（任何认证用户）
- **路径参数**: `roleId`

#### 检查用户是否拥有某个权限
```http
GET /api/permissions/check
```
- **描述**: 检查用户是否拥有指定权限
- **认证**: 需要（任何认证用户）
- **参数**: `userId`, `permissionCode`

#### 检查用户是否拥有某个URL和方法的权限
```http
GET /api/permissions/check-url
```
- **描述**: 检查用户是否拥有指定URL和HTTP方法的权限
- **认证**: 需要（任何认证用户）
- **参数**: `userId`, `url`, `method`

#### 健康检查
```http
GET /api/permissions/health
```
- **描述**: 权限服务健康检查
- **认证**: 不需要（公开端点）
- **响应**:
  ```json
  {
    "status": "UP",
    "database": "PostgreSQL with MyBatis Plus",
    "permissionCount": 50,
    "message": "权限服务运行正常",
    "timestamp": 1672531200000
  }
  ```

### 5. 基础演示模块

#### Hello端点
```http
GET /hello
```
- **描述**: 简单的Hello端点
- **认证**: 不需要
- **响应**: "Hello Spring Boot 4!"

#### 虚拟线程演示端点
```http
GET /demo/hello
```
- **描述**: 演示虚拟线程的端点
- **认证**: 不需要
- **特点**: 在控制台打印线程信息，确认是否使用虚拟线程
- **响应**: "hello virtual thread"

### 6. 数据库测试模块
**基础路径**: `/db`

#### 数据库连接测试
```http
GET /db/test
```
- **描述**: 测试PostgreSQL数据库连接和虚拟线程支持
- **认证**: 不需要
- **响应**:
  ```json
  {
    "status": "success",
    "databaseVersion": "PostgreSQL 15.0...",
    "testQueryResult": 1,
    "threadInfo": {
      "name": "VirtualThread-1",
      "isVirtual": true,
      "threadId": 123
    },
    "message": "PostgreSQL database connection successful with virtual thread support"
  }
  ```

#### 数据库健康检查
```http
GET /db/health
```
- **描述**: 数据库连接健康检查
- **认证**: 不需要
- **响应**:
  ```json
  {
    "status": "UP",
    "database": "PostgreSQL",
    "message": "Database connection is healthy"
  }
  ```

## 默认用户和角色

### 初始化数据
项目启动时会自动执行`schema.sql`和`data.sql`，创建以下默认数据：

#### 默认角色
1. **管理员角色** (ROLE_ADMIN)
   - ID: 1
   - 名称: 管理员
   - 编码: ROLE_ADMIN
   - 描述: 系统管理员，拥有所有权限

2. **普通用户角色** (ROLE_USER)
   - ID: 2
   - 名称: 普通用户
   - 编码: ROLE_USER
   - 描述: 普通用户，拥有基本权限

#### 默认用户
1. **管理员用户**
   - 用户名: `admin`
   - 密码: `password`
   - 邮箱: `admin@example.com`
   - 角色: ROLE_ADMIN

2. **普通用户**
   - 用户名: `user`
   - 密码: `password`
   - 邮箱: `user@example.com`
   - 角色: ROLE_USER

## 错误代码和状态码

### HTTP状态码
- `200 OK` - 请求成功
- `201 Created` - 资源创建成功
- `400 Bad Request` - 请求参数错误
- `401 Unauthorized` - 未认证或认证失败
- `403 Forbidden` - 认证成功但权限不足
- `404 Not Found` - 资源不存在
- `500 Internal Server Error` - 服务器内部错误

### 响应格式
所有API端点都返回统一的JSON响应格式：
```json
{
  "success": true,           // 请求是否成功
  "message": "操作成功",     // 消息描述
  "data": { ... },           // 业务数据（可选）
  "timestamp": 1672531200000 // 时间戳
}
```

## 虚拟线程特性说明

### 配置说明
项目已配置为全面支持Java 21虚拟线程：

1. **全局虚拟线程启用**
   ```yaml
   spring:
     threads:
       virtual:
         enabled: true  # Spring Boot 4特性
   ```

2. **Tomcat虚拟线程优化**
   ```yaml
   tomcat:
     max-connections: 10000  # 虚拟线程下可设置更高连接数
     accept-count: 100       # 较小的accept-count
     threads:
       max: 200              # 后备线程数
   ```

3. **HikariCP连接池优化**
   ```yaml
   hikari:
     allow-virtual-thread-pool: true  # 启用虚拟线程感知连接池
     maximum-pool-size: 50            # 虚拟线程下可减少连接池大小
   ```

## 数据库配置

### PostgreSQL连接
- **URL**: `jdbc:postgresql://127.0.0.1:5432/postgres`
- **用户名**: `xz`
- **密码**: `252511`
- **驱动**: `org.postgresql.Driver`

### 连接池配置（HikariCP）
- **最大连接数**: 50（虚拟线程优化）
- **最小空闲连接**: 10
- **连接超时**: 30000ms
- **空闲超时**: 600000ms
- **最大生命周期**: 1800000ms

## Redis配置和Token存储

### Redis连接配置
- **地址**: `127.0.0.1:6379`
- **密码**: `252511`
- **数据库**: `0`
- **连接池配置**:
  - `max-active`: 8
  - `max-idle`: 8
  - `min-idle`: 0
  - `max-wait`: -1（无限等待）

### OAuth2 Token Redis存储
系统已配置将OAuth2授权信息（授权码、access token、refresh token）存储到Redis中：
- **存储内容**: OAuth2Authorization对象（包含token信息、用户信息、权限等）
- **过期策略**: 自动根据token过期时间设置Redis键过期
- **索引机制**: 通过token值快速查找对应的授权信息
- **虚拟线程支持**: 所有Redis操作都记录线程类型（虚拟/平台线程）

### Redis测试端点
**基础路径**: `/api/redis`

#### 连接测试
```http
GET /api/redis/test
```
- **描述**: 测试Redis连接
- **认证**: 不需要
- **响应**: 返回Redis连接测试结果

#### 异步连接测试
```http
GET /api/redis/test/async
```
- **描述**: 异步测试Redis连接（使用虚拟线程）
- **认证**: 不需要

#### 健康检查
```http
GET /api/redis/health
```
- **描述**: Redis服务健康检查
- **认证**: 不需要
- **响应**: 包含Redis连接状态

#### 设置键值对
```http
POST /api/redis/set
```
- **描述**: 设置Redis键值对
- **认证**: 需要（任何认证用户）
- **请求体**:
  ```json
  {
    "key": "user:1001",
    "value": {
      "username": "test",
      "email": "test@example.com"
    },
    "timeout": 3600,           // 可选，过期时间
    "timeUnit": "SECONDS"      // 可选，时间单位
  }
  ```

#### 获取键值对
```http
GET /api/redis/get/{key}
```
- **描述**: 获取Redis键值对
- **认证**: 需要（任何认证用户）
- **路径参数**: `key` - Redis键

#### 删除键值对
```http
DELETE /api/redis/delete/{key}
```
- **描述**: 删除Redis键
- **认证**: 需要（任何认证用户）
- **路径参数**: `key`

#### 检查键是否存在
```http
GET /api/redis/exists/{key}
```
- **描述**: 检查Redis键是否存在
- **认证**: 需要（任何认证用户）
- **路径参数**: `key`

#### 设置过期时间
```http
POST /api/redis/expire/{key}
```
- **描述**: 设置Redis键过期时间
- **认证**: 需要（任何认证用户）
- **路径参数**: `key`
- **参数**: `timeout` (必需), `timeUnit` (默认SECONDS)

#### 获取Redis信息
```http
GET /api/redis/info
```
- **描述**: 获取Redis服务器信息
- **认证**: 需要（任何认证用户）

#### 获取Redis统计信息
```http
GET /api/redis/stats
```
- **描述**: 获取Redis统计信息
- **认证**: 需要（任何认证用户）

#### 获取所有键（匹配模式）
```http
GET /api/redis/keys
```
- **描述**: 按模式查询Redis键
- **认证**: 需要（任何认证用户）
- **参数**: `pattern` (默认*)

#### 哈希操作
```http
POST /api/redis/hash/{key}/{field}
```
- **描述**: 设置Redis哈希字段值
- **认证**: 需要（任何认证用户）
- **路径参数**: `key`, `field`
- **请求体**: 字段值

```http
GET /api/redis/hash/{key}/{field}
```
- **描述**: 获取Redis哈希字段值
- **认证**: 需要（任何认证用户）

#### 列表操作
```http
POST /api/redis/list/{key}/lpush
```
- **描述**: 向左推入Redis列表
- **认证**: 需要（任何认证用户）
- **路径参数**: `key`
- **请求体**: 值

```http
GET /api/redis/list/{key}
```
- **描述**: 获取Redis列表范围
- **认证**: 需要（任何认证用户）
- **参数**: `start` (默认0), `end` (默认-1)

#### 集合操作
```http
POST /api/redis/set/{key}
```
- **描述**: 添加Redis集合元素
- **认证**: 需要（任何认证用户）
- **路径参数**: `key`
- **请求体**: 值数组

#### 性能测试
```http
POST /api/redis/performance/batch-set
```
- **描述**: Redis性能测试 - 批量设置键值对
- **认证**: 需要（任何认证用户）
- **参数**: `count` (默认100)

#### 并发测试
```http
GET /api/redis/concurrent-test
```
- **描述**: Redis并发测试 - 模拟多个并发Redis操作
- **认证**: 需要（任何认证用户）
- **参数**: `concurrentCount` (默认10)

#### 清空数据库
```http
DELETE /api/redis/flush
```
- **描述**: 清空Redis当前数据库（需要管理员权限）
- **认证**: 需要，且需ROLE_ADMIN角色
- **警告**: 此操作会删除所有数据，请谨慎使用

## 开发和使用建议

### 测试建议
1. **使用默认用户**:
   - 管理员: `admin` / `password`
   - 普通用户: `user` / `password`

2. **认证测试**:
   ```bash
   # 使用HTTP Basic认证
   curl -u admin:password http://localhost:8080/api/auth/me
   
   # 注册新用户
   curl -X POST http://localhost:8080/api/auth/register \
     -H "Content-Type: application/json" \
     -d '{"username":"test","password":"test123","email":"test@example.com"}'
   ```

### 故障排除

#### 常见问题
1. **数据库连接失败**
   - 检查PostgreSQL服务是否运行
   - 验证数据库连接参数
   - 检查网络连接

2. **权限认证失败**
   - 确认用户名密码正确
   - 检查用户角色和权限分配
   - 验证安全配置

3. **虚拟线程未启用**
   - 确认Java版本为21+
   - 检查`spring.threads.virtual.enabled`配置
   - 验证`VirtualThreadConfig`是否正确加载
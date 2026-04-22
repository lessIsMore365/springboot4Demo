# Spring Boot 4 虚拟线程演示项目

这是一个基于Spring Boot 4.1.0-M3（里程碑版本）和Java 21的演示项目，展示了虚拟线程支持与PostgreSQL数据库集成。项目实现了完整的RBAC（基于角色的访问控制）权限管理系统。

## 项目特性

- **Spring Boot 4.1.0-M3**: 使用里程碑版本，支持Java 21虚拟线程
- **虚拟线程优化**: 全面支持Java 21虚拟线程，优化了HTTP请求处理和数据库连接池
- **RBAC权限系统**: 完整的用户-角色-权限管理系统
- **PostgreSQL集成**: 使用MyBatis Plus进行数据库操作
- **Spring Security**: 集成Spring Security进行认证和授权

## 快速开始

### 环境要求
- Java 21+
- Maven 3.6+
- PostgreSQL 14+

### 数据库配置
1. 创建PostgreSQL数据库
2. 修改`src/main/resources/application.yml`中的数据库连接信息

### 运行应用
```bash
mvn clean compile
mvn spring-boot:run
```

应用将在 http://localhost:8080 启动

## API文档

详细的API接口文档请查看 [API_DOCUMENTATION.md](API_DOCUMENTATION.md)

### 主要API端点
- `/api/auth/**` - 认证相关（注册、登录、当前用户）
- `/api/users/**` - 用户管理（需要管理员权限）
- `/api/roles/**` - 角色管理（需要管理员权限）
- `/api/permissions/**` - 权限管理（需要管理员权限）
- `/db/**` - 数据库测试端点
- `/demo/hello` - 虚拟线程演示端点
- `/hello` - 简单Hello端点

## 默认用户

系统初始化时会创建以下默认用户：

1. **管理员用户**
   - 用户名: `admin`
   - 密码: `password`
   - 角色: ROLE_ADMIN（拥有所有权限）

2. **普通用户**
   - 用户名: `user`
   - 密码: `password`
   - 角色: ROLE_USER（拥有基本权限）

## 项目结构

```
src/main/java/org/example/
├── DemoApplication.java          # 主应用类
├── config/                       # 配置类
│   ├── VirtualThreadConfig.java  # 虚拟线程配置
│   └── MyBatisPlusConfig.java    # MyBatis Plus配置
├── entity/                       # 实体类
│   ├── User.java                 # 用户实体
│   ├── Role.java                 # 角色实体
│   ├── Permission.java           # 权限实体
│   ├── UserRole.java             # 用户-角色关联实体
│   └── RolePermission.java       # 角色-权限关联实体
├── mapper/                       # MyBatis Plus Mapper接口
├── service/                      # 服务层接口和实现
├── controller/                   # REST控制器
└── security/                     # Spring Security配置
```

## 虚拟线程特性

项目配置了全面的虚拟线程支持：

1. **Tomcat虚拟线程处理**: 每个HTTP请求使用独立的虚拟线程
2. **异步任务虚拟线程**: `@Async`方法使用虚拟线程执行器
3. **数据库连接池优化**: HikariCP配置支持虚拟线程
4. **MyBatis Plus优化**: 执行器类型调整为虚拟线程友好

## 开发指南

### 添加新功能
1. 在`entity`包中添加实体类
2. 在`mapper`包中添加MyBatis Plus Mapper
3. 在`service`包中添加服务接口和实现
4. 在`controller`包中添加REST控制器
5. 在`data.sql`中添加初始数据（如需要）

### 权限控制
- 使用`@PreAuthorize("hasRole('ADMIN')")`进行角色控制
- 使用`@PreAuthorize("hasPermission(...)")`进行细粒度权限控制

### 虚拟线程验证
所有服务方法都会打印线程信息，可以在控制台查看是否使用虚拟线程：
```
Handling request in: VirtualThread-1
Thread is virtual: true
```

## 故障排除

### 常见问题
1. **数据库连接失败**: 检查PostgreSQL服务是否运行，连接参数是否正确
2. **权限认证失败**: 确认用户名密码正确，用户角色和权限分配正确
3. **虚拟线程未启用**: 确认Java版本为21+，检查`spring.threads.virtual.enabled`配置

### 查看日志
- 应用启动日志显示数据库初始化信息
- 虚拟线程创建和使用情况会在控制台打印
- SQL语句日志已启用，可在控制台查看数据库操作

## 许可证

本项目仅供学习和演示使用。

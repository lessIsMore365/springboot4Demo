
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

The application will start on the default port (8080). Available endpoints:
- `GET /hello` - Returns "Hello Spring Boot 4!" (HelloController)
- `GET /demo/hello` - Returns "hello virtual thread" and prints thread info (DemoController)
- `GET /db/test` - Tests PostgreSQL database connection and returns version info (DatabaseTestController)
- `GET /db/health` - Health check for database connection (DatabaseTestController)
- `GET /api/redis/test` - Tests Redis connection (RedisTestController)
- `GET /api/redis/health` - Health check for Redis connection (RedisTestController)
- `GET /api/roles` - 分页查询角色（需要认证）
- `GET /api/permissions` - 分页查询权限（需要认证）
- `POST /api/roles` - 创建角色（需要管理员权限）
- `POST /api/permissions` - 创建权限（需要管理员权限）
- `GET /api/roles/health` - 角色服务健康检查（公开）
- `GET /api/permissions/health` - 权限服务健康检查（公开）

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

#### JPA Settings
- `ddl-auto: update` - Automatically update database schema (development only)
- `show-sql: true` - Log SQL statements for debugging
- PostgreSQL dialect configured for proper SQL generation

### Project Structure
- `src/main/java/org/example/DemoApplication.java` - Main Spring Boot application class
- `src/main/java/org/example/config/VirtualThreadConfig.java` - Virtual thread Java configuration
- `src/main/java/org/example/config/MyBatisPlusConfig.java` - MyBatis Plus configuration
- `src/main/java/org/example/config/RedisConfig.java` - Redis connection and template configuration
- `src/main/java/org/example/entity/` - Entity classes
  - `User.java` - User entity
  - `Role.java` - Role entity for RBAC
  - `Permission.java` - Permission entity for RBAC
  - `UserRole.java` - User-Role association entity
  - `RolePermission.java` - Role-Permission association entity
- `src/main/java/org/example/mapper/` - MyBatis Plus mapper interfaces
  - `UserMapper.java`, `RoleMapper.java`, `PermissionMapper.java`, `UserRoleMapper.java`, `RolePermissionMapper.java`
- `src/main/java/org/example/service/` - Service interfaces and implementations
  - `UserService.java`, `RoleService.java`, `PermissionService.java`, `RedisService.java`
- `src/main/java/org/example/service/impl/` - Service implementations
  - `RedisServiceImpl.java` - Redis service implementation with virtual thread support
- `src/main/java/org/example/controller/` - REST controllers
  - `HelloController.java` - Simple greeting endpoint
  - `DemoController.java` - Virtual thread demonstration endpoint
  - `DatabaseTestController.java` - PostgreSQL database test and health check endpoints
  - `RedisTestController.java` - Redis test and management endpoints
  - `AuthController.java` - Authentication endpoints (register, current user)
  - `UserController.java` - User management endpoints
  - `RoleController.java` - Role management endpoints
  - `PermissionController.java` - Permission management endpoints
- `src/main/java/org/example/security/` - Spring Security configuration
  - `RedisTokenStoreConfig.java` - Redis OAuth2 token storage configuration
- `src/main/resources/application.yml` - Virtual thread and database YAML configuration
- `src/test/java/org/example/VirtualThreadTest.java` - Basic virtual thread test

### Dependencies
- Spring Boot 4.1.0-M3 with `spring-boot-starter-web`
- `spring-boot-starter-data-jpa` for database access
- `spring-boot-starter-data-redis` for Redis connectivity
- `postgresql` driver for PostgreSQL connectivity
- Uses milestone repository: `https://repo.spring.io/milestone`

## Important Notes

1. **Spring Boot 4 Milestone Release** - This project uses Spring Boot 4.1.0-M3, which is a milestone release. The POM is configured with the Spring Milestones repository.

2. **Java 21** - The project requires Java 21 (as specified in `maven.compiler.source` and `maven.compiler.target`).

3. **Virtual Threads** - The configuration enables virtual threads for both HTTP request processing and asynchronous task execution. Virtual threads are a standard feature in Java 21+ and are fully supported in Spring Boot 4.

4. **Dual Configuration** - Virtual threads are configured both via Java config (`VirtualThreadConfig.java`) and YAML properties (`spring.threads.virtual.enabled: true`). The Java config provides `TaskExecutorAdapter` for better Spring integration with `@Async` methods.

5. **Thread Information** - The `/demo/hello` endpoint prints thread information to confirm virtual threads are being used. Check console output for thread details.

6. **Database Integration** - The project includes PostgreSQL database configuration optimized for virtual threads. Test database connection via `/db/test` and health check via `/db/health` endpoints. Password is hardcoded for demonstration only - use environment variables or secrets management in production.

7. **RBAC (Role-Based Access Control)** - The project includes a complete RBAC implementation with User, Role, and Permission entities. Users can be assigned multiple roles, and roles can have multiple permissions. The system supports virtual thread-optimized database operations for all RBAC operations. Manage roles via `/api/roles/**` endpoints and permissions via `/api/permissions/**` endpoints.

8. **Redis Token Storage** - The project configures Redis to store OAuth2 authorization tokens (authorization codes, access tokens, refresh tokens). This enables distributed token management and improves performance. Redis configuration is optimized for virtual threads and includes comprehensive monitoring endpoints at `/api/redis/**`. All Redis operations log thread information to confirm virtual thread usage.

8. **Database Tables and Initial Data** - The project includes SQL schema definitions in `src/main/resources/schema.sql` that automatically create the following tables on application startup:
   - `sys_user` - User table (already exists)
   - `sys_role` - Role table for RBAC
   - `sys_permission` - Permission table for RBAC
   - `sys_user_role` - User-Role association table
   - `sys_role_permission` - Role-Permission association table
   
   Initial data is provided in `src/main/resources/data.sql`:
   - Default roles: `ROLE_ADMIN` and `ROLE_USER`
   - Basic permissions for user, role, and permission management
   - Default users: `admin` (password: `password`) with ADMIN role, and `user` (password: `password`) with USER role
   - Role-permission assignments: ADMIN has all permissions, USER has basic read permissions

   **Security Note**: The default passwords are for demonstration only. Change them in production or use the registration endpoint to create new users.

## Development Workflow
When adding new features or modifying virtual thread configuration:
- Update `VirtualThreadConfig.java` for thread pool changes
- Add new controllers in the `controller` package
- Test virtual thread behavior by checking thread names in output
- Ensure compatibility with Spring Boot 4 milestone APIs
# Spring Boot 4 Demo

Spring Boot 4 + Java 25 全栈演示项目，涵盖虚拟线程、RBAC 权限管理、OAuth2 认证、支付集成、AI 多模型对话、系统监控等。

## 技术栈

- **Spring Boot 4.1.0** + Java 25
- **PostgreSQL** + MyBatis Plus 3.5.10（含数据权限拦截）
- **Redis** — 缓存 / OAuth2 Token 存储 / 验证码
- **Spring Authorization Server** — OAuth2 密码模式 / JWT / RSA 签名
- **虚拟线程** — Tomcat + @Async 全链路虚拟线程
- **AI 模块** — DeepSeek / 通义千问 / Kimi / 智谱GLM 多模型对话 + RAG
- **支付模块** — 支付宝 / 微信支付（RSA2 / APIv3 签名，无 SDK 依赖）
- **监控** — JVM / DB / 服务器 / 日志在线查看 / 慢 SQL 采集

## 快速开始

```bash
# 1. 复制配置模板
cp src/main/resources/application-example.yml src/main/resources/application.yml

# 2. 编辑 application.yml，填入数据库/Redis 密码

# 3. 首次运行前，将 spring.sql.init.mode 改为 always（自动建表）
#    之后改回 never

# 4. 启动
mvn spring-boot:run
```

默认用户：`admin` / `user`，密码均为 `password`

完整 API 文档：[CLAUDE.md](./CLAUDE.md)

## 开源协议

本项目采用 **源可用（Source Available）** 协议：

| ✅ 允许 | ❌ 禁止 |
|---------|---------|
| 学习、研究、个人使用 | 商业用途 |
| Fork 仓库 | 提供收费服务 |
| 提交 PR 贡献代码 | 售卖源码 |
| 技术分享、教学演示 | 集成到商业产品 |

> 📧 商业授权请联系 **xz-jasper@qq.com**  
> 详见 [COMMERCIAL_LICENSE.md](./COMMERCIAL_LICENSE.md) 和 [LICENSE](./LICENSE)

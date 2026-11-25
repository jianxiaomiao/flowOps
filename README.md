# FlowOps

FlowOps 是一个面向企业内部协作的多租户工单流转平台。它把问题提交、部门分派、处理、方案确认、SLA 监控、消息通知和审计追踪组织成一条可验证的业务闭环。

项目采用学习型开发方式建设：除了实现功能，也记录为什么选择这些架构、替代方案是什么，以及安全、并发和一致性边界在哪里。

## 已实现能力

- 企业自助注册：原子创建租户、默认部门、首位管理员和 RBAC 关系；
- JWT 登录与当前用户恢复，前后端统一处理 401、403 和 TraceId；
- 租户、部门、用户、角色、权限、SLA 的管理与停用/重新启用；
- 工单创建、分派、接单、处理、提交方案、确认/驳回和关闭；
- 工单状态、分派和评论历史；
- SLA 截止时间快照、扫描和原子幂等超时处理；
- Transactional Outbox、RabbitMQ 重试/死信和通知消费幂等；
- Redis 分布式登录/注册限流、通知未读数缓存和 JWT 撤销/会话版本；
- 站内通知收件箱、操作日志和审计日志；
- Actuator 健康检查、Prometheus 指标、SLA 扫描业务指标、Grafana 预置看板和最小告警规则；
- Vue 3 管理界面和 53/53 后端 HTTP 操作的类型安全接入；
- Postman 全流程 Collection 和后端自动化测试。

## 技术栈

| 层次 | 技术 | 在 FlowOps 中的用途 |
| --- | --- | --- |
| 后端 | Java 21、Spring Boot 3.5 | HTTP API、业务编排、事务和应用配置 |
| 安全 | Spring Security、JWT、BCrypt | 无状态认证、方法级授权和密码摘要 |
| 持久化 | MySQL 8.4、MyBatis-Plus 3.5 | 多租户业务数据、分页、自动填充和乐观锁 |
| 消息 | RabbitMQ、Spring AMQP | 异步通知、重试和死信处理；可通过功能开关关闭 |
| 缓存 | Redis、Spring Data Redis、Lettuce | 分布式限流、未读数 Cache Aside、Token 撤销和带 TTL 共享状态 |
| 前端 | Vue 3、TypeScript、Vite | SPA 页面、类型安全和开发代理 |
| 前端生态 | Vue Router、Pinia、Axios、Element Plus | 权限路由、登录状态、HTTP 封装和业务组件 |
| 数据库迁移 | Flyway | 版本化建表、权限种子和现有数据库 baseline |
| 测试 | JUnit 5、Mockito、Spring Boot Test、Postman、Playwright | 领域规则、认证、消息幂等、接口和浏览器闭环验证 |
| 本地基础设施 | Docker Compose、Prometheus、Grafana | 启动并持久化 MySQL、Redis、RabbitMQ 与本地可观测性栈 |

## 核心设计理念

### 多租户隔离

`tenantId` 只从已经验签的 JWT 和后端安全上下文获取，不信任客户端传入值。业务查询和写入都会附加当前租户条件，避免通过猜测资源 ID 横向访问其他企业数据。

效果：同一套系统可以服务多个企业，同时保持数据和管理权限隔离。

### RBAC 角色权限控制

权限使用 `permission` 定义，角色通过 `role_permission` 获得权限，用户通过 `user_role` 获得角色。Controller 使用 `@PreAuthorize` 校验动作权限，工单读取还会叠加本人、部门或租户级数据范围。

效果：前端可以按权限隐藏入口，后端仍是最终安全边界；租户管理员和工单处理人看到的能力不同。

### DTO、Entity、VO 分离

- DTO 只接收客户端允许提交的数据；
- Entity 映射数据库表；
- VO 只返回前端需要且允许暴露的数据。

效果：客户端不能直接修改 `tenantId`、状态、密码摘要、审计字段等服务端可信数据。

### 状态机

工单不能直接修改 `status`，只能提交业务动作：

```text
OPEN → ASSIGNED → ACCEPTED → PROCESSING → PENDING_CONFIRMATION
                                             ├─ REJECT → PROCESSING
                                             └─ CONFIRM → RESOLVED → CLOSED
```

效果：非法流转会被拒绝，每次动作都有历史记录，前端和数据库不会绕过业务规则写入任意状态。

### 乐观锁

工单、角色、SLA 等重要数据使用 `version`。更新时版本不一致返回 HTTP 409 和业务码 `30002`。

效果：多人同时操作时不会静默覆盖较早用户的修改，前端需要刷新后由用户决定是否重试。

### SLA 快照

工单创建时根据优先级确定响应和解决截止时间，此后修改 SLA 策略不会篡改历史工单承诺。

效果：历史服务承诺可追溯，扫描器可以幂等标记超时。

### Transactional Outbox

工单事务先把领域事件写入 `outbox_event`，后台发布器再发送到 RabbitMQ。消费者通过事件 ID 去重，并配合重试与死信队列。

效果：RabbitMQ 暂时不可用时不会丢失已经提交的业务事件，重复投递也不会生成重复通知。

### 可观测性与审计

HTTP 请求携带或生成 `X-Trace-Id`，重要操作写入审计日志。日志禁止记录密码、JWT 和敏感工单正文。

效果：接口失败时可以用 TraceId 串联排查，同时保留关键管理操作的责任链。

## 项目结构

```text
flowops/
├─ demo/                    # Spring Boot 后端
│  └─ src/main/java/...     # Controller、Service、Domain、Mapper、安全与消息
├─ web/                     # Vue 3 前端
│  └─ src/
│     ├─ services/          # 按业务模块拆分的 Axios API
│     ├─ stores/            # Pinia 登录状态
│     ├─ views/             # 工单、通知和管理页面
│     └─ assets/style.css   # 全局统一样式
├─ docs/                    # 架构、API、测试、ADR 和实施计划
├─ monitoring/              # Prometheus、Grafana、RabbitMQ 指标配置
├─ compose.yaml             # 完整本地基础设施与可观测性栈
└─ AGENTS.md                # 学习型协作与编码规范
```

## 本地启动

### 环境要求

- JDK 21
- Maven 3.9+
- MySQL 8
- Node.js 20+
- Docker Desktop（启用 RabbitMQ 时需要）

### 1. 数据库与后端配置

创建 MySQL 数据库并按 [数据库设计](docs/02-database-design.md) 初始化表结构，再执行必要的平台权限种子脚本：

```text
docs/sql/01-seed-platform-permissions.sql
```

复制配置模板：

```powershell
Copy-Item demo/src/main/resources/application-local.example.yml demo/src/main/resources/application-local.yml
```

在被 Git 忽略的 `application-local.yml` 中填写本地数据库信息和至少 32 字节的 JWT 密钥。不要把真实配置提交到仓库。

启动后端：

```powershell
cd demo
mvn.cmd spring-boot:run
```

默认地址：`http://localhost:8080/flowOps`。

### 2. 启动前端

```powershell
cd web
npm install
npm run dev
```

打开 `http://localhost:5173`。Vite 会把 `/api` 代理到后端 `/flowOps`。

本地环境默认允许通过 `/register` 创建新企业。注册会自动创建默认部门和首位管理员；生产环境默认关闭公开注册。

### 3. 可选：RabbitMQ

复制环境变量模板并设置本地强密码：

```powershell
Copy-Item .env.example .env
docker compose up -d rabbitmq
docker compose ps
```

然后设置后端环境变量 `FLOWOPS_MESSAGING_ENABLED=true`。详细步骤见 [RabbitMQ Docker 验证](docs/07-rabbitmq-docker-verification.md)。

## 测试

后端：

```powershell
cd demo
mvn.cmd "-Dmaven.repo.local=C:\path\to\flowops\demo\.m2repo" test
```

前端类型检查与生产构建：

```powershell
cd web
npm run build
```

接口闭环测试见 [Postman 使用说明](docs/postman/README.md)。

## 配置与密钥安全

- `.env`、`application-local.yml`、构建目录和本地 Maven 仓库已加入 `.gitignore`；
- 仓库只提交 `.env.example` 和 `application-local.example.yml`；
- JWT 密钥、数据库密码、RabbitMQ 密码必须通过本地文件或环境变量注入；
- 如果真实密钥曾经提交到远程仓库，仅删除文件不够，必须立即轮换密钥并清理 Git 历史。

## 文档导航

- [MVP 架构](docs/01-mvp-architecture.md)
- [数据库设计](docs/02-database-design.md)
- [安全、SLA 与消息方案](docs/03-security-sla-messaging-plan.md)
- [认证与消息测试指南](docs/04-auth-rbac-sla-messaging-test-guide.md)
- [代码阅读路线](docs/05-code-reading-roadmap.md)
- [HTTP API 契约](docs/06-api-contract.md)
- [前端 API 接入清单](docs/09-frontend-api-integration.md)
- [Nexus 工单接入验收](docs/17-nexus-ticket-acceptance.md)
- [Redis 接入计划](docs/10-redis-integration-plan.md)
- [Playwright 与 Flyway 验证](docs/11-playwright-flyway-verification.md)
- [Docker 与配置分层指南](docs/12-docker-configuration-guide.md)
- [CI 自动验收](docs/13-ci-quality-gate.md)
- [监控与健康检查](docs/14-observability-and-health-checks.md)
- [数据可靠性与生产基线](docs/15-data-reliability-and-production-baseline.md)
- [架构决策记录](docs/adr/)

## 当前边界与后续方向

- 数据库结构已通过 Flyway V1/V2 纳入版本化迁移；后续任何变更必须新增 V3+ 脚本，并在临时库和已有库上演练升级与恢复；
- 公开企业注册已有分布式限流，生产环境仍需要验证码、邮箱验证和审核；
- Redis 安全状态当前以 Redis 持久化能力为前提；生产环境应启用 AOF/持久卷，并评估将会话版本持久化到 MySQL；
- 前端仍可继续补充关联对象名称、表单细节和端到端自动化测试；
- 当前保持模块化单体，在业务边界稳定前不拆微服务。

# Nexus 工单接入一期验收

使用一个空的本地 MySQL 8.4 测试库，例如 `flowops_test_nexus`。启动 FlowOps 时将数据源指向该库，确认 Flyway 自动执行 V1～V5。浏览器验收使用当前 Vite 代理地址，因此 FlowOps 后端绑定 `127.0.0.1:8080`，Web 使用 `127.0.0.1:5173`。可按仓库 README 启动所需配套服务。验收使用 MySQL、真实应用和浏览器，测试配置可关闭 Redis、RabbitMQ 和 SLA 扫描，建单时依旧需要启用的 P2 SLA。

## HTTP 与 MySQL

1. 在本机终端使用 `mysql_config_editor set --login-path=flowops-nexus-test --host=127.0.0.1 --user=<测试库用户> --password` 配置测试库登录。脚本连接回环地址，要求库名以 `flowops_test_` 开头，并检查 MySQL 版本和 Flyway V5 的成功记录。
2. 在 FlowOps 根目录运行以下命令。本机需要 `python3`、`mysql` 和支持 BCrypt 的 `htpasswd`。

```bash
export FLOWOPS_ACCEPTANCE_URL=http://127.0.0.1:8080/flowOps
export FLOWOPS_ACCEPTANCE_MYSQL_LOGIN_PATH=flowops-nexus-test
export FLOWOPS_ACCEPTANCE_MYSQL_PORT=3306
export FLOWOPS_ACCEPTANCE_SCHEMA=flowops_test_nexus
export FLOWOPS_ACCEPTANCE_ENV_FILE="$(mktemp -d)/flowops-nexus.env"
python3 tools/acceptance/nexus_tickets.py
```

每次运行会新增两套独立的租户和测试数据。脚本打印两条路由的租户、部门及工单 ID，并检查首次提交、重放、内容冲突、未知路由字段、并发导入、越权拒绝、并发领取和原有接单流程。领取后直接查库，确认只有一条 `CLAIM` 流转和一条 `CLAIM` 分派。

## 工作台

API 验收通过后在同一终端加载脚本生成的本地环境文件，然后运行浏览器用例。

```bash
source "$FLOWOPS_ACCEPTANCE_ENV_FILE"
cd web
npm ci
npm run build
npx playwright install chromium
npm run test:e2e -- nexus-tickets.spec.ts
```

浏览器用例使用真实客服登录，在「待领取」中定位新建工单，点击「领取」，再切换到「我负责的外部工单」检查同一工单号和「已接单」。

## Nexus 联调

联调进程从本地环境读取 `NEXUS_SUPPORT_FLOWOPS_INTEGRATION_ID`、`NEXUS_SUPPORT_FLOWOPS_ENDPOINT` 和 `NEXUS_SUPPORT_FLOWOPS_CREDENTIAL`。测试端点为 `http://127.0.0.1:8080/flowOps/integration/v1/tickets`。验收脚本生成权限为 `0600` 的环境文件并写入上述变量和 FlowOps 侧的 `NEXUS_FLOWOPS_TEST_*` 变量。客服 JWT 按应用配置过期，默认 30 分钟。

首次导入返回 `201`，同一事件及同一内容重放返回 `200`，均使用相同的回执字段。业务码 `30015` 对应接入标识冲突 `409`。导入中遇到可重试的 FlowOps 创建并发冲突时返回 `503/50001`，由 Nexus 使用同一事件重试。客服领取时的乐观锁冲突仍使用 `409/30002`。

# ServiceFlow 企业售后工单管理系统

ServiceFlow（Work Order Management System）是一个面向销售设备、智能硬件或软件产品企业售后团队的工单管理系统，覆盖客户报修、客服建单、工单派发、维修处理、服务评价与运营分析。

> 当前进度：**Stage 6（运营统计与 Redis 缓存）已完成** —— 已落地 17 张业务表、四角色 RBAC、JWT 认证、操作审计、客户与设备档案、工单全生命周期、评价与附件、运营统计（含 Redis 缓存与定时任务），并通过 98 个单元测试与 64 个集成测试。完整功能按 `docs/DEVELOPMENT_PLAN.md` 的阶段划分推进，README 随阶段持续补充。

## 技术栈

| 层次 | 技术 |
| --- | --- |
| 后端 | Java 17、Spring Boot 3.3.5、Spring MVC、MyBatis-Plus 3.5.7、MySQL 8、Redis、Springdoc OpenAPI、JUnit 5 |
| 前端 | Vue 3、JavaScript（ES2022，不使用 TypeScript）、Vite 5、Element Plus、Vue Router、Pinia、Axios、ECharts、Vitest |
| 构建与规范 | Maven（Wrapper）、npm、ESLint、Prettier |

架构与详细设计见 [docs/TECHNICAL_DESIGN.md](docs/TECHNICAL_DESIGN.md)。

> 前端语言说明：本项目前端统一使用 **JavaScript（ES2022）**，不引入 TypeScript。因此前端没有 `npm run type-check` 脚本，代码质量门禁为 `npm run lint` + `npm run test` + `npm run build`；编辑器的路径别名（`@/*`）提示由 `frontend/jsconfig.json` 提供，接口字段含义以 JSDoc 注释记录。

## 环境要求

| 组件 | 版本要求 | 说明 |
| --- | --- | --- |
| JDK | 17 | Spring Boot 3.x 的最低要求 |
| Maven | 3.10.0 | 也可使用项目内置的 Maven Wrapper（`backend/mvnw`） |
| Node.js | 20+ | 本项目在 Node.js 24.21.0 上验证通过 |
| MySQL | 8.x | 需要两个库：`serviceflow`（开发）、`serviceflow_test`（集成测试） |
| Redis | 5.0+ | 用于缓存与并发锁（本项目当前在 Windows 版 5.0.14.1 上验证） |

## 快速开始

### 1. 初始化数据库

```powershell
mysql -u root -p -e "CREATE DATABASE IF NOT EXISTS serviceflow DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci; CREATE DATABASE IF NOT EXISTS serviceflow_test DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;"
```

### 2. 启动 Redis

推荐注册为 Windows 服务常驻运行（开机自启，无需每次手动启动）：

```powershell
cd "D:\Tools\Redis\Redis-x64-5.0.14.1"
.\redis-server.exe --service-install redis.windows.conf --loglevel verbose   # 仅首次安装
.\redis-server.exe --service-start
redis-cli ping   # 期望输出 PONG
```

也可以前台手动启动，此时务必显式指定 `--dir` 与 `--logfile`（否则 RDB 可能写失败，日志也可能占用项目目录）：

```powershell
Set-Location "D:\Tools\Redis\Redis-x64-5.0.14.1"
.\redis-server.exe redis.windows.conf --dir "D:\Tools\Redis\Redis-x64-5.0.14.1" --logfile "D:\Tools\Redis\Redis-x64-5.0.14.1\redis-server.log"
redis-cli ping   # 期望输出 PONG
```

启动后建议确认持久化与数据目录正常：

```powershell
redis-cli config get dir    # 期望输出 Redis 安装目录
redis-cli bgsave
redis-cli info persistence | Select-String rdb_last_bgsave_status   # 期望 ok
```

> 排错提示：如果 `redis-cli` 返回 `MISCONF Redis is configured to save RDB snapshots, but it is currently not able to persist on disk`，
> 说明该实例的快照写入失败，此时**所有写命令（含缓存与令牌黑名单）都会被拒绝**，而连接与 `PING` 仍然正常，容易被误判为"Redis 正常"。
> 处理方式：以安装目录为工作目录并用 `--dir` 指向可写目录重启 Redis，然后执行 `redis-cli bgsave` 确认 `rdb_last_bgsave_status:ok`。

### 3. 启动后端

数据库密码通过环境变量注入，不写入配置文件、不提交到仓库（变量清单见 `.env.example`）。

```powershell
$env:JAVA_HOME = "D:\Tools\envs\jdk17"      # 指向本机 JDK 17
$env:DB_PASSWORD = "你的本地开发数据库密码"
$env:DB_NAME = "serviceflow"

cd backend
.\mvnw.cmd spring-boot:run
```

> 首次启动前请按 [docs/DATABASE_DESIGN.md](docs/DATABASE_DESIGN.md) 第 7 节依次执行 `db/schema.sql`、`db/data.sql` 与 `db/demo-data.sql`（演示数据仅用于本地开发）。

后端默认端口 `8080`，启动后：

- 健康检查：<http://localhost:8080/api/v1/health>（会实际探测 MySQL 与 Redis）
- 接口文档：<http://localhost:8080/swagger-ui.html>
- OpenAPI JSON：<http://localhost:8080/v3/api-docs>

### 4. 启动前端

```powershell
cd frontend
npm install
npm run dev
```

访问 <http://localhost:5173/>。开发服务器会把 `/api` 请求代理到 `http://127.0.0.1:8080`，因此本地开发无需配置跨域。

## 测试与构建

```powershell
# 后端：单元测试（仅依赖 JVM，不需要数据库与 Redis）
cd backend
.\mvnw.cmd clean test

# 后端：单元测试 + 集成测试（需要本地 MySQL 与 Redis 已启动）
.\mvnw.cmd clean verify

# 后端：打包（同时执行单元测试）
.\mvnw.cmd package

# 前端：规范检查、单元测试、构建（本项目不使用 TypeScript，无类型检查步骤）
cd frontend
npm run lint
npm run test
npm run build
```

### 演示账号

本地开发演示账号统一密码 `Serviceflow@2026`（明文仅用于本地联调，库中以 BCrypt 哈希存储；完整说明见 [docs/DATABASE_DESIGN.md](docs/DATABASE_DESIGN.md) 第 6.1 节）。

| 账号 | 角色 | 说明 |
| --- | --- | --- |
| `admin` | 系统管理员 | 全部权限 |
| `service01` | 售后客服 | 客户、设备、工单创建与分派 |
| `engineer01` / `engineer02` | 维修工程师 | 接单与维修处理 |
| `customer01` / `customer02` | 客户 | 查看本人工单、确认与评价 |

## 目录结构

```
.
├── backend/         Spring Boot 后端工程
├── frontend/        Vue 3 + JavaScript 前端工程
├── docs/            开发计划、技术设计、Git 工作流、环境检查报告
├── .env.example     环境变量示例（仅占位值）
└── README.md
```

## 已知环境说明

- **附件存储目录**：默认 `backend/storage/attachments`（可通过环境变量 `APP_UPLOAD_DIR` 修改），已在 `.gitignore` 中排除，不会进入版本库；上传扩展名与大小上限由 `sys_config` 的 `attachment.allowed_extensions`、`attachment.max_size_mb` 控制。

- **Redis 版本**：需求文档声明 Redis 7，本机实际使用 Windows 版 5.0.14.1（Windows 无官方 Redis 7），系统仅使用 5.0 已支持的命令，功能不受影响。
- **npm 缓存目录**：本机全局缓存目录 `D:\Tools\envs\Nodejs-v24.21.0\node_cache` 对当前用户只读，直接执行 `npm install` 会报 `EPERM`。临时规避方式是指定项目内缓存：

  ```powershell
  npm install --cache ./.npm-cache
  ```

- **GitHub 网络**：本机直连 `github.com:443` 不可用，推送需经本地代理（例如 `127.0.0.1:7897`）。

## 文档索引

| 文档 | 内容 |
| --- | --- |
| [docs/DEVELOPMENT_PLAN.md](docs/DEVELOPMENT_PLAN.md) | 阶段划分、验收标准与风险清单 |
| [docs/TECHNICAL_DESIGN.md](docs/TECHNICAL_DESIGN.md) | 架构、数据库、状态机、权限、API 与缓存设计 |
| [docs/GIT_WORKFLOW.md](docs/GIT_WORKFLOW.md) | 分支策略、提交规范与双远程同步流程 |
| [docs/STAGE0_ENVIRONMENT_CHECK.md](docs/STAGE0_ENVIRONMENT_CHECK.md) | 环境检查记录与处理结果 |
| [docs/DATABASE_DESIGN.md](docs/DATABASE_DESIGN.md) | 数据库表结构、约束、索引、演示账号与初始化步骤 |

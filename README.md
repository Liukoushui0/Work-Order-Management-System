# ServiceFlow 企业售后工单管理系统

ServiceFlow（Work Order Management System）是一个面向销售设备、智能硬件或软件产品企业售后团队的工单管理系统，覆盖客户报修、客服建单、工单派发、维修处理、服务评价与运营分析。

> 当前进度：**Stage 1（工程骨架与基础设施）已完成**。完整功能按 `docs/DEVELOPMENT_PLAN.md` 的阶段划分推进，README 会随阶段推进持续补充。

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

```powershell
# 以本机 Redis 安装目录为例
& "D:\Tools\Redis\Redis-x64-5.0.14.1\redis-server.exe" "D:\Tools\Redis\Redis-x64-5.0.14.1\redis.windows.conf"
redis-cli ping   # 期望输出 PONG
```

### 3. 启动后端

数据库密码通过环境变量注入，不写入配置文件、不提交到仓库（变量清单见 `.env.example`）。

```powershell
$env:JAVA_HOME = "D:\Tools\envs\jdk17"      # 指向本机 JDK 17
$env:DB_PASSWORD = "你的本地开发数据库密码"
$env:DB_NAME = "serviceflow"

cd backend
.\mvnw.cmd spring-boot:run
```

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
# 后端：单元测试（默认不执行集成测试）
cd backend
.\mvnw.cmd clean test

# 后端：打包
.\mvnw.cmd package

# 前端：类型检查、规范检查、单元测试、构建
cd frontend
npm run lint
npm run test
npm run build
```

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

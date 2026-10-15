# ServiceFlow 企业售后工单管理系统

ServiceFlow（Work Order Management System）是面向销售设备、智能硬件或软件产品企业售后团队的工单管理系统，覆盖**客户报修 → 客服建单 → 工单派发 → 维修处理 → 客户确认 → 服务评价 → 运营分析**的完整业务流程。

项目形态：模块化单体 + 前后端分离，不使用 Docker，全部可在本地 Windows 环境直接运行。

## 功能概览

- **认证与权限**：JWT 登录、退出登录令牌黑名单、修改密码、四角色 RBAC（后端三层校验：接口级 / 资源级 / 数据域）。
- **客户与设备档案**：客户建档与维护、设备档案与保修信息、客户数据域隔离。
- **工单全生命周期**：唯一编号生成、SLA 截止时间、分派、接单、转派、退回、维修处理、提交结果、客户确认、退回重做、重新打开、取消；状态机与处理历史可追溯。
- **评价与附件**：一单一评（仅已完成、仅所属客户、支持匿名）、附件上传下载删除（类型与大小白名单、SHA-256、路径穿越防护、按工单权限校验）。
- **运营统计**：总览指标、工单趋势（日/周/月）、故障类型与优先级分布、工程师工作量、客户满意度；Redis 缓存 + 事务提交后失效 + 不可用降级。
- **定时任务**：超期自动确认（默认 72 小时）与每小时逾期提醒日志。
- **审计**：关键操作写入操作日志，参数脱敏，traceId 贯穿请求与日志。

## 技术栈

| 层次 | 技术 |
| --- | --- |
| 后端 | Java 17、Spring Boot 3.3.5、Spring MVC、Spring Security、MyBatis-Plus 3.5.7、MySQL 8、Redis、Springdoc OpenAPI 2.6.0、JUnit 5 + Mockito |
| 前端 | Vue 3、JavaScript（ES2022，不使用 TypeScript）、Vite 5、Element Plus 2.11.9、Vue Router、Pinia、Axios、ECharts 5.5.1、Vitest + Vue Test Utils、ESLint + Prettier |
| 构建 | Maven 3.10.0（含 Wrapper）、npm |

## 环境要求

| 组件 | 版本 | 说明 |
| --- | --- | --- |
| JDK | 17（LTS） | Spring Boot 3.x 最低要求，本项目在 Temurin 17.0.20.1 验证 |
| Maven | 3.10.0 | 也可使用内置 `backend/mvnw.cmd` |
| Node.js | 20+ | 本项目在 Node.js 24.21.0 + npm 11 验证 |
| MySQL | 8.x | 需要 `serviceflow`（开发）与 `serviceflow_test`（集成测试）两个库 |
| Redis | 5.0+ | 缓存、令牌黑名单与并发锁；本项目在 Windows 5.0.14.1 验证 |

## 1. 安装与配置 JDK 17

1. 下载 Temurin 17（Windows x64，`.zip` 免安装版最省事）。
2. 解压到固定目录，例如 `D:\Tools\envs\jdk17`，确认存在 `bin\java.exe` 与 `bin\javac.exe`。
3. 验证：

```powershell
& "D:\Tools\envs\jdk17\bin\java.exe" -version    # 期望 17.x
```

> 本项目不要求修改系统全局 `JAVA_HOME`，启动与构建时按下方命令显式指定即可。

## 2. 安装与配置 Maven

1. 下载 Apache Maven 3.10.0 并解压，例如 `D:\Tools\envs\Maven\apache-maven-3.10.0`。
2. 让 Maven 使用 JDK 17（否则 Spring Boot 3 无法编译）：

```powershell
$env:JAVA_HOME = "D:\Tools\envs\jdk17"
& "D:\Tools\envs\Maven\apache-maven-3.10.0\bin\mvn.cmd" -v   # Java version 应为 17.x
```

3. 国内网络建议在 `conf\settings.xml` 配置阿里云镜像（本项目使用的即 `https://maven.aliyun.com/repository/public`）。
4. 也可直接使用内置 Wrapper：`backend\mvnw.cmd`（首次运行会下载 Maven 3.10.0 到 `~/.m2/wrapper`）。

## 3. 安装与配置 Node.js

1. 安装 Node.js 20 及以上（本项目验证 24.21.0）与随附 npm，验证 `node -v`、`npm -v`。
2. 国内网络建议切换 registry（本项目使用 npmmirror）：

```powershell
npm config set registry https://registry.npmmirror.com
```

## 4. 安装 MySQL 8 并初始化数据库

1. 安装 MySQL 8（本项目验证 8.0.46），确认服务已启动（Windows 服务名示例 `MySQL80`）。
2. 创建开发库与测试库：

```powershell
mysql -u root -p -e "CREATE DATABASE IF NOT EXISTS serviceflow DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci; CREATE DATABASE IF NOT EXISTS serviceflow_test DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;"
```

3. 依次执行三个脚本（结构 → 基础数据 → 演示数据）。开发库与测试库都要执行：

```powershell
cd "E:\Work Order Management System"
mysql -u root -p serviceflow      -e "source backend/src/main/resources/db/schema.sql"
mysql -u root -p serviceflow      -e "source backend/src/main/resources/db/data.sql"
mysql -u root -p serviceflow      -e "source backend/src/main/resources/db/demo-data.sql"
mysql -u root -p serviceflow_test -e "source backend/src/main/resources/db/schema.sql"
mysql -u root -p serviceflow_test -e "source backend/src/main/resources/db/data.sql"
mysql -u root -p serviceflow_test -e "source backend/src/main/resources/db/demo-data.sql"
```

三个脚本均可重复执行（结构用 `CREATE TABLE IF NOT EXISTS`，数据用 `INSERT ... ON DUPLICATE KEY UPDATE`）。

## 5. 安装 Redis 7 并启动服务

Windows 没有官方 Redis 7，可选 WSL2 安装官方版本、Memurai，或 Windows 移植版。本项目在移植版 5.0.14.1 上验证：

```powershell
cd "D:\Tools\Redis\Redis-x64-5.0.14.1"
.\redis-server.exe --service-install redis.windows.conf --loglevel verbose   # 仅首次注册服务
.\redis-server.exe --service-start
redis-cli ping    # 期望 PONG
```

如需前台运行，**必须显式指定数据目录与日志文件**：

```powershell
.\redis-server.exe redis.windows.conf --dir "D:\Tools\Redis\Redis-x64-5.0.14.1" --logfile "D:\Tools\Redis\Redis-x64-5.0.14.1\redis-server.log"
```

启动后建议自检持久化（详见"常见错误与排查"第 3 条）：

```powershell
redis-cli config get dir
redis-cli bgsave
redis-cli info persistence | Select-String rdb_last_bgsave_status   # 期望 ok
```

## 6. 配置后端环境变量

数据库密码、JWT 密钥、Redis 密码均通过环境变量注入，**不写入仓库**。完整清单见 `.env.example`：

| 变量 | 默认值 | 说明 |
| --- | --- | --- |
| `DB_HOST` / `DB_PORT` / `DB_NAME` | 127.0.0.1 / 3306 / serviceflow | 数据库连接 |
| `DB_USERNAME` / `DB_PASSWORD` | root / 空 | 本地开发账号，必须显式设置密码 |
| `REDIS_HOST` / `REDIS_PORT` / `REDIS_PASSWORD` | 127.0.0.1 / 6379 / 空 | Redis 连接 |
| `JWT_SECRET` | 空 | 留空时启动生成一次性随机密钥（重启后旧令牌失效）；生产必须显式配置 |
| `APP_UPLOAD_DIR` | ./storage/attachments | 附件存储目录 |
| `SPRING_PROFILES_ACTIVE` | dev | 使用 `application-dev.yml` |

## 7. 启动 Spring Boot 后端

```powershell
$env:JAVA_HOME = "D:\Tools\envs\jdk17"
$env:DB_PASSWORD = "你的本地数据库密码"
$env:DB_NAME = "serviceflow"

cd "E:\Work Order Management System\backend"
.\mvnw.cmd spring-boot:run
# 或运行已打包的 jar：
# & "D:\Tools\envs\jdk17\bin\java.exe" -jar target\serviceflow-backend-1.0.0-SNAPSHOT.jar
```

启动成功后访问健康检查 <http://localhost:8080/api/v1/health>，期望返回 `"database":"UP","redis":"UP"`。

## 8. 启动 Vue 前端

```powershell
cd "E:\Work Order Management System\frontend"
npm install
npm run dev
```

访问 <http://localhost:5173/>。开发服务器将 `/api` 代理到 `http://127.0.0.1:8080`，本地无需配置跨域。

生产构建：`npm run build` 产出 `frontend/dist`，可由任意静态服务器托管（接口地址通过 `VITE_API_BASE_URL` 指定）。

## 9. 访问系统与 API 文档

| 入口 | 地址 |
| --- | --- |
| 前端页面 | <http://localhost:5173/> |
| 健康检查 | <http://localhost:8080/api/v1/health> |
| Swagger UI | <http://localhost:8080/swagger-ui.html> |
| OpenAPI JSON | <http://localhost:8080/v3/api-docs> |

## 10. 演示账号与角色权限

本地开发演示账号统一密码 **`Serviceflow@2026`**（库中以 BCrypt 哈希存储，明文仅用于本地联调；生产环境务必删除这些账号）。登录页在开发服务器下会展示一键填充按钮。

| 账号 | 角色 | 主要权限 |
| --- | --- | --- |
| `admin` | `ROLE_ADMIN` | 用户/角色/故障分类/系统配置/操作日志，全局统计与全部业务操作 |
| `service01` | `ROLE_SERVICE` | 客户与设备维护、建单、分派/转派/退回/取消、全局统计 |
| `engineer01` / `engineer02` | `ROLE_ENGINEER` | 仅本人负责工单：接单、处理记录、提交结果、转派；设备只读；个人统计 |
| `customer01` / `customer02` | `ROLE_CUSTOMER` | 仅自身客户档案与设备、自身工单：确认服务结果、评价 |

## 11. 测试与构建

```powershell
# 后端：单元测试（仅依赖 JVM，不需要数据库与 Redis）
cd backend
$env:JAVA_HOME = "D:\Tools\envs\jdk17"
.\mvnw.cmd clean test

# 后端：单元 + 集成测试（需要本地 MySQL 与 Redis 已启动，集成测试使用 serviceflow_test 库）
$env:DB_PASSWORD = "你的本地数据库密码"
.\mvnw.cmd clean verify

# 后端：打包
.\mvnw.cmd package

# 前端：规范检查、单元测试、构建（本项目不使用 TypeScript，因此没有类型检查步骤）
cd ..\frontend
npm run lint
npm run test
npm run build
```

## 12. 常见启动错误与排查

1. **`mvn` 提示 Java 版本过低 / 编译报 `invalid target release: 17`**：Maven 仍在用 JDK 8。执行 `mvn -v` 确认 `Java version` 为 17.x，必要时先设置 `$env:JAVA_HOME`。
2. **健康检查 `"database":"DOWN"`**：确认 MySQL 服务已启动、`DB_PASSWORD` 已设置、且已执行 `schema.sql`；应用日志会打印具体连接错误。
3. **Redis 返回 `MISCONF ... not able to persist on disk`**：该实例 RDB 快照写入失败，此时**所有写命令（缓存、令牌黑名单）都会被拒绝**，而 `PING` 仍然正常，容易被误判为"Redis 正常"。处理方式：以安装目录为工作目录并用 `--dir` 指向可写目录重启，然后 `redis-cli bgsave` 确认 `rdb_last_bgsave_status:ok`。
4. **`npm install` 报 `EPERM ... node_cache`**：全局 npm 缓存目录对当前用户不可写。临时规避：`npm install --cache ./.npm-cache`；彻底解决需以管理员调整该目录权限。
5. **提交/推送需反复输入凭据或 `Github 无法访问`**：GitHub 在部分网络下需要代理（例如 `git -c http.proxy=http://127.0.0.1:7897 push github main`）；Gitee 可直连。
6. **上传附件报 400/1503 或 1504**：扩展名不在白名单或超过大小上限，可通过 `sys_config` 的 `attachment.allowed_extensions`、`attachment.max_size_mb` 调整。
7. **接口返回 401/1004**：令牌缺失、过期或已退出登录（黑名单命中）。前端会自动清理本地令牌并跳回登录页。
8. **端口占用**：后端默认 8080，前端默认 5173；被占用时分别修改 `application.yml` 的 `server.port` 与 `vite.config.js` 的 `server.port`。

## 13. 文档索引

| 文档 | 内容 |
| --- | --- |
| [docs/DEVELOPMENT_PLAN.md](docs/DEVELOPMENT_PLAN.md) | 阶段划分、验收标准与风险清单 |
| [docs/TECHNICAL_DESIGN.md](docs/TECHNICAL_DESIGN.md) | 架构、技术栈、状态机、权限、API 与缓存设计 |
| [docs/DATABASE_DESIGN.md](docs/DATABASE_DESIGN.md) | 17 张表结构、约束、索引、演示数据与初始化步骤 |
| [docs/API_DOCUMENTATION.md](docs/API_DOCUMENTATION.md) | 接口清单（按模块）、统一响应、错误码与权限要求 |
| [docs/LOCAL_DEPLOYMENT.md](docs/LOCAL_DEPLOYMENT.md) | 本地部署步骤与故障排查 |
| [docs/TEST_REPORT.md](docs/TEST_REPORT.md) | 测试命令与真实结果 |
| [docs/STAGE8_ACCEPTANCE.md](docs/STAGE8_ACCEPTANCE.md) | 阶段 8 验收与安全审查报告 |
| [docs/GIT_WORKFLOW.md](docs/GIT_WORKFLOW.md) | 分支策略、提交规范与双远程同步流程 |
| [docs/STAGE0_ENVIRONMENT_CHECK.md](docs/STAGE0_ENVIRONMENT_CHECK.md) | 环境检查记录与处理结果 |
| [docs/FINAL_DELIVERY.md](docs/FINAL_DELIVERY.md) | 最终交付报告 |

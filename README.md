# ServiceFlow 企业售后工单管理系统

面向销售设备、智能硬件或软件产品企业售后团队的工单管理系统，覆盖 **客户报修 → 客服建单 → 工单派发 → 维修处理 → 客户确认 → 服务评价 → 运营分析** 的完整业务流程。

**技术栈**：Java 17 · Spring Boot 3.3 · Spring Security + JWT · MyBatis-Plus · MySQL 8 · Redis · Vue 3 · Vite · Element Plus · ECharts
**形态**：模块化单体 + 前后端分离，不使用 Docker，可在本地 Windows 环境直接运行
**状态**：全部 10 个阶段（Stage 0~9）已完成并通过验收 · 后端 163 个自动化测试 + 前端 25 个测试全部通过 · GitHub 与 Gitee 双远程同步

## 1. 项目状态

| 项 | 数据 |
| --- | --- |
| 代码规模 | 后端 158 个文件 / 8353 行；测试 28 个文件 / 3461 行；前端 40 个文件 / 4889 行；SQL 687 行 |
| 数据库 | 17 张表（工单、分派、处理记录、评论、评价、附件、审计、用户/角色/权限、故障分类、配置、序列表） |
| 接口 | 14 个控制器 / 67 个 REST 端点，统一 `/api/v1` 前缀与统一响应结构 |
| 测试 | 后端 163（98 单元 + 65 集成，集成测试连接真实 MySQL 与 Redis）；前端 25 |
| 提交 | 54 个 Conventional Commits（feat 13 / docs 19 / test 6 / fix 5 / build 2 / style 1 / chore 1 / merge 7，分阶段 feature 分支 + `--no-ff` 合并） |
| 文档 | 11 份（开发计划、技术设计、数据库设计、API、部署排查、测试报告、验收与安全审查、简历素材等） |

开发过程严格按阶段推进，每阶段"开发 → 测试 → 审查 → 提交 → 合并 → 双远程同步 → 汇报"，进度与验收记录见 [docs/DEVELOPMENT_PLAN.md](docs/DEVELOPMENT_PLAN.md)。

## 2. 功能概览

| 模块 | 能力 |
| --- | --- |
| 认证与个人中心 | JWT 登录、退出（令牌黑名单）、刷新令牌、修改密码、个人资料；BCrypt 存储、失败 5 次锁定 15 分钟（阈值可配置） |
| 用户/角色/权限 | 用户增改启停、分配角色、重置密码；4 角色 / 30 权限 / 65 条授权；故障分类与系统配置管理；操作日志查询 |
| 客户资料 | 建档、检索（关键词/等级/状态）、详情（设备数/工单数）、修改、启停、删除（含关联保护） |
| 设备档案 | 建档（编号与序列号唯一）、检索、详情（保修状态/工单数）、修改（归属变更校验）、删除（有工单时拒绝） |
| 售后工单 | 唯一编号（`SO+yyyyMMdd+6 位`）、SLA 截止时间、8 类条件筛选 + 分页 + 排序白名单、详情、逾期查询 |
| 分派/接单/转派/维修 | 分派、接单、转派、退回、处理记录、提交结果；状态机 + 条件更新 + Redis 锁 + 唯一索引四重保护 |
| 处理记录与评价 | 处理历史时间线（操作人/角色/前后状态/内容/工时）；一单一评、仅已完成、仅所属客户、支持匿名 |
| 工单附件 | 上传/列表/下载/删除；白名单、文件名清洗、UUID 存储名、SHA-256、路径穿越防护、按工单权限校验 |
| 超时提醒与审计 | 超期自动确认（默认 72 小时，写 `AUTO_CONFIRM` 记录）；每小时逾期提醒；关键操作审计（参数脱敏 + traceId） |
| 运营统计 | 总览、趋势（日/周/月）、故障类型分布、优先级分布、工程师工作量、客户满意度；前端 ECharts 使用真实数据 |
| Redis 缓存 | 统计缓存（键含数据范围隔离、60/300/600 秒 TTL）、事务提交后失效、不可用降级；令牌黑名单；接单分布式锁 |
| 系统基础能力 | 统一响应与错误码（7 区间）、全局异常处理、traceId 与结构化日志、系统配置项、定时任务开关 |

前端共 13 个业务页面 + 403/404：登录、运营仪表盘、工单列表/创建/详情/处理工作台、客户管理、设备档案、服务评价、用户管理、角色与权限、系统配置、操作日志、个人中心。

## 3. 技术栈

| 层次 | 技术选型 |
| --- | --- |
| 后端 | Java 17、Spring Boot 3.3.5、Spring MVC、Spring Security 6.3（JWT 无状态认证）、MyBatis-Plus 3.5.7、MySQL 8、Redis（Lettuce）、Springdoc OpenAPI 2.6.0、jjwt 0.12.6、Lombok |
| 前端 | Vue 3.5、JavaScript（ES2022）、Vite 5.4、Element Plus 2.11.9、Vue Router 4.4、Pinia 2.2、Axios 1.7、ECharts 5.5 |
| 测试 | JUnit 5 + Mockito + Spring Boot Test（Surefire 单元 / Failsafe 集成）、Vitest 2.1 + Vue Test Utils |
| 工程 | Maven 3.10（含 Wrapper）、npm 11、ESLint 9（flat config）+ Prettier、Git（GitHub + Gitee 双远程） |

## 4. 架构与数据流

```
┌─────────────────────── 浏览器（Vue 3 SPA）───────────────────────┐
│  页面 / 通用组件 / Pinia Store / 路由守卫 / ECharts 图表          │
│  Axios 封装：统一 baseURL、token 注入、错误归一化                  │
└───────────────────────────┬──────────────────────────────────────┘
                            │  /api/v1（开发环境经 Vite 代理）
┌───────────────────────────▼──────────────────────────────────────┐
│                        Spring Boot 应用                           │
│  Controller（参数校验 + @PreAuthorize 接口级鉴权）                 │
│  Service（业务规则 / 状态机 / 事务 / 缓存 / AccessGuard 资源级鉴权）│
│  DataScopeService（数据域收敛）    AOP 操作审计切面（脱敏 + traceId）│
│  Mapper（MyBatis-Plus）            Security（JWT 过滤器 + 令牌黑名单）│
└──────────┬────────────────────────────────┬──────────────────────┘
           │                                │
   ┌───────▼────────┐              ┌────────▼────────┐
   │ MySQL 8（17 表）│              │ Redis（缓存/锁/黑名单）│
   └────────────────┘              └─────────────────┘
```

**请求处理链路**：traceId 过滤器 → JWT 认证过滤器（校验签名/有效期/黑名单并加载角色权限）→ Controller 参数校验 → 接口级鉴权 → Service 业务校验（状态机 / 资源关系 / 数据域）→ 条件更新或查询 → 事务提交后清理缓存 → 统一响应包装。

## 5. 目录结构

```
Work Order Management System/
├── backend/                       Spring Boot 后端
│   ├── mvnw / mvnw.cmd / .mvn/     Maven Wrapper（锁定 Maven 3.10.0）
│   ├── src/main/java/com/serviceflow/
│   │   ├── controller/  service/  mapper/  entity/  dto/  vo/
│   │   ├── security/    config/   exception/  common/  aspect/
│   │   └── ServiceFlowApplication.java
│   ├── src/main/resources/
│   │   ├── application.yml / application-dev.yml
│   │   ├── db/schema.sql  db/data.sql  db/demo-data.sql
│   │   └── logback-spring.xml
│   └── src/test/java/...         单元测试 + 集成测试（*IntegrationTest）
├── frontend/                      Vue 3 前端
│   ├── vite.config.js  jsconfig.json  eslint.config.js
│   └── src/
│       ├── api/  components/  layouts/  router/  stores/  styles/  utils/
│       └── views/（ticket/ customer/ device/ system/ 其余单页）
├── docs/                          11 份项目文档
├── .env.example                   环境变量示例（仅占位值）
└── README.md
```

## 6. 项目亮点

- **工单状态机 + 并发保护**：6 状态、10 类流转动作，「状态 + 版本号」条件更新 + Redis 锁 + 唯一索引三层保证一致性；4 线程并发接单仅 1 次成功，重复操作返回明确业务码。
- **并发建单死锁的定位与重构**：发现"查最大编号 + 唯一索引重试"在 REPEATABLE READ 下读到同一快照导致重试风暴与死锁，改为序列表原子取号，5 线程并发建单编号唯一且无重试。
- **三层权限校验**：接口级 + 资源级 + 数据域自动收敛，集成测试覆盖"改 ID 越权""跨客户访问""非负责人操作"等场景。
- **事务缺陷修复**：登录失败计数若与登录同事务会被回滚导致锁永不生效，改为独立事务写入并补测试守护。
- **缓存工程化**：键含数据范围防串数据、写操作在事务提交后失效、Redis 不可用自动降级，均有测试覆盖。
- **前端体积优化**：`manualChunks` + 路由懒加载把主包从 1.1 MB 降至 51 KB（gzip 21 KB）。

> 完整的亮点清单、技术难点详解（问题→分析→方案→结果）与面试问答素材见 [docs/RESUME_PROJECT.md](docs/RESUME_PROJECT.md)。

## 7. 环境要求

| 组件 | 版本 | 说明 |
| --- | --- | --- |
| JDK | 17（LTS） | Spring Boot 3.x 最低要求，本项目在 Temurin 17.0.20.1 验证 |
| Maven | 3.10.0 | 也可使用内置 `backend/mvnw.cmd` |
| Node.js | 20+ | 本项目在 Node.js 24.21.0 + npm 11 验证 |
| MySQL | 8.x | 需要 `serviceflow`（开发）与 `serviceflow_test`（集成测试）两个库 |
| Redis | 5.0+ | 缓存、令牌黑名单与并发锁；本项目在 Windows 5.0.14.1 验证 |

## 8. 安装与配置 JDK 17

1. 下载 Temurin 17（Windows x64，`.zip` 免安装版最省事）。
2. 解压到固定目录，例如 `D:\Tools\envs\jdk17`，确认存在 `bin\java.exe` 与 `bin\javac.exe`。
3. 验证：

```powershell
& "D:\Tools\envs\jdk17\bin\java.exe" -version    # 期望 17.x
```

> 本项目不要求修改系统全局 `JAVA_HOME`，启动与构建时按下方命令显式指定即可。

## 9. 安装与配置 Maven

1. 下载 Apache Maven 3.10.0 并解压，例如 `D:\Tools\envs\Maven\apache-maven-3.10.0`。
2. 让 Maven 使用 JDK 17（否则 Spring Boot 3 无法编译）：

```powershell
$env:JAVA_HOME = "D:\Tools\envs\jdk17"
& "D:\Tools\envs\Maven\apache-maven-3.10.0\bin\mvn.cmd" -v   # Java version 应为 17.x
```

3. 国内网络建议在 `conf\settings.xml` 配置阿里云镜像（本项目使用的即 `https://maven.aliyun.com/repository/public`）。
4. 也可直接使用内置 Wrapper：`backend\mvnw.cmd`（首次运行会下载 Maven 3.10.0 到 `~/.m2/wrapper`）。

## 10. 安装与配置 Node.js

1. 安装 Node.js 20 及以上（本项目验证 24.21.0）与随附 npm，验证 `node -v`、`npm -v`。
2. 国内网络建议切换 registry（本项目使用 npmmirror）：

```powershell
npm config set registry https://registry.npmmirror.com
```

## 11. 安装 MySQL 8 并初始化数据库

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

三个脚本均可重复执行（结构用 `CREATE TABLE IF NOT EXISTS`，数据用 `INSERT ... ON DUPLICATE KEY UPDATE`）；执行后应得到 17 张表、6 个演示账号、30 项权限、65 条角色授权、4 个客户、8 台设备、8 张演示工单。

## 12. 安装 Redis 并启动服务

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

启动后建议自检持久化（详见第 17 节第 3 条）：

```powershell
redis-cli config get dir
redis-cli bgsave
redis-cli info persistence | Select-String rdb_last_bgsave_status   # 期望 ok
```

## 13. 配置后端环境变量

数据库密码、JWT 密钥、Redis 密码均通过环境变量注入，**不写入仓库**。完整清单见 `.env.example`：

| 变量 | 默认值 | 说明 |
| --- | --- | --- |
| `DB_HOST` / `DB_PORT` / `DB_NAME` | 127.0.0.1 / 3306 / serviceflow | 数据库连接 |
| `DB_USERNAME` / `DB_PASSWORD` | root / 空 | 本地开发账号，必须显式设置密码 |
| `REDIS_HOST` / `REDIS_PORT` / `REDIS_PASSWORD` | 127.0.0.1 / 6379 / 空 | Redis 连接 |
| `JWT_SECRET` | 空 | 留空时启动生成一次性随机密钥（重启后旧令牌失效）；生产必须显式配置 |
| `APP_UPLOAD_DIR` | ./storage/attachments | 附件存储目录 |
| `SPRING_PROFILES_ACTIVE` | dev | 使用 `application-dev.yml` |

## 14. 启动后端与前端

```powershell
# 后端（默认端口 8080）
$env:JAVA_HOME = "D:\Tools\envs\jdk17"
$env:DB_PASSWORD = "你的本地数据库密码"
$env:DB_NAME = "serviceflow"
cd "E:\Work Order Management System\backend"
.\mvnw.cmd spring-boot:run
# 或运行已打包的 jar：
# & "D:\Tools\envs\jdk17\bin\java.exe" -jar target\serviceflow-backend-1.0.0-SNAPSHOT.jar

# 前端（默认端口 5173，另开一个终端）
cd "E:\Work Order Management System\frontend"
npm install
npm run dev
```

前端开发服务器会把 `/api` 代理到 `http://127.0.0.1:8080`，本地无需配置跨域；生产构建 `npm run build` 产出 `frontend/dist`，接口地址通过 `VITE_API_BASE_URL` 指定。

## 15. 访问入口与演示账号

| 入口 | 地址 |
| --- | --- |
| 前端页面 | <http://localhost:5173/> |
| 健康检查 | <http://localhost:8080/api/v1/health>（实际探测 MySQL 与 Redis） |
| Swagger UI | <http://localhost:8080/swagger-ui.html> |
| OpenAPI JSON | <http://localhost:8080/v3/api-docs> |

本地开发演示账号统一密码 **`Serviceflow@2026`**（库中以 BCrypt 哈希存储，明文仅用于本地联调；生产环境务必删除这些账号）。登录页在开发服务器下提供一键填充按钮。

| 账号 | 角色 | 主要权限 |
| --- | --- | --- |
| `admin` | `ROLE_ADMIN` | 用户/角色/故障分类/系统配置/操作日志，全局统计与全部业务操作（含代客户评价） |
| `service01` | `ROLE_SERVICE` | 客户与设备维护、建单、分派/转派/退回/取消、全局统计 |
| `engineer01` / `engineer02` | `ROLE_ENGINEER` | 仅本人负责工单：接单、处理记录、提交结果、转派；设备只读；个人统计 |
| `customer01` / `customer02` | `ROLE_CUSTOMER` | 仅自身客户档案与设备、自身工单：确认服务结果、评价（可匿名） |

## 16. 测试与构建

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

当前结果：后端 **163 个测试全部通过**（98 单元 + 65 集成），前端 **25 个测试全部通过**，前后端均可构建。测试分布、用例清单与端到端验收记录见 [docs/TEST_REPORT.md](docs/TEST_REPORT.md)。

## 17. 常见启动错误与排查

1. **`mvn` 提示 Java 版本过低 / 编译报 `invalid target release: 17`**：Maven 仍在用 JDK 8。执行 `mvn -v` 确认 `Java version` 为 17.x，必要时先设置 `$env:JAVA_HOME`。
2. **健康检查 `"database":"DOWN"`**：确认 MySQL 服务已启动、`DB_PASSWORD` 已设置、且已执行 `schema.sql`；应用日志会打印具体连接错误。
3. **Redis 返回 `MISCONF ... not able to persist on disk`**：该实例 RDB 快照写入失败，此时**所有写命令（缓存、令牌黑名单）都会被拒绝**，而 `PING` 仍然正常，容易被误判为"Redis 正常"。处理方式：以安装目录为工作目录并用 `--dir` 指向可写目录重启，然后 `redis-cli bgsave` 确认 `rdb_last_bgsave_status:ok`。
4. **`npm install` 报 `EPERM ... node_cache`**：全局 npm 缓存目录对当前用户不可写。临时规避：`npm install --cache ./.npm-cache`；彻底解决需以管理员调整该目录权限。
5. **推送 GitHub 失败（`Connection was reset`）**：该网络下 GitHub 需要代理，例如 `git -c http.proxy=http://127.0.0.1:7897 push github main`；Gitee 可直连。
6. **上传附件报 400/1503 或 1504**：扩展名不在白名单或超过大小上限，可通过 `sys_config` 的 `attachment.allowed_extensions`、`attachment.max_size_mb` 调整。
7. **接口返回 401/1004**：令牌缺失、过期或已退出登录（黑名单命中），前端会自动清理本地令牌并跳回登录页。
8. **端口占用**：后端默认 8080、前端默认 5173，被占用时分别修改 `application.yml` 的 `server.port` 与 `vite.config.js` 的 `server.port`。
9. **`mvn clean` 报文件被占用**：通常是上一次运行的应用仍持有 `target` 下的日志文件句柄，先在运行它的终端按 Ctrl+C 停止。

更完整的部署顺序、验证清单与逐项排查表见 [docs/LOCAL_DEPLOYMENT.md](docs/LOCAL_DEPLOYMENT.md)。

## 18. 文档索引

| 文档 | 内容 |
| --- | --- |
| [docs/DEVELOPMENT_PLAN.md](docs/DEVELOPMENT_PLAN.md) | 阶段划分、验收标准、风险清单与阶段进度 |
| [docs/TECHNICAL_DESIGN.md](docs/TECHNICAL_DESIGN.md) | 架构、技术栈与版本、状态机、权限模型、API、Redis 与安全设计 |
| [docs/DATABASE_DESIGN.md](docs/DATABASE_DESIGN.md) | 17 张表结构、约束与索引、事务与并发策略、演示数据、初始化步骤 |
| [docs/API_DOCUMENTATION.md](docs/API_DOCUMENTATION.md) | 67 个端点按模块整理，含权限要求、统一响应与错误码 |
| [docs/LOCAL_DEPLOYMENT.md](docs/LOCAL_DEPLOYMENT.md) | 本地部署顺序、部署后验证清单、故障排查表 |
| [docs/TEST_REPORT.md](docs/TEST_REPORT.md) | 测试命令、用例分布、端到端验收证据与测试局限 |
| [docs/STAGE8_ACCEPTANCE.md](docs/STAGE8_ACCEPTANCE.md) | 整体验收与安全审查报告（敏感配置、越权、SQL 注入、文件访问、日志） |
| [docs/RESUME_PROJECT.md](docs/RESUME_PROJECT.md) | 简历素材：项目描述写法、亮点清单、技术难点详解、面试问答 |
| [docs/GIT_WORKFLOW.md](docs/GIT_WORKFLOW.md) | 分支策略、提交规范、双远程同步流程与失败处理 |
| [docs/FINAL_DELIVERY.md](docs/FINAL_DELIVERY.md) | 最终交付报告（功能、架构、测试、Git 与远程状态、已知问题） |
| [docs/STAGE0_ENVIRONMENT_CHECK.md](docs/STAGE0_ENVIRONMENT_CHECK.md) | 环境检查记录与阻塞项处理结果 |

## 19. 已知限制与取舍

| 项 | 说明 |
| --- | --- |
| Redis 版本 | 需求文本声明 Redis 7，实际验证环境为 Windows 移植版 5.0.14.1；系统仅使用 5.0 已支持的命令（缓存、`SET NX EX` 锁、带 TTL 的键） |
| Element Plus 引入方式 | 目前为全量引入（已独立分包，gzip 约 330 KB），如需进一步减小可改为按需引入 |
| 逾期提醒 | 目前输出结构化日志，未接入邮件/短信渠道（需求未要求；接入时替换定时任务中的输出实现即可） |
| OpenAPI 文档 | 端点匿名可访问（便于开发调试），生产环境建议限制为仅管理员 |
| 令牌存储 | 前端 access token 存于 localStorage（有效期 2 小时），存在 XSS 场景下被读取的风险；更强方案为 HttpOnly Cookie + CSRF 令牌 |
| 附件存储 | 仅支持本地磁盘（`APP_UPLOAD_DIR`），多实例部署需扩展为共享存储 |
| 测试范围 | 未包含浏览器端 E2E 与压力测试；前端以组件测试 + 构建 + 真实接口冒烟覆盖 |

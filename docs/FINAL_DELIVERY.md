# ServiceFlow 最终交付报告

> 交付日期：2026-10-15
> 交付方式：Git 仓库（本地 `E:\Work Order Management System`，远程 `gitee` 与 `github`）
> 本文档对应开发计划 Stage 9，是整个项目的收口说明。所有数据均来自实际执行结果，未完成项在最后一节如实列出。

## 1. 已完成的功能

| 模块 | 完成情况 |
| --- | --- |
| 用户认证与个人中心 | 登录、退出（令牌黑名单）、刷新令牌、当前用户信息、修改密码、更新资料；BCrypt 存储；失败 5 次锁定 15 分钟（阈值可配置） |
| 用户、角色与权限管理 | 用户增改、启停、分配角色、重置密码、逻辑删除；角色与权限清单（4 角色 / 30 权限 / 65 条授权）；故障分类管理；系统配置管理；操作日志查询 |
| 客户资料管理 | 建档、检索（关键词/等级/状态）、详情（设备数、工单数）、修改、启停、删除（带关联保护） |
| 设备档案管理 | 建档（编号与序列号唯一）、检索、详情（保修状态、工单数）、修改（含归属变更校验）、删除（有工单时拒绝） |
| 售后工单管理 | 唯一编号（`SO+yyyyMMdd+6 位`）、SLA 截止时间、列表（8 类筛选 + 分页 + 排序白名单）、详情（含客户联系方式、设备信息、历史、评价、附件与评论数）、逾期查询 |
| 分派、接单、转派与维修处理 | 分派、接单、转派、退回、填写处理记录、提交处理结果；状态机 + 条件更新 + Redis 锁 + 唯一索引四重保护；处理历史全程留痕 |
| 工单处理记录与客户评价 | 处理历史时间线（操作人、角色、前后状态、内容、工时）；一单一评、仅已完成、仅所属客户、支持匿名（对非评价人隐藏内容） |
| 工单附件管理 | 上传、列表、下载、删除；扩展名与大小白名单（配置驱动）、文件名清洗、UUID 存储名、SHA-256、路径穿越防护、按工单权限校验、删除同步清理物理文件 |
| 超时提醒与操作审计 | 超期自动确认（默认 72 小时，写 `AUTO_CONFIRM` 记录，操作人 SYSTEM）；每小时逾期提醒日志；关键操作审计（参数脱敏、traceId 贯穿） |
| 售后运营数据统计 | 总览、趋势（日/周/月）、故障类型分布、优先级分布、工程师工作量、客户满意度；前端 ECharts 全部使用真实统计 |
| Redis 缓存与失效 | 统计缓存（键含数据范围隔离、TTL 60s/300s/600s）、事务提交后失效、Redis 不可用降级为查库；令牌黑名单；接单分布式锁 |
| 系统基础配置与异常处理 | 统一响应结构与错误码（7 个区间）、全局异常处理（参数校验/鉴权/认证/404/兜底）、traceId 过滤器、结构化日志、系统配置项 |
| 前端页面 | 13 个页面 + 403/404：登录、仪表盘、工单列表/创建/详情/处理工作台、客户管理、设备档案、服务评价、用户管理、角色与权限、系统配置、操作日志、个人中心 |

## 2. 项目架构与技术栈

**架构**：模块化单体（非微服务），前后端分离。后端分层为 Controller → Service → Mapper → Entity，配套 DTO / VO / Security / Config / Exception / Common；权限采用"接口级（`@PreAuthorize`）+ 资源级（`AccessGuard`）+ 数据域级（`DataScopeService`）"三层校验。前端为 Vue 3 SPA（Pinia + Vue Router + Axios），通过 Vite 代理访问后端。

| 层次 | 版本与组件 |
| --- | --- |
| 后端 | Java 17、Spring Boot 3.3.5、Spring MVC、Spring Security 6.3、MyBatis-Plus 3.5.7、MySQL 8、Redis（Lettuce）、Springdoc OpenAPI 2.6.0、jjwt 0.12.6、JUnit 5 + Mockito |
| 前端 | Vue 3.5、JavaScript（ES2022，按用户要求不使用 TypeScript）、Vite 5.4、Element Plus 2.11.9、Vue Router 4.4、Pinia 2.2、Axios 1.7、ECharts 5.5、Vitest 2.1 + Vue Test Utils、ESLint 9 + Prettier |
| 构建与工具 | Maven 3.10.0（含 Wrapper）、npm 11、Git、GitHub、Gitee、IntelliJ IDEA、VS Code |

## 3. 数据库表与主要 API

**数据库**：17 张表（`sys_user`、`sys_role`、`sys_user_role`、`sys_permission`、`sys_role_permission`、`fault_category`、`sys_config`、`customer`、`device`、`service_ticket`、`ticket_assignment`、`ticket_process_record`、`ticket_comment`、`ticket_evaluation`、`ticket_attachment`、`sys_operation_log`、`ticket_sequence`），结构与索引设计见 [DATABASE_DESIGN.md](DATABASE_DESIGN.md)。

**主要 API**（统一前缀 `/api/v1`，共 14 个控制器、67 个端点）：

| 模块 | 端点示例 |
| --- | --- |
| 认证 | `POST /auth/login`、`POST /auth/logout`、`POST /auth/refresh`、`GET /auth/me`、`PUT /auth/password`、`PUT /auth/profile` |
| 用户与系统 | `GET/POST/PUT/DELETE /users`、`PUT /users/{id}/roles`、`PUT /users/{id}/status`、`POST /users/{id}/reset-password`、`GET /users/engineers`、`GET /roles`、`GET /permissions`、`/fault-categories`、`/configs`、`GET /operation-logs` |
| 客户与设备 | `GET/POST/PUT/DELETE /customers`、`PUT /customers/{id}/status`、`GET /customers/options`、`GET/POST/PUT/DELETE /devices`、`GET /devices/options` |
| 工单 | `GET/POST /tickets`、`GET /tickets/overdue`、`GET /tickets/{id}`、`GET /tickets/{id}/records`、`POST /tickets/{id}/{assign\|accept\|transfer\|reject\|process\|complete\|confirm\|reopen\|cancel}`、`/tickets/{id}/comments`、`/tickets/{id}/evaluation`、`/tickets/{id}/attachments`（含 `/{attachmentId}/download`） |
| 统计 | `GET /dashboard/{overview\|trend\|fault-distribution\|priority-distribution\|engineer-workload\|satisfaction}` |
| 健康检查 | `GET /health`（匿名，实际探测 MySQL 与 Redis） |

完整清单与权限要求见 [API_DOCUMENTATION.md](API_DOCUMENTATION.md)；可交互文档由 Springdoc 在 `/swagger-ui.html` 提供。

## 4. 实际执行的测试命令和结果

| 命令 | 结果 |
| --- | --- |
| `mvn clean test` | 98 个单元测试全部通过（不依赖数据库） |
| `mvn clean verify` | 单元 98 + 集成 65 = **163 个测试全部通过**，BUILD SUCCESS |
| `mvn clean package` | BUILD SUCCESS，产出可执行 jar |
| `npm run lint` | 退出码 0（`--max-warnings 0`） |
| `npm run test` | **25 个前端测试全部通过**（7 个测试文件） |
| `npm run build` | 构建成功，主包 51 KB（gzip 21 KB），ECharts 与 Element Plus 独立分包 |
| 端到端验收 | 在打包产物上完成真实链路：建单 → 分派 → 接单 → 处理 → 提交 → 确认 → 评价 + 评论可见性 + 附件上传下载删除 + 统计与审计，17 步全部符合预期 |

测试明细与验收证据见 [TEST_REPORT.md](TEST_REPORT.md) 与 [STAGE8_ACCEPTANCE.md](STAGE8_ACCEPTANCE.md)。

## 5. 本地启动方式

完整步骤见 [README.md](../README.md) 第 1~8 节与 [LOCAL_DEPLOYMENT.md](LOCAL_DEPLOYMENT.md)，最短路径：

```powershell
# 1) 数据库（开发库与测试库）
mysql -u root -p -e "CREATE DATABASE IF NOT EXISTS serviceflow DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci; CREATE DATABASE IF NOT EXISTS serviceflow_test DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;"
mysql -u root -p serviceflow -e "source backend/src/main/resources/db/schema.sql"
mysql -u root -p serviceflow -e "source backend/src/main/resources/db/data.sql"
mysql -u root -p serviceflow -e "source backend/src/main/resources/db/demo-data.sql"

# 2) Redis（注册为服务后常驻）
cd "D:\Tools\Redis\Redis-x64-5.0.14.1"; .\redis-server.exe --service-start; redis-cli ping

# 3) 后端
$env:JAVA_HOME = "D:\Tools\envs\jdk17"; $env:DB_PASSWORD = "你的本地数据库密码"
cd "E:\Work Order Management System\backend"; .\mvnw.cmd spring-boot:run

# 4) 前端
cd "E:\Work Order Management System\frontend"; npm install; npm run dev
```

访问：前端 <http://localhost:5173/>，健康检查 <http://localhost:8080/api/v1/health>，接口文档 <http://localhost:8080/swagger-ui.html>。

## 6. 演示账号与角色

| 账号 | 角色 | 权限范围 |
| --- | --- | --- |
| `admin` | `ROLE_ADMIN` | 全部权限：用户/角色/故障分类/系统配置/操作日志、全局统计、全部业务操作 |
| `service01` | `ROLE_SERVICE` | 客户与设备维护、创建工单、分派/转派/退回/取消、全局统计 |
| `engineer01` / `engineer02` | `ROLE_ENGINEER` | 仅本人负责工单：接单、处理记录、提交结果、转派；设备只读；个人统计 |
| `customer01` / `customer02` | `ROLE_CUSTOMER` | 仅自身客户档案与设备、自身工单：确认服务结果、评价（可匿名） |

统一密码 `Serviceflow@2026`，仅用于本地开发；库中以 BCrypt 哈希存储，明文未进入任何配置文件；生产环境必须删除这些账号。

## 7. Git 提交记录

- 提交总数 **46**，分布：`feat` 13、`docs` 13、`test` 6、`fix` 4、`build` 2、`chore` 1、`merge` 7（各阶段 `--no-ff` 合并）。
- 阶段对应关系：

| 阶段 | 代表提交 |
| --- | --- |
| Stage 0 环境与计划 | `38772a3` docs(project): add development plan, technical design and git workflow；`674eeb3` docs(stage0): record environment blocker resolutions and first sync |
| Stage 1 工程骨架 | `7e9a7a3` build(backend)…；`e7228d2` feat(common)…；`b05ef8e` build(frontend)…；`e506cca` chore(project)…；`903eb27` merge |
| 前端 JS 迁移 | `0542a5d` fix(web): migrate frontend from typescript to javascript；`7bc56a6` merge |
| Stage 2 数据库与认证权限 | `604d2d3` feat(db)…；`310764e` feat(auth)…；`b61f254` test(security)…；`4237af8` docs(db)…；`6041a74` fix(audit)…；`e2d9618` docs(workflow)… |
| Stage 3 客户与设备 | `5cbe6a1` feat(db)…；`ce7dbe3` feat(customer)…；`e8a235f` fix(rbac)…；`6ed4645` test(customer)…；`5c4d70d` docs(db)…；`a4504cf` merge |
| Stage 4 工单核心 | `368e305` feat(db)…；`239b570` feat(ticket)…；`d0ed1a0` test(ticket)…；`fcdc6a8` docs(ticket)…；`1db363e` merge |
| Stage 5 评价与附件 | `0706724` feat(feedback)…；`8d84f64` feat(rbac)…；`a5fde71` test(feedback)…；`a15cd5b` docs(feedback)…；`8427d97` merge |
| Stage 6 统计与缓存 | `ffe617a` feat(dashboard)…；`bf20b4b` test(dashboard)…；`2da7cb0` docs(dashboard)…；`9e27391` merge |
| Stage 7 前端页面 | `3556474` feat(ticket)…；`8f91642` feat(web)…；`f943397` feat(web)…；`26c4a2c` docs(web)…；`84c41de` test(web)…；`f1b49b0` merge |
| Stage 8 验收审查 | `6b6a838` fix(web): hide demo accounts in production builds；`4dc9bd4` docs(acceptance)… |
| Stage 9 交付文档 | 见本阶段提交 |

> 完整历史可用 `git log --oneline` 查看；每个阶段的提交顺序均为"实现 → 测试 → 文档"。

## 8. GitHub 推送状态

- 远程：`github` → `https://github.com/Liukoushui0/Work-Order-Management-System.git`
- **已同步**：`38772a3`（初始文档）、`674eeb3`、`903eb27`（Stage 1）、`7bc56a6`（JS 迁移）、`e2d9618`（Stage 2）、`a4504cf`（Stage 3）、`1db363e`（Stage 4）、`8427d97`（Stage 5）、`9e27391`（Stage 6）
- **待推送**：Stage 7 之后的提交（`f1b49b0` 及之后）。原因：访问 GitHub 需要本机代理（`127.0.0.1:7897`），当前代理已关闭，直连 `github.com:443` 会在 TLS 阶段被重置。
- 补齐方式：启动代理后执行 `git -c http.proxy=http://127.0.0.1:7897 push github main`（本地提交完整保留，未做任何强制推送）。

## 9. Gitee 推送状态

- 远程：`gitee` → `https://gitee.com/liukoushui0/Work-Order-Management-System.git`
- **已全部同步**：最后一次推送 `f1b49b0..4dc9bd4 main -> main` 成功；本地 `main` 与 `gitee/main` 提交一致。
- 每次阶段验收后均先确认远程无新提交再推送，未使用过 `--force`。

> 说明：两个远程当前**尚未完全一致**（GitHub 落后于 Gitee），这是已知的待办项，未在报告中声称"双远程已同步"。

## 10. 未完成事项与已知问题

| 编号 | 事项 | 影响 | 建议处理 |
| --- | --- | --- | --- |
| K1 | GitHub 待补推（见第 8 节） | 双远程历史暂不一致 | 代理可用后一条命令补齐 |
| K2 | Element Plus 全量引入（已独立分包，gzip 约 330 KB） | 首屏体积偏大 | 可改为按需引入（`unplugin-vue-components`） |
| K3 | 逾期提醒仅输出结构化日志，未接入邮件/短信 | 需人工查看日志 | 需求未要求通知渠道；接入时替换 `TicketScheduledService.remindOverdueTickets` 的输出实现即可 |
| K4 | Redis 实际为 5.0.14.1（需求文档声明 7） | 与需求文本不一致 | 已如实记录；系统仅使用 5.0 支持的命令；如需 7.x 可安装 WSL2 官方版或 Memurai |
| K5 | OpenAPI 文档端点匿名可访问 | 暴露接口清单 | 生产建议限制为仅管理员访问 |
| K6 | access token 存于前端 localStorage | XSS 场景可被读取 | 已用短有效期（2 小时）+ 后端强制鉴权控制影响面；更强方案为 HttpOnly Cookie + CSRF |
| K7 | 附件仅支持本地磁盘存储 | 多实例部署无法共享 | 本地开发无此需求；如需扩展可抽象存储接口 |
| K8 | 未做浏览器端 E2E 与压力测试 | 缺少真实浏览器交互与性能数据 | 前端已用组件测试 + 构建 + 真实接口冒烟覆盖；如需可引入 Playwright |

## 11. 交付结论

项目的全部 12 个后端模块、13 个前端页面与 3 份脚本（结构/基础数据/演示数据）已实现并通过验收：后端 163 个自动化测试、前端 25 个测试全部通过，打包产物完成真实端到端验收，权限、状态机、事务、并发、缓存、安全（敏感配置、日志脱敏、文件访问、SQL 注入）均经检查。

唯一未完成项是 **GitHub 远程的最后一段推送（K1）**，属网络环境限制，本地提交完整保留，恢复代理后即可补齐；其余 K2~K8 为可选优化或已记录的取舍，不影响系统功能与可运行性。

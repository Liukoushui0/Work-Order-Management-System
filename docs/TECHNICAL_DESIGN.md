# ServiceFlow 企业售后工单管理系统 — 技术设计文档

> 文档版本：v1.0
> 阶段：Stage 0
> 关联文档：[DEVELOPMENT_PLAN.md](./DEVELOPMENT_PLAN.md)、[GIT_WORKFLOW.md](./GIT_WORKFLOW.md)、[STAGE0_ENVIRONMENT_CHECK.md](./STAGE0_ENVIRONMENT_CHECK.md)

## 1. 系统概述

ServiceFlow 是一个面向企业售后团队的工单管理系统，采用**模块化单体架构**，前后端分离部署（本地开发环境不使用容器）：

- 后端：Spring Boot 3 单体服务，对外提供 `/api/v1` 前缀的 REST API。
- 前端：Vue 3 SPA，通过 Axios 调用后端 API，由 Vite 提供开发服务器。
- 存储：MySQL 8 保存全部业务数据；Redis 用于缓存、注销令牌与并发锁。

### 1.1 业务角色与职责

| 角色编码 | 名称 | 核心职责 |
| --- | --- | --- |
| `ROLE_ADMIN` | 系统管理员 | 管理用户、角色、故障分类与系统配置，查看全局运营统计与操作日志 |
| `ROLE_SERVICE` | 售后客服/销售 | 维护客户与设备信息，创建工单，分配或转派维修任务 |
| `ROLE_ENGINEER` | 维修工程师 | 查看分配给自己的工单，接单、处理故障、填写维修记录、提交处理结果 |
| `ROLE_CUSTOMER` | 客户 | 查看自己的设备与工单，符合条件时确认服务结果并进行评价 |

### 1.2 模块划分（模块化单体）

| 模块 | 内容 |
| --- | --- |
| 认证与个人中心 | 登录、退出、令牌刷新、当前用户信息、修改密码、个人资料 |
| 用户/角色/权限 | 用户管理、角色管理、权限分配、故障分类管理、系统配置 |
| 客户资料 | 客户建档、检索、维护、状态管理 |
| 设备档案 | 设备建档、与客户关联、保修信息、状态管理 |
| 售后工单 | 创建、编号生成、查询、分派、接单、转派、退回、处理、完成、取消、重新打开 |
| 处理记录与评价 | 处理历史、内部备注/客户沟通、客户评价 |
| 附件管理 | 上传、下载、删除、类型与大小限制、访问控制 |
| 超时提醒与审计 | 逾期识别与查询、操作审计日志 |
| 运营统计 | 趋势、故障类型、优先级、逾期、效率、工作量、满意度 |
| 基础设施 | Redis 缓存与失效、统一配置、统一异常与响应、结构化日志 |

模块之间通过 Service 接口调用，禁止跨模块直接操作对方 Mapper。

---

## 2. 技术栈与版本

### 2.1 版本选型原则

选择**相互兼容且经过广泛验证**的稳定版本组合，不为追求最新版本引入兼容风险。任何调整都必须先分析原因、记录方案，再实施。

### 2.2 后端

| 组件 | 版本 | 说明 |
| --- | --- | --- |
| JDK | 17（LTS） | 项目编译与运行基线 |
| Spring Boot | 3.3.5 | Web / Security / Validation / Data Redis 统一由父 POM 管理 |
| Spring Framework | 6.1.x | 由 Spring Boot 管理 |
| Spring Security | 6.3.x | 由 Spring Boot 管理，无状态 JWT 认证 |
| MyBatis-Plus | 3.5.7（`mybatis-plus-spring-boot3-starter`） | ORM / 分页插件 / 乐观锁插件 / 逻辑删除 |
| MySQL Connector/J | 由 Spring Boot 管理（8.4.x） | MySQL 8 驱动 |
| HikariCP | 由 Spring Boot 管理 | 连接池 |
| JWT | jjwt 0.12.6（api/impl/jackson） | 令牌签发与校验 |
| Redis 客户端 | spring-boot-starter-data-redis + Lettuce + commons-pool2 | 缓存、锁、令牌黑名单 |
| Springdoc OpenAPI | 2.6.0（`springdoc-openapi-starter-webmvc-ui`） | Swagger UI 与接口文档 |
| 参数校验 | spring-boot-starter-validation（Hibernate Validator 8.x） | DTO 校验 |
| Lombok | 由 Spring Boot 管理 | 减少样板代码 |
| 测试 | JUnit 5、Mockito、spring-boot-starter-test、spring-security-test | 单元与集成测试 |
| 构建 | Maven（Wrapper 锁定 3.10.0） | 构建与打包 |

### 2.3 前端

| 组件 | 版本 |
| --- | --- |
| Node.js | 24.x（本机 v24.21.0） |
| Vue | 3.5.x |
| TypeScript | 5.6.x |
| Vite | 5.4.x + `@vitejs/plugin-vue` 5.1.x |
| Element Plus | 2.11.9 |
| Vue Router | 4.4.x |
| Pinia | 2.2.x |
| Axios | 1.7.x |
| ECharts | 5.5.x |
| 测试 | Vitest 2.1.x + Vue Test Utils 2.4.x + jsdom |
| 质量 | vue-tsc 2.1.x、ESLint 9（flat config）+ eslint-plugin-vue、Prettier 3.3.x |

> 兼容性说明：本机 Node.js 为 24.x，高于 Vite 5 官方主要验证版本区间。Stage 1 会在真实构建中验证该组合；若出现不兼容（例如原生依赖或构建失败），将分析原因并记录后升级至 Vite 6 + Vitest 3，且同步更新本文档。

### 2.3.1 版本调整记录

| 调整项 | 原计划 | 实际采用 | 原因与证据 |
| --- | --- | --- | --- |
| Element Plus | 2.8.8 | **2.11.9** | 2.8.8 的 `el-tag` 在 vnode 挂载钩子中直接执行 `vnode.component.subTree.component.bum = null`，未做空值判断。当 `el-tag` 渲染在 `el-descriptions` 的插槽渲染函数中时（本项目的仪表盘状态标签），`subTree.component` 为 null，抛出 `TypeError: Cannot set properties of null (setting 'bum')`，并导致该组件树实例被破坏。2.11.9 的同一钩子已改为逐级可选链判空，缺陷消失。该结论由前端单元测试 `DashboardView.spec.ts` 在升级前后真实运行验证（升级前 2 个用例失败，升级后 10/10 通过）。 |
| Vite / Vitest | Vite 5.4.11 + Vitest 2.1.8 | 与计划一致 | Node.js 24.21.0 下 `npm run build`、`npm run test` 均通过，无需升级。 |
| Logback 配置 | `TimeBasedRollingPolicy` + `SizeAndTimeBasedFNATP` | `SizeAndTimeBasedRollingPolicy` | Spring Boot 3.3 使用 Logback 1.5，`SizeAndTimeBasedFNATP` 已被移除，原配置导致上下文启动失败；改用新策略后启动正常。 |

### 2.4 外部服务

| 服务 | 版本要求 | 本机现状（见环境检查报告） |
| --- | --- | --- |
| MySQL | 8.x | 8.0.46，服务 `MySQL80` 运行中（凭据待确认） |
| Redis | 7.x（需求） | 仅 Windows 版 5.0.14.1，未运行（偏差待决策） |

---

## 3. 工程结构与分层

### 3.1 目录结构

```
Work Order Management System/
├── backend/
│   ├── pom.xml
│   ├── mvnw / mvnw.cmd / .mvn/wrapper/
│   └── src/
│       ├── main/java/com/serviceflow/
│       │   ├── ServiceFlowApplication.java
│       │   ├── common/        # Result、PageRequest/PageResult、错误码、常量、工具
│       │   ├── config/        # Security、MyBatis-Plus、Redis、OpenAPI、Web、Jackson
│       │   ├── exception/     # BusinessException、GlobalExceptionHandler
│       │   ├── security/      # JwtTokenProvider、JwtAuthenticationFilter、UserPrincipal、
│       │   │                  # AccessGuard、DataScopeService
│       │   ├── controller/    # REST 接口层
│       │   ├── service/       # 业务逻辑 + 事务（impl 子包放实现）
│       │   ├── mapper/        # MyBatis-Plus Mapper
│       │   ├── entity/        # 数据库实体
│       │   ├── dto/           # 请求参数
│       │   ├── vo/            # 响应对象
│       │   └── aspect/        # 操作日志、耗时统计切面
│       ├── main/resources/
│       │   ├── application.yml / application-dev.yml / application-test.yml
│       │   ├── db/schema.sql、db/data.sql
│       │   ├── mapper/*.xml   # 复杂 SQL（统计、列表联查）
│       │   └── logback-spring.xml
│       └── test/java/com/serviceflow/...   # 单元测试与集成测试
├── frontend/
│   ├── package.json、vite.config.ts、tsconfig*.json、eslint.config.js
│   └── src/
│       ├── api/        # 按模块拆分的接口封装
│       ├── assets/     # 静态资源
│       ├── components/ # 通用组件（搜索栏、表格、状态标签、图表容器等）
│       ├── composables/# 组合式函数（分页、权限、字典）
│       ├── layouts/    # 主布局（侧边栏/顶栏/面包屑）
│       ├── router/     # 路由与守卫
│       ├── stores/     # Pinia（auth、app、dict）
│       ├── styles/     # 全局样式与主题变量
│       ├── types/      # TS 类型（与 VO 对应）
│       ├── utils/      # request 封装、格式化、校验
│       └── views/      # 页面
├── docs/
└── .gitignore
```

### 3.2 分层职责

| 层 | 职责 | 约束 |
| --- | --- | --- |
| Controller | 接收请求、参数绑定与校验、返回统一响应 | 不含业务逻辑；不直接使用 Mapper |
| Service | 业务规则、状态机、权限判定、事务边界、缓存读写 | 事务注解在 Service 方法上 |
| Mapper | 数据访问，复杂查询使用 XML | 不在 XML 中拼接用户输入 |
| Entity | 数据库表映射 | 不直接返回给前端 |
| DTO | 请求参数，带校验注解 | 一个接口一个 DTO |
| VO | 响应对象 | 只暴露必要字段，绝不返回密码、内部标记 |
| Security | 认证、授权、数据域 | 所有鉴权必须在后端执行 |
| Config | 框架配置 | 敏感值来自环境变量 |
| Exception | 统一异常与错误码 | 业务异常不打印堆栈，系统异常记录堆栈与 traceId |
| Common | 统一响应、分页、工具、常量 | 不依赖上层包 |

### 3.3 API 规范

- 统一前缀：`/api/v1`。
- 统一响应结构：

```json
{
  "code": 0,
  "message": "success",
  "data": {},
  "traceId": "6f1c...",
  "timestamp": "2026-10-15T10:30:00+08:00"
}
```

- 错误响应使用相同结构，`code` 为业务错误码，HTTP 状态码同时保持语义正确（400/401/403/404/409/500）。
- 分页响应统一为 `{ "records": [], "total": 0, "pageNum": 1, "pageSize": 10, "pages": 0 }`。
- 分页请求统一参数：`pageNum`（默认 1，最小 1）、`pageSize`（默认 10，最大 100）、`sortField`（白名单校验）、`sortOrder`（`asc`/`desc`）。

### 3.4 错误码规划

| 区间 | 含义 | 示例 |
| --- | --- | --- |
| 0 | 成功 | — |
| 1000~1099 | 认证 | 1001 用户名或密码错误；1002 账号被禁用；1003 账号被锁定；1004 令牌无效或过期 |
| 1100~1199 | 鉴权 | 1101 无权访问该资源；1102 数据域外访问 |
| 1200~1299 | 参数与校验 | 1201 参数校验失败；1202 非法排序字段 |
| 1300~1399 | 客户与设备 | 1301 客户不存在；1302 设备不存在；1303 设备不属于该客户 |
| 1400~1499 | 工单 | 1401 工单不存在；1402 非法状态流转；1403 工单已被接单；1404 工单已取消；1405 无权限操作该工单 |
| 1500~1599 | 评价与附件 | 1501 工单未完成不可评价；1502 该工单已评价；1503 文件类型不允许；1504 文件过大；1505 附件不存在 |
| 1600~1699 | 系统与缓存 | 1601 系统内部错误；1602 并发冲突请重试；1603 外部依赖不可用 |

---

## 4. 数据库设计

### 4.1 设计约定

- 引擎与字符集：`InnoDB` + `utf8mb4` / `utf8mb4_general_ci`。
- 主键：`BIGINT UNSIGNED AUTO_INCREMENT`，列名 `id`。
- 时间字段：`create_time`、`update_time`（`DATETIME`）。
- 审计字段：`create_by`、`update_by`。
- 逻辑删除：主数据表使用 `deleted BIGINT NOT NULL DEFAULT 0`，删除时写入本行 `id`（而非固定 1），配合唯一索引 `(业务唯一列, deleted)` 保证"未删除时全局唯一，删除后同名可重建"。
- 乐观锁：需要并发保护的 `service_ticket` 使用 `version INT NOT NULL DEFAULT 0`。
- 工单类业务记录（`service_ticket`、`ticket_process_record`、`ticket_comment`、`ticket_assignment`、`ticket_evaluation`、`sys_operation_log`）不做逻辑删除，通过状态字段表达生命周期，保证可追溯。

### 4.2 表清单

| # | 表名 | 中文名 | 说明 |
| --- | --- | --- | --- |
| 1 | `sys_user` | 用户表 | 员工与客户登录账号 |
| 2 | `sys_role` | 角色表 | 四种角色 |
| 3 | `sys_user_role` | 用户角色关系表 | 支持一人多角色 |
| 4 | `sys_permission` | 权限表 | 菜单/按钮/接口权限（新增表，用于细粒度授权） |
| 5 | `sys_role_permission` | 角色权限关系表 | 新增表 |
| 6 | `fault_category` | 故障分类表 | 新增表，支撑"管理员管理故障分类" |
| 7 | `sys_config` | 系统配置表 | 新增表，支撑"系统基础配置" |
| 8 | `customer` | 客户表 | 客户资料 |
| 9 | `device` | 设备表 | 客户设备档案 |
| 10 | `service_ticket` | 工单表 | 工单主表 |
| 11 | `ticket_assignment` | 工单分派表 | 分派/转派/退回记录 |
| 12 | `ticket_process_record` | 处理记录表 | 状态流转与处理历史 |
| 13 | `ticket_comment` | 工单评论表 | 内部备注与客户沟通 |
| 14 | `ticket_evaluation` | 服务评价表 | 一单一评（唯一约束） |
| 15 | `ticket_attachment` | 附件表 | 工单附件元数据 |
| 16 | `sys_operation_log` | 操作日志表 | 审计日志 |

> 需求要求"至少"包含 11 张指定表，上表 16 张为其超集，新增表均为实现需求中明确功能（权限管理、故障分类、系统配置、客户评价）所必需。

### 4.3 关键表结构（节选，完整 DDL 于 Stage 2 落地为 `db/schema.sql`）

**sys_user**

| 字段 | 类型 | 约束/说明 |
| --- | --- | --- |
| id | BIGINT UNSIGNED | PK, AUTO_INCREMENT |
| username | VARCHAR(50) | NOT NULL |
| password | VARCHAR(100) | NOT NULL，BCrypt 哈希，禁止明文 |
| real_name | VARCHAR(50) | NOT NULL |
| phone | VARCHAR(20) | 手机号，格式校验 |
| email | VARCHAR(100) | — |
| avatar | VARCHAR(255) | 头像路径 |
| customer_id | BIGINT UNSIGNED NULL | 客户角色账号绑定的客户档案 |
| status | TINYINT | 1 启用 / 0 禁用 |
| login_fail_count | INT | 连续登录失败次数 |
| lock_until | DATETIME NULL | 锁定截止时间 |
| last_login_time / last_login_ip | DATETIME / VARCHAR(45) | 登录审计 |
| version | INT | 乐观锁 |
| deleted | BIGINT | 逻辑删除 |
| create_time / update_time / create_by / update_by | — | 审计字段 |

索引：`uk_username (username, deleted)`、`uk_customer_id (customer_id)`、`idx_status`、`idx_phone`。

**service_ticket（核心表）**

| 字段 | 类型 | 约束/说明 |
| --- | --- | --- |
| id | BIGINT UNSIGNED | PK |
| ticket_no | VARCHAR(32) | NOT NULL，唯一：`uk_ticket_no` |
| title | VARCHAR(200) | NOT NULL |
| description | TEXT | 故障描述 |
| customer_id | BIGINT UNSIGNED | NOT NULL，客户 |
| device_id | BIGINT UNSIGNED | NOT NULL，设备 |
| fault_category_id | BIGINT UNSIGNED | 故障分类 |
| priority | VARCHAR(16) | LOW/MEDIUM/HIGH/URGENT |
| status | VARCHAR(24) | PENDING_ASSIGN/PENDING_ACCEPT/PROCESSING/PENDING_CONFIRM/COMPLETED/CANCELLED |
| source | VARCHAR(16) | PHONE/WEB/WECHAT/EMAIL |
| reporter_name / reporter_phone | VARCHAR(50) / VARCHAR(20) | 报修人信息 |
| assignee_id | BIGINT UNSIGNED NULL | 当前负责人（工程师） |
| deadline_time | DATETIME | SLA 截止时间，逾期查询依据 |
| assign_time / accept_time / start_time / finish_time / confirm_time / cancel_time / closed_time | DATETIME | 各节点时间戳，用于效率统计 |
| cancel_reason | VARCHAR(255) | 取消原因 |
| reject_count / reopen_count / transfer_count | INT | 退回/重开/转派次数 |
| handle_hours | DECIMAL(10,2) | 处理总时长（小时） |
| version | INT | 乐观锁 |
| create_by / create_time / update_by / update_time | — | 审计字段 |

索引：`uk_ticket_no (ticket_no)`、`idx_status_create_time (status, create_time)`、`idx_assignee_status (assignee_id, status)`、`idx_customer_status (customer_id, status)`、`idx_deadline_status (deadline_time, status)`、`idx_device_id (device_id)`、`idx_priority`。

**其他表要点**

- `ticket_assignment`：`ticket_id`、`from_user_id`、`to_user_id`、`assign_type`（ASSIGN/TRANSFER/REJECT_TO_POOL）、`reason`、`active_flag`（活动记录为 1，历史记录写入自身 `id`），唯一索引 `uk_ticket_active (ticket_id, active_flag)` 保证"一个工单同时只有一条有效分派记录"。
- `ticket_process_record`：`ticket_id`、`operator_id`、`operator_role`、`action`、`from_status`、`to_status`、`content`、`used_hours`、`create_time`；索引 `idx_ticket_create (ticket_id, create_time)`。这是"工单历史可追溯"的依据。
- `ticket_comment`：`ticket_id`、`user_id`、`user_type`（STAFF/CUSTOMER）、`content`、`is_internal`（1 仅员工可见）。
- `ticket_evaluation`：唯一索引 `uk_ticket_id (ticket_id)` 保证一单一评；`score`（1~5）、`satisfaction`、`speed_score`、`quality_score`、`attitude_score`、`tags`、`content`。
- `ticket_attachment`：`ticket_id`、`file_name`（原始名）、`stored_name`、`file_path`（相对存储目录）、`file_ext`、`file_size`、`content_type`、`sha256`、`biz_type`、`uploader_id`、`status`、`deleted`；索引 `idx_ticket_id (ticket_id, deleted)`。
- `sys_operation_log`：`user_id`、`username`、`module`、`operation`、`request_uri`、`request_method`、`request_params`（脱敏 JSON，截断）、`response_code`、`error_msg`、`ip`、`user_agent`、`duration_ms`、`create_time`；索引 `idx_create_time`、`idx_user_id`、`idx_module`。

### 4.4 事务边界与并发策略

| 业务操作 | 事务范围 | 并发保护 |
| --- | --- | --- |
| 创建工单 | 插入工单 + 写入创建处理记录 | 编号生成使用唯一索引兜底并重试 |
| 分派/转派 | 更新工单负责人与状态 + 关闭旧分派记录 + 新增分派记录 + 处理记录 | 条件更新 `WHERE id=? AND status=? AND version=?`，影响行数为 0 则返回 1602 并发冲突 |
| 接单 | 更新工单状态与接单人 + 处理记录 | Redis 锁 `sf:dev:lock:ticket:accept:{ticketId}` + 条件更新双保险；Redis 不可用时降级为数据库条件更新 |
| 提交处理/完成 | 更新工单状态、处理时长 + 处理记录 | 条件更新 + 版本号 |
| 客户评价 | 校验状态与归属 + 插入评价 + 处理记录 | 唯一索引 `uk_ticket_id` 兜底，重复评价返回 1502 |
| 附件上传 | 文件落盘 + 插入元数据 | 落盘失败不入库；入库失败删除已落盘文件 |

**事务原则**：事务只包裹数据库操作，不包裹文件 IO 与网络调用；跨模块操作在同一个 Service 事务方法内完成；只读查询使用 `readOnly` 或不加事务。

### 4.5 编号生成策略

工单号格式：`SO` + `yyyyMMdd` + 6 位当日序列，例如 `SO20261015000001`。

生成方式：按当日工单序号生成（基于当日最大序号 + 数据库唯一索引冲突重试），确保并发下唯一且不可重复，容量为单日 999999 单。

### 4.6 演示数据与初始化

- `db/schema.sql`：建表、索引、约束（`CREATE TABLE IF NOT EXISTS`）。
- `db/data.sql`：演示角色、权限、用户、客户、设备、故障分类、各状态工单、评价样本；结构脚本与演示数据脚本严格分离，生产部署只执行结构脚本；演示数据脚本的具体可重复执行策略在 Stage 2 实现时确定并记录。

---

## 5. 工单状态机

### 5.1 状态定义

| 状态 | 中文 | 含义 |
| --- | --- | --- |
| `PENDING_ASSIGN` | 待分派 | 工单已创建，等待客服分派 |
| `PENDING_ACCEPT` | 待接单 | 已分派给工程师，等待接单 |
| `PROCESSING` | 处理中 | 工程师已接单，正在处理 |
| `PENDING_CONFIRM` | 待确认 | 工程师已提交处理结果，等待客户确认 |
| `COMPLETED` | 已完成 | 客户已确认（或超期自动确认） |
| `CANCELLED` | 已取消 | 工单被取消（终态） |

### 5.2 主流程

```
PENDING_ASSIGN --分派--> PENDING_ACCEPT --接单--> PROCESSING --提交处理--> PENDING_CONFIRM --确认--> COMPLETED
```

### 5.3 允许的转换矩阵

| 当前状态 | 操作 | 目标状态 | 允许角色 | 前置条件 |
| --- | --- | --- | --- | --- |
| — | 创建 | PENDING_ASSIGN | 客服、管理员 | 客户与设备存在且匹配 |
| PENDING_ASSIGN | 分派 | PENDING_ACCEPT | 客服、管理员 | 工单未取消；指定工程师有效 |
| PENDING_ASSIGN | 取消 | CANCELLED | 客服、管理员 | 填写取消原因 |
| PENDING_ACCEPT | 接单 | PROCESSING | 被分派的工程师 | 当前操作人 == 负责人；未被他人接单 |
| PENDING_ACCEPT | 转派 | PENDING_ACCEPT | 客服、管理员、当前工程师 | 指定新工程师并记录转派 |
| PENDING_ACCEPT | 退回 | PENDING_ASSIGN | 当前工程师 | 填写退回原因；`reject_count+1` |
| PROCESSING | 填写处理记录 | PROCESSING | 当前工程师 | 工单处于处理中 |
| PROCESSING | 提交处理结果 | PENDING_CONFIRM | 当前工程师 | 至少一条处理记录；填写处理说明 |
| PROCESSING | 转派 | PENDING_ACCEPT | 客服、管理员 | 转派后需重新接单 |
| PENDING_CONFIRM | 客户确认 | COMPLETED | 该工单客户账号、管理员 | 操作人属于该工单客户 |
| PENDING_CONFIRM | 退回重做 | PROCESSING | 该工单客户、管理员 | 填写不满原因；`reopen_count+1` |
| PENDING_CONFIRM | 自动确认 | COMPLETED | 系统定时任务 | 超过 `confirm_timeout_hours`（默认 72 小时） |
| COMPLETED | 重新打开 | PENDING_ASSIGN | 客服、管理员、客户 | 完成时间在允许窗口内（默认 7 天）；`reopen_count+1` |
| 任意非终态 | 取消 | CANCELLED | 客服、管理员 | 填写取消原因 |
| CANCELLED | 任何操作 | — | — | 终态，禁止一切变更 |

### 5.4 特殊规则

- **幂等与重复执行防护**：接单、分派、确认、评价等关键操作均使用状态前置校验 + 条件更新；重复提交返回明确业务错误，不产生重复记录。
- **并发接单**：两名工程师同时接同一工单，只有一个成功，另一个收到 1403（已被他人接单）。
- **越权**：工程师不能处理非本人负责的工单（1405）；客户不能操作非本人工单（1102）。
- **自动确认**：由 Spring 定时任务扫描超期的 `PENDING_CONFIRM` 工单并转为 `COMPLETED`，记录操作人为系统；是否启用由系统配置控制。
- **逾期判定**：`status NOT IN (COMPLETED, CANCELLED) AND deadline_time < NOW()`，通过 `idx_deadline_status` 高效查询。

---

## 6. 权限与数据隔离设计

### 6.1 认证

- 登录成功后签发 JWT：`sub` = 用户 ID，`jti` = 令牌 ID，`roles`、`exp`（access token 默认 2 小时）。
- refresh token 默认 7 天用于续期；退出时将 `jti` 写入 Redis 黑名单，TTL = 令牌剩余有效期。
- `JwtAuthenticationFilter` 校验签名、有效期与黑名单，构造 `UserPrincipal`（用户 ID、用户名、角色、权限、customerId）。
- 无状态会话（`SessionCreationPolicy.STATELESS`），因使用 Bearer token 且无 Cookie 会话而关闭表单登录与 CSRF。

### 6.2 授权层次

1. **接口级**：`@PreAuthorize("hasAuthority('ticket:create')")`。
2. **资源级（关键）**：`AccessGuard` 在 Service 内做关系校验，例如判定"操作人是否为该工单的客户/负责人/客服/管理员"。
3. **数据域级**：`DataScopeService` 为查询自动附加范围条件——客户角色强制 `customer_id = 当前用户绑定客户`；工程师的"我的工单"强制 `assignee_id = 当前用户`。

**原则**：前端隐藏按钮仅是体验优化，所有权限判断在后端强制执行；通过修改 URL、工单 ID、客户 ID 或请求参数尝试越权的请求都会被资源级校验拦截并返回 403。

### 6.3 权限矩阵

| 功能 | 管理员 | 客服 | 工程师 | 客户 |
| --- | --- | --- | --- | --- |
| 用户/角色/权限管理 | 允许 | 禁止 | 禁止 | 禁止 |
| 故障分类与系统配置 | 允许 | 禁止 | 禁止 | 禁止 |
| 操作日志查看 | 允许 | 禁止 | 禁止 | 禁止 |
| 全局运营统计 | 允许 | 只读 | 仅个人工作量 | 禁止 |
| 客户资料维护 | 允许 | 允许 | 禁止 | 禁止 |
| 设备档案维护 | 允许 | 允许 | 只读 | 仅自身只读 |
| 创建工单 | 允许 | 允许 | 禁止 | 禁止 |
| 分派/转派/退回 | 允许 | 允许 | 转派（仅本人工单） | 禁止 |
| 接单/处理/提交结果 | 允许 | 禁止 | 仅本人负责工单 | 禁止 |
| 确认服务结果 | 允许 | 禁止 | 禁止 | 仅自身工单 |
| 客户评价 | 按配置可代评 | 禁止 | 禁止 | 仅自身工单 |
| 附件上传/下载 | 允许 | 允许 | 仅相关工单 | 仅自身工单 |

---

## 7. 主要 API 设计

### 7.1 认证与个人中心

| 方法 | 路径 | 说明 | 权限 |
| --- | --- | --- | --- |
| POST | `/api/v1/auth/login` | 登录，返回 access/refresh token 与用户信息 | 匿名 |
| POST | `/api/v1/auth/logout` | 退出，令牌加入黑名单 | 登录用户 |
| POST | `/api/v1/auth/refresh` | 刷新令牌 | 持有 refresh token |
| GET | `/api/v1/auth/me` | 当前用户信息（含角色与权限） | 登录用户 |
| PUT | `/api/v1/auth/password` | 修改密码（校验原密码） | 登录用户 |
| PUT | `/api/v1/auth/profile` | 更新个人资料 | 登录用户 |

### 7.2 用户、角色、权限、配置

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET/POST/PUT/DELETE | `/api/v1/users`、`/api/v1/users/{id}` | 用户管理（分页、状态、关键词） |
| PUT | `/api/v1/users/{id}/roles` | 分配角色 |
| PUT | `/api/v1/users/{id}/status` | 启用/禁用 |
| POST | `/api/v1/users/{id}/reset-password` | 重置密码 |
| GET | `/api/v1/roles`、`/api/v1/permissions` | 角色与权限列表 |
| GET/POST/PUT/DELETE | `/api/v1/fault-categories`、`/api/v1/fault-categories/{id}` | 故障分类管理 |
| GET/PUT | `/api/v1/configs`、`/api/v1/configs/{key}` | 系统配置 |
| GET | `/api/v1/operation-logs` | 操作日志（分页 + 条件筛选） |

### 7.3 客户与设备

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/api/v1/customers` | 分页 + 关键词/等级/状态筛选 |
| GET | `/api/v1/customers/{id}` | 详情（含设备与工单数量） |
| POST / PUT / DELETE | `/api/v1/customers`、`/api/v1/customers/{id}` | 新增/修改/停用 |
| GET | `/api/v1/devices` | 分页 + 客户/状态/关键词筛选 |
| GET | `/api/v1/devices/{id}` | 详情（含关联工单） |
| POST / PUT / DELETE | `/api/v1/devices`、`/api/v1/devices/{id}` | 新增/修改/停用 |

### 7.4 工单

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/api/v1/tickets` | 创建工单（生成编号） |
| GET | `/api/v1/tickets` | 分页 + 状态/优先级/客户/负责人/关键词/时间范围筛选（按角色自动收敛数据域） |
| GET | `/api/v1/tickets/{id}` | 详情（含流程、历史、设备、附件、评价） |
| POST | `/api/v1/tickets/{id}/assign` | 分派 |
| POST | `/api/v1/tickets/{id}/accept` | 接单（并发保护） |
| POST | `/api/v1/tickets/{id}/transfer` | 转派 |
| POST | `/api/v1/tickets/{id}/reject` | 退回 |
| POST | `/api/v1/tickets/{id}/process` | 填写处理记录 |
| POST | `/api/v1/tickets/{id}/complete` | 提交处理结果 |
| POST | `/api/v1/tickets/{id}/confirm` | 客户确认 |
| POST | `/api/v1/tickets/{id}/reopen` | 退回重做 / 重新打开 |
| POST | `/api/v1/tickets/{id}/cancel` | 取消 |
| GET | `/api/v1/tickets/{id}/records` | 处理历史 |
| GET/POST | `/api/v1/tickets/{id}/comments` | 评论（区分内部可见性） |
| POST | `/api/v1/tickets/{id}/evaluation` | 提交评价 |
| GET | `/api/v1/tickets/{id}/attachments` | 附件列表 |
| POST | `/api/v1/tickets/{id}/attachments` | 上传附件 |
| GET | `/api/v1/tickets/{id}/attachments/{attachmentId}/download` | 下载（权限校验） |
| DELETE | `/api/v1/tickets/{id}/attachments/{attachmentId}` | 删除附件 |
| GET | `/api/v1/tickets/overdue` | 逾期工单 |

### 7.5 统计与仪表盘

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/api/v1/dashboard/overview` | 总览指标（总数、各状态数、逾期数、平均处理时长、满意度） |
| GET | `/api/v1/dashboard/trend` | 工单趋势（按日/周/月，支持时间范围） |
| GET | `/api/v1/dashboard/fault-distribution` | 故障类型分布 |
| GET | `/api/v1/dashboard/priority-distribution` | 优先级分布 |
| GET | `/api/v1/dashboard/engineer-workload` | 工程师工作量 |
| GET | `/api/v1/dashboard/satisfaction` | 客户满意度统计 |

所有统计接口按角色收敛：管理员/客服看全局，工程师仅本人，客户不可访问。

---

## 8. Redis 设计

### 8.1 用途与键规范

统一前缀 `sf:`，环境区分：`sf:{env}:{module}:{key}`（本地开发 env = `dev`）。

| 键 | 类型 | TTL | 用途 | 失效时机 |
| --- | --- | --- | --- | --- |
| `sf:dev:dashboard:overview` | String(JSON) | 60s | 仪表盘总览指标 | 工单创建/状态变更/评价提交后删除 |
| `sf:dev:stats:trend:{range}` | String(JSON) | 300s | 工单趋势统计 | 工单创建/状态变更后按模式删除 |
| `sf:dev:stats:fault-distribution` | String(JSON) | 300s | 故障类型分布 | 工单创建/更新故障分类后删除 |
| `sf:dev:stats:engineer-workload` | String(JSON) | 300s | 工程师工作量 | 接单/完成/转派后删除 |
| `sf:dev:stats:satisfaction` | String(JSON) | 600s | 满意度统计 | 评价提交后删除 |
| `sf:dev:config:all` | String(JSON) | 1800s | 系统配置字典 | 配置更新后删除 |
| `sf:dev:token:blacklist:{jti}` | String | 令牌剩余有效期 | 注销令牌 | 到期自动清除 |
| `sf:dev:lock:ticket:accept:{ticketId}` | String(SET NX EX) | 10s | 并发接单锁 | 接单完成后释放 |

### 8.2 缓存原则

- **不缓存**：用户凭据、密码哈希、私钥、令牌明文（黑名单仅存 `jti`）、附件二进制内容。
- **不过度缓存**：只缓存高频读取且计算代价高的统计结果与系统配置，不做全量查询缓存。
- **失效策略**：写操作提交成功后主动精确删除对应键（不在事务提交前删除），TTL 作为兜底。
- **降级策略**：所有缓存读写包裹异常处理，Redis 不可用时记录 WARN 日志并直接查库；分布式锁不可用时依赖数据库条件更新保证正确性，功能不中断。

---

## 9. 安全设计

| 项 | 措施 |
| --- | --- |
| 密码存储 | BCrypt（强度 10），禁止明文与可逆加密 |
| 登录防护 | 连续失败 5 次锁定 15 分钟，记录失败日志 |
| 令牌 | 短有效期 access token + refresh token；退出即拉黑；密钥来自环境变量 |
| SQL 注入 | 全部使用参数占位（`#{}`）；排序字段白名单校验 |
| 越权 | 接口级 + 资源级 + 数据域级三层校验，覆盖"改 ID 越权"场景 |
| 敏感数据泄露 | VO 不返回密码与内部标记；日志脱敏；生产环境不返回堆栈信息 |
| 文件上传 | 扩展名与 MIME 白名单、大小上限（默认 20MB）、存储名重命名、SHA-256 校验、禁止路径穿越、上传目录不在 Web 根目录且不进 Git |
| 附件下载 | 先校验工单访问权限，再流式响应，不暴露物理路径 |
| 审计 | 关键操作写入 `sys_operation_log`，包含失败请求 |
| 配置安全 | 数据库密码、JWT 密钥、Redis 密码均通过环境变量注入，仓库仅保留 `.env.example` |

---

## 10. 日志规范

- 使用 SLF4J + Logback，输出格式包含时间、级别、traceId、线程、类名与消息。
- traceId 由过滤器生成（或沿用请求头 `X-Trace-Id`），写入 MDC 并回传响应头，便于问题定位。
- 访问日志记录方法、路径、状态码与耗时；异常请求与慢请求提升为 WARN。
- 禁止打印密码、令牌等敏感信息；手机号、邮箱按需掩码。
- 日志按天滚动，本地保留 15 天。

---

## 11. 前端设计

### 11.1 页面清单

| 页面 | 路由 | 可见角色 |
| --- | --- | --- |
| 登录页 | `/login` | 匿名 |
| 售后运营仪表盘 | `/dashboard` | 管理员、客服、工程师 |
| 工单列表 | `/tickets` | 全部（数据域收敛） |
| 工单创建 | `/tickets/create` | 管理员、客服 |
| 工单详情 | `/tickets/:id` | 相关人 |
| 工单处理工作台 | `/workspace` | 工程师 |
| 客户管理 | `/customers` | 管理员、客服 |
| 设备档案 | `/devices` | 管理员、客服、工程师只读 |
| 客户评价 | `/evaluations` | 管理员、客服（只读）、客户（提交） |
| 用户与角色管理 | `/system/users`、`/system/roles` | 管理员 |
| 操作日志 | `/system/logs` | 管理员 |
| 个人中心 | `/profile` | 全部 |
| 404 页面 | `/:pathMatch(.*)*` | 全部 |

### 11.2 交互与状态约定

- 采用统一的现代蓝白企业 SaaS 风格，使用 CSS 变量定义主色、圆角、阴影与间距。
- 布局统一：左侧侧边栏（按角色过滤菜单）+ 顶部导航（用户信息、通知）+ 面包屑 + 内容区。
- 列表页统一结构：搜索条件区（可折叠/重置）+ 工具栏（新增/批量操作）+ 表格（分页、排序）+ 状态标签。
- 所有请求必须有加载态与错误反馈；空数据、请求失败、无权限分别提供明确提示与恢复操作。
- 表单统一校验规则（必填、长度、格式、范围），提交按钮带 loading 与防重复提交。
- 按钮与操作按角色显示，但后端仍强制校验（前端不承担安全职责）。
- ECharts 图表数据全部来自后端统计接口，禁止硬编码数字。

### 11.3 前端状态管理

- `auth` store：token、用户信息、角色与权限集合、登录/登出/刷新。
- `app` store：侧边栏折叠、面包屑、页签。
- `dict` store：故障分类、状态、优先级等字典（来自后端配置/字典接口）。
- 路由守卫：未登录跳转登录页；无权限跳转 403/404；登录后按角色重定向默认页。

---

## 12. 测试设计

### 12.1 后端测试分层

| 类型 | 依赖 | 运行方式 | 覆盖内容 |
| --- | --- | --- | --- |
| 单元测试 | 无（Mockito） | `mvn test` | 状态机、权限判定、编号生成、参数校验器、缓存键与失效逻辑、文件名校验 |
| 集成测试 | 真实 MySQL + Redis | `mvn verify -Pintegration` | 认证、越权、数据隔离、工单全流程、并发接单、统计数值、缓存失效与降级、附件权限 |

### 12.2 关键测试用例设计（示例）

- **并发接单**：两个线程同时对接单接口发起请求（`CountDownLatch` 同步），断言恰好一个成功、一个返回 1403，且数据库只有一条有效分派记录。
- **越权访问**：客户 A 请求客户 B 的工单详情，断言返回 403 且不泄露数据。
- **非法状态跳转**：对 `PENDING_ASSIGN` 工单调用"接单"，断言返回 1402。
- **评价条件**：对未完成工单、已评价工单、非本人客户工单分别调用评价接口，断言返回 1501/1502/1102。
- **统计正确性**：插入确定性演示数据后断言各统计接口返回的数值与手工计算结果一致。
- **缓存失效**：先请求统计接口（产生缓存），再创建一条工单，断言缓存键被删除且再次请求返回更新后的数值。
- **降级**：使用不可用的 Redis 连接配置启动上下文（或 Mock 抛出连接异常），断言统计接口仍返回正确数据。

### 12.3 前端测试

- 组件渲染：列表、表单、状态标签、空态与错误态。
- 表单校验：必填、格式、提交前拦截与错误提示。
- API 交互：成功与失败（401/403/500）路径的提示与状态回滚。
- 路由守卫：未登录跳转、无权限跳转、按角色生成菜单。

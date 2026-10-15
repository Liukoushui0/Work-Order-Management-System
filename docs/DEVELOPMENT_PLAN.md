# ServiceFlow 企业售后工单管理系统 — 开发计划

> 文档版本：v1.0
> 阶段：Stage 0（环境检查与计划）
> 适用范围：本项目全部开发阶段（Stage 0 ~ Stage 9）
> 关联文档：[TECHNICAL_DESIGN.md](./TECHNICAL_DESIGN.md)、[GIT_WORKFLOW.md](./GIT_WORKFLOW.md)、[STAGE0_ENVIRONMENT_CHECK.md](./STAGE0_ENVIRONMENT_CHECK.md)

## 1. 项目目标与范围

### 1.1 目标

开发一个**真实可运行**的企业售后工单管理系统（ServiceFlow / Work Order Management System），面向销售设备、智能硬件或软件产品的企业售后团队，覆盖：

- 客户报修与客服建单
- 工单派发、接单、转派与维修处理
- 处理记录与客户评价
- 附件管理、超时提醒与操作审计
- 售后运营数据统计

项目定位为 Java 后端 / 全栈开发实习与校招作品，因此**业务完整性与可运行性优先于技术炫技**。

### 1.2 明确的非目标

- 不做微服务拆分（采用模块化单体）。
- 不使用 Docker / Docker Compose 或任何容器化部署方式。
- 不伪造测试结果、接口响应、截图或未完成的功能。
- 不以内存集合或前端假数据替代真实业务数据。

### 1.3 技术栈（锁定版本，详见 TECHNICAL_DESIGN.md 第 2 章）

后端：Java 17、Spring Boot 3.3.x、Maven、Spring MVC、Spring Security、JWT、MyBatis-Plus、MySQL 8、Redis、Springdoc OpenAPI、Hibernate Validator、JUnit 5、Mockito。

前端：Vue 3、JavaScript（ES2022，不使用 TypeScript）、Vite、Element Plus、Vue Router、Pinia、Axios、ECharts、Vitest、Vue Test Utils、ESLint、Prettier。

工具：Git、GitHub、Gitee、IntelliJ IDEA、VS Code。

---

## 2. 总体实施原则

1. **业务流程优先**：每个模块都必须具备真实业务规则（状态机、权限、数据隔离、并发保护），不接受"只有增删改查"的实现。
2. **先跑通，再做全**：每个阶段都必须以"可编译、可测试、可运行"为下限，不允许出现长期不可运行的分支状态。
3. **权限在后端**：所有权限校验在后端实施，前端只做体验层控制。
4. **测试锚定真实实现**：测试必须能因实现错误而失败，禁止"永远返回成功"的测试。
5. **每阶段闭环**：开发 → 测试 → 审查 → 提交 → 合并 → 双远程同步 → 汇报。
6. **环境问题显式化**：遇到环境限制时记录真实现象、执行替代验证，并如实告知，不掩盖、不跳过。

---

## 3. 阶段划分与验收标准

### 通用阶段执行顺序（每阶段均须遵守）

1. 明确本阶段目标与验收标准。
2. 检查当前分支与工作区状态。
3. 实现本阶段功能。
4. 运行与改动相关的测试。
5. 检查编译、类型、格式与静态检查结果。
6. 执行 `git diff --check`。
7. 审查 `git diff` 与 `git status`。
8. 确认无敏感文件、构建产物、临时文件或无关修改。
9. 更新必要文档。
10. 暂存本阶段相关文件。
11. 再次检查暂存区差异。
12. 创建符合 Conventional Commits 规范的提交。
13. 阶段验收通过后合并到 `main`。
14. 将 `main` 同步到 `github` 与 `gitee`。
15. 检查两个远程仓库的推送结果。

---

### Stage 0：环境检查、开发计划与技术设计

**目标**：在不修改任何既有文件的前提下，完成环境与仓库状态审计，产出开发计划、技术设计、Git 工作流三份文档，并向用户汇报。

**交付物**

- `docs/DEVELOPMENT_PLAN.md`（本文档）
- `docs/TECHNICAL_DESIGN.md`
- `docs/GIT_WORKFLOW.md`
- `docs/STAGE0_ENVIRONMENT_CHECK.md`（环境检查原始记录）

**只读检查项**

- 当前目录是否为 `E:\Work Order Management System`
- 是否已有代码、文档、配置
- Git 工作区状态、当前分支
- 本地 `user.name` / `user.email`
- `git remote -v` 是否包含 `github` 与 `gitee`
- 两个远程仓库当前是否有提交
- Java、Maven、Node.js、npm 可用性
- MySQL、Redis 安装与连通性
- `.gitignore` 是否存在于合理配置
- 是否存在 `.env`、数据库密码、私钥、令牌等敏感信息

**禁止操作**：`git init`、重复添加远程、`git remote remove`、`git reset --hard`、`git clean -fd`、`git push --force`、覆盖远程 README/LICENSE/已有提交、在终端输出或提交真实密码/令牌/私钥。

**验收标准**

- 三份文档生成且内容与本机实际情况一致（不臆测环境）。
- 环境风险已被识别并给出处理方案或待确认项。
- 完成环境检查与开发计划汇报，等待用户确认后再进入 Stage 1。

**测试**：本阶段无代码，测试为只读检查命令的实际输出核对。

**Git 提交**：经用户确认后将三份文档作为初始提交（`docs(project): ...`）。初始提交必须在 Stage 1 建立 `.gitignore` 之前完成或与之合并考虑，避免把构建产物纳入版本控制。

---

### Stage 1：工程骨架与基础设施

**目标**：搭建可编译、可启动、可构建的前后端工程骨架，建立统一的基础设施代码。

**后端交付物**

- `backend/pom.xml`（Spring Boot 3.3.x 父 POM，锁定依赖版本）
- Maven Wrapper（`mvnw` / `mvnw.cmd` / `.mvn/wrapper`），锁定 Maven 版本，避免依赖全局 Maven 的 PATH 配置
- 包结构：`controller` / `service` / `mapper` / `entity` / `dto` / `vo` / `security` / `config` / `exception` / `common`
- 统一响应结构 `Result<T>` 与统一异常体系（`BusinessException`、错误码枚举 `ErrorCode`）
- 全局异常处理器 `GlobalExceptionHandler`（含参数校验异常、权限异常、认证异常、兜底异常）
- 统一分页请求/响应对象 `PageRequest` / `PageResult<T>`
- 结构化日志配置（`logback-spring.xml`，含 traceId MDC）
- 多环境配置文件：`application.yml`、`application-dev.yml`、`application-test.yml`（敏感项使用环境变量占位）
- Springdoc OpenAPI 基础配置

**前端交付物**

- `frontend/`（Vite + Vue 3 + JavaScript 工程）
- 目录：`api` / `assets` / `components` / `layouts` / `router` / `stores` / `styles` / `types` / `utils` / `views`
- Axios 封装（统一 baseURL、请求/响应拦截、错误提示、token 注入）
- Pinia 初始化、Vue Router 骨架、Element Plus 引入
- ESLint（flat config）+ Prettier + `.editorconfig`
- `package.json` 脚本：`dev` / `build` / `lint` / `format` / `test`

**工程配置交付物**

- `.gitignore`（Java、Maven、Node、IDE、日志、上传目录、本地配置）
- `.env.example` 与环境配置示例（不含真实凭据）
- `README.md` 骨架

**验收标准**

- `mvn -q clean compile` 与 `mvn -q clean test` 通过（无测试时以编译与骨架测试为准）。
- `mvn package` 产出可执行 jar，应用能以 `dev` profile 启动并响应健康检查接口。
- `npm install`、`npm run lint`、`npm run test`、`npm run build` 全部通过（本项目不使用 TypeScript，因此没有类型检查步骤）。
- 前后端能同时启动，前端能成功调用后端健康检查/基础接口。
- 确认没有构建产物或本地配置进入暂存区。

**风险依赖**：需要 JDK 17（Stage 0 已确认为环境缺口）。

**提交拆分建议**：`build(backend): add spring boot project skeleton`、`build(frontend): add vite vue3 javascript skeleton`、`feat(common): add unified response and exception handling`、`chore(project): add gitignore and env examples`。

---

### Stage 2：数据库、认证与权限

**目标**：建立完整数据模型与脚本，实现登录、退出、密码哈希、JWT 身份认证和四角色权限控制。

**交付物**

- `backend/src/main/resources/db/schema.sql`（15 张表的建表语句、索引、唯一约束）
- `backend/src/main/resources/db/data.sql`（演示数据，与结构脚本分离）
- `docs/DATABASE_DESIGN.md`（数据库结构设计文档，Stage 9 继续完善）
- 认证：`/api/v1/auth/login`、`/api/v1/auth/logout`、`/api/v1/auth/refresh`、`/api/v1/auth/me`
- BCrypt 密码哈希，登录失败计数与锁定策略
- JWT 签发/校验过滤器、无状态安全配置、方法级权限（`@PreAuthorize`）
- RBAC：用户、角色、权限、用户角色关系、角色权限关系
- 四种角色初始化：`ROLE_ADMIN`、`ROLE_SERVICE`、`ROLE_ENGINEER`、`ROLE_CUSTOMER`
- 数据域隔离基础能力（`DataScopeService`，客户角色仅可见自身数据）
- 操作日志切面（`@OperationLog` 注解 + AOP），记录失败请求
- 统一错误处理覆盖认证/鉴权失败

**验收标准**

- 数据库脚本可在空库一次性执行成功，重复执行不会破坏已有数据（幂等或明确重建策略）。
- 四种角色均可登录；密码在库中为 BCrypt 哈希。
- 未登录访问受保护接口返回统一 401 结构；越权访问返回统一 403 结构。
- 认证与越权测试全部通过。

**测试**

- 单元测试：密码哈希、JWT 生成/解析/过期、权限判定、登录失败锁定。
- 集成测试（真实 MySQL + Redis）：登录成功/失败、未登录访问、四角色权限边界、客户数据隔离、退出后 token 失效。

**提交拆分建议**：`feat(db): add schema and demo data scripts`、`feat(auth): implement jwt authentication`、`feat(rbac): implement role and permission control`、`test(auth): add authentication and authorization tests`。

---

### Stage 3：客户与设备档案管理

**目标**：完成客户资料与客户设备档案两个模块的后端能力与测试。

**交付物**

- 客户 CRUD：列表（分页/关键词/等级/状态筛选）、详情、新增、修改、停用
- 设备档案 CRUD：列表（分页/关键词/客户/状态筛选）、详情（含关联工单数量）、新增、修改
- 设备与客户的关联完整性校验（设备必须归属有效客户）
- 客户登录账号与客户档案的绑定
- 权限：管理员/客服可维护，工程师只读，客户角色仅可见自身客户与设备
- 参数校验（长度、格式、手机号、必填、枚举）

**验收标准**

- 分页、筛选、排序行为正确，非法参数返回统一校验错误结构。
- 客户角色越权访问其他客户数据被拒绝（含通过修改 URL/ID 的方式）。
- 业务测试覆盖数据隔离与关联校验。

**提交拆分建议**：`feat(customer): implement customer management`、`feat(device): implement device archive management`、`test(customer): add customer and device isolation tests`。

---

### Stage 4：工单核心业务（创建、分派、接单、转派、处理、状态机）

**目标**：实现工单全生命周期主流程，这是项目最核心的阶段。

**交付物**

- 工单创建：编号生成（唯一、可读、含日期与序列）、客户/设备/故障分类/优先级/截止时间校验
- 工单查询：列表（分页 + 状态、优先级、客户、负责人、关键词、时间范围筛选）、详情（含流程、历史、设备）
- 工单分派、接单、转派、退回、维修处理、完成、取消、重新打开
- 状态机（见 TECHNICAL_DESIGN.md 第 5 章）：`PENDING_ASSIGN → PENDING_ACCEPT → PROCESSING → PENDING_CONFIRM → COMPLETED`，以及 `CANCELLED`、退回、转派、重新打开规则
- 处理历史（`ticket_process_record`）与状态变更留痕（操作人、前后状态、时间、内容）
- 事务边界与并发控制：版本号乐观锁 + 条件更新；接单使用 Redis 锁 + 数据库兜底
- 逾期工单查询（`deadline_time` 索引 + 逾期标识）
- 处理时长与人员工作量基础统计字段（供 Stage 6 使用）

**验收标准**

- 非法状态跳转被拒绝并返回明确业务错误码。
- 重复接单/重复分派只能成功一次（并发测试验证）。
- 每次状态变更均产生处理记录，且记录操作人。
- 客户与工程师只能访问与自己相关的工单。

**测试**

- 单元测试：状态机转换矩阵、编号生成唯一性、权限判定。
- 集成测试：创建与查询、分派、重复接单（并发）、非法跳转、取消后不可处理、逾期查询、跨角色越权。

**提交拆分建议**：`feat(ticket): implement work order creation and query`、`feat(ticket): implement assignment and acceptance workflow`、`feat(ticket): add process history and state machine`、`test(ticket): add state transition and concurrency tests`。

---

### Stage 5：评价、附件与操作记录

**目标**：完成客户评价、工单附件上传下载、工单操作记录的闭环。

**交付物**

- 客户评价：仅"已完成"且未评价的工单可评价；评分（1~5）、满意度、维度评分、标签、内容；一单一评（数据库唯一约束兜底）
- 附件：上传、列表、下载、删除（软删除）；文件类型与大小白名单校验；文件名安全处理（防路径穿越）；SHA-256 校验；上传目录可配置且不进 Git
- 附件访问控制：工单相关人可访问，其他人返回 403
- 工单操作记录与评论（区分内部备注与客户可见）
- 服务端文件存储目录结构与元数据表保持一致性（落盘与入库的事务/补偿策略）

**验收标准**

- 不满足条件的工单评价被拒绝（未完成、已评价、非本人客户）。
- 非法文件类型/超大文件被拒绝；伪造路径的下载请求无法读取目录外文件。
- 无权限用户下载附件返回 403。

**测试**：单元测试（文件名清洗、类型校验、评价条件）+ 集成测试（评价幂等、附件权限、附件元数据一致性）。

**提交拆分建议**：`feat(evaluation): implement customer service evaluation`、`feat(attachment): implement ticket attachment management`、`test(attachment): add attachment access control tests`。

---

### Stage 6：运营统计与 Redis 缓存

**目标**：提供售后运营统计接口，并用 Redis 缓存热点统计结果，明确失效策略与降级策略。

**交付物**

- 统计接口：工单趋势（按日/周/月）、故障类型分布、优先级分布、逾期工单、处理效率（平均处理时长、平均响应时长）、工程师工作量、客户满意度
- 仪表盘接口：总览指标 + 图表数据（一次请求聚合，避免前端多次往返）
- Redis 缓存：仪表盘/统计结果缓存（键规范、TTL、失效规则），系统配置缓存，注销 token 黑名单，接单分布式锁
- 缓存失效：工单状态变更/新增/评价提交时精确失效相关键
- 降级策略：Redis 不可用时统计走数据库、锁退化为数据库条件更新，核心业务不受影响
- 缓存与统计测试：统计结果正确性断言 + 缓存命中/失效断言 + Redis 不可用降级测试

**验收标准**

- 统计数值由数据库真实计算，测试用固定演示数据断言具体数值。
- 数据更新后缓存正确失效，不出现陈旧数据。
- Redis 停止时核心业务与统计接口仍可用（验证降级路径）。

**提交拆分建议**：`feat(dashboard): implement ticket statistics`、`perf(cache): add redis cache with invalidation`、`test(cache): add cache invalidation and degradation tests`。

---

### Stage 7：前端业务页面与体验完善

**目标**：完成全部核心业务页面并与真实后端 API 对接，统一 UI 风格与交互反馈。

**阶段顺序说明（重要）**

为避免重复开发，**前端页面与其对应后端阶段同步开发**：Stage 2 产出登录页与布局骨架，Stage 3 产出客户/设备页面，Stage 4 产出工单列表/创建/详情/工作台，Stage 5 产出评价与附件交互，Stage 6 产出仪表盘图表。Stage 7 为**收口阶段**，负责：

- 补齐剩余页面：用户与角色管理、操作日志、个人中心、404
- 统一视觉规范（现代蓝白企业 SaaS 风格）：侧边栏、顶部导航、面包屑、表格、表单、按钮、空态
- 统一状态反馈：加载中、空数据、请求失败、无权限
- 统一表单校验与提交反馈
- 依据角色收敛操作入口（后端仍强制校验）
- 前端测试补齐：组件渲染、表单校验、API 异常反馈、路由权限、核心交互

**验收标准**

- 13 个页面全部可用，所有按钮都有真实行为。
- 工单列表支持分页、状态筛选、优先级筛选与关键词查询。
- 工单详情展示处理流程、操作历史与关联设备。
- ECharts 使用后端真实统计数据，无硬编码假数字。
- `npm run lint`、`npm run test`、`npm run build` 全部通过。

**提交拆分建议**：`feat(web): implement ticket management pages`、`feat(web): implement dashboard charts`、`feat(web): add user role and log pages`、`style(web): unify layout and form feedback`、`test(web): add component and router guard tests`。

---

### Stage 8：整体验收、安全审查与打包

**目标**：执行完整测试，审查越权、状态机、事务与并发问题，检查敏感配置、日志与文件访问安全，修复真实发现的问题。

**交付物**

- 完整后端测试执行结果（单元 + 集成）
- 完整前端检查结果（lint / test / build）
- 安全审查记录与修复提交
- 打包产物验证（可执行 jar + 前端 dist）

**验收标准**

- 所有测试真实通过，失败项已修复或明确记录为环境限制并给出替代验证。
- 无明文密码、令牌、私钥进入仓库。
- 日志不输出密码、token 等敏感字段。
- 打包产物可启动并完成一次端到端核心业务验证。

**提交拆分建议**：`fix(security): ...`（按实际发现问题命名）、`test(project): add end-to-end acceptance tests`。

---

### Stage 9：文档与最终交付

**目标**：补齐交付文档，核对 Git 历史与双远程同步状态，输出最终交付报告。

**交付物**

- `README.md`（安装 JDK 17 / Maven / Node.js、MySQL 8 初始化、Redis 7 启动、后端配置与启动、前端启动、访问入口与 API 文档、演示账号与角色权限、常见错误与排查）
- `docs/DATABASE_DESIGN.md`（表结构、索引、约束、事务边界、并发策略）
- `docs/API_DOCUMENTATION.md`（主要接口清单与说明，配合 OpenAPI）
- `docs/LOCAL_DEPLOYMENT.md`（本地部署与故障排查）
- `docs/TEST_REPORT.md`（测试命令与真实结果）
- `docs/FINAL_DELIVERY.md`（最终交付报告：功能、架构、表与 API、测试、启动方式、演示账号、Git 记录、双远程推送状态、未完成事项）

**验收标准**

- 文档经核对与实际项目一致，不含未实现功能的描述。
- 演示账号仅用于本地开发环境，明确标注，不出现生产默认弱密码。
- GitHub 与 Gitee 的 `main` 分支同步情况已实际检查并记录。

---

## 4. 测试策略总览

### 4.1 后端

| 层级 | 范围 | 运行方式 |
| --- | --- | --- |
| 单元测试 | 状态机、权限判定、编号生成、校验器、缓存键与失效逻辑、文件名校验 | `mvn test`（默认，无需外部依赖） |
| 集成测试 | 认证、鉴权、数据隔离、工单流转、并发接单、统计正确性、缓存失效、附件权限 | `mvn verify`（Maven Failsafe 按 `*IntegrationTest` 命名约定执行，需本地 MySQL 8 + Redis） |
| 接口级手工验证 | OpenAPI 文档与实际响应结构一致性 | 启动应用后通过 Swagger UI / HTTP 客户端验证 |

**约定**：集成测试统一打 `@Tag("integration")` 并以 `*IntegrationTest` 命名，由 Maven Failsafe 插件在 `verify` 阶段执行；`mvn test`（Surefire）排除它们，因此在没有数据库的机器上依然可运行。阶段验收必须执行完整测试，即 `mvn verify`。

### 4.2 前端

| 范围 | 工具 | 命令 |
| --- | --- | --- |
| 组件渲染与交互 | Vitest + Vue Test Utils | `npm run test` |
| 语言与质量 | JavaScript + ESLint（不使用 TypeScript，无类型检查步骤） | `npm run lint` |
| 规范检查 | ESLint + Prettier | `npm run lint` |
| 构建验证 | Vite | `npm run build` |

### 4.3 必须覆盖的场景（来自需求）

登录成功/失败、未登录访问受保护接口、不同角色权限边界、客户数据隔离、工单创建与查询、工单状态机、工单分派与重复接单、非法状态跳转、工单完成与评价条件、参数校验、统计结果正确性、缓存失效、附件访问权限；前端组件渲染、表单验证、API 调用与异常反馈、路由权限、核心交互。

---

## 5. 风险与应对

| # | 风险 | 影响 | 应对 |
| --- | --- | --- | --- |
| R1 | 本机只有 JDK 8，无 JDK 17 | Spring Boot 3.x 无法编译运行 | 安装 JDK 17 并配置项目级 `JAVA_HOME`；不改动全局环境前先取得用户确认 |
| R2 | Maven 未加入 PATH（已安装在 `D:\Tools\envs\Maven\apache-maven-3.10.0`） | 命令行构建不可用 | 使用 Maven Wrapper（`mvnw`）锁定版本；全路径/包装器方式调用 |
| R3 | MySQL `root` 密码未知 | 无法建库、无法跑集成测试 | 向用户索取凭据或由用户创建受限账号（仅限本地开发库） |
| R4 | 本机 Redis 为 Windows 版 5.0.14.1，非 Redis 7，且当前未运行 | 与需求声明的 Redis 7 不一致 | 记录偏差，提供三种方案供用户选择（沿用 5.0.14 / Memurai / WSL2 安装 Redis 7），并在 README 与最终报告中如实说明实际版本 |
| R5 | Maven 本地仓库为空 | 首次构建需下载全部依赖，耗时较长 | 已确认阿里云镜像与 Maven Central 可达，使用镜像加速并预留下载时间 |
| R6 | `github.com:443` 直连不可用 | 推送 GitHub 失败 | 通过本机代理（127.0.0.1:7897）访问；在 Git 工作流文档中记录并作为同步步骤的显式处理项 |
| R7 | Git 凭据助手未配置，无 `.git-credentials` | 推送需交互输入凭据 | 推送前与用户确认凭据方式（HTTPS + Personal Access Token），不写入仓库、不出现在日志中 |
| R8 | 工单并发操作 | 重复接单、状态错乱 | 版本号乐观锁 + 条件更新 + Redis 锁 + 唯一约束兜底 |
| R9 | 附件目录可能被提交 | 仓储污染 | `.gitignore` 排除上传目录，仅保留 `.gitkeep` |

---

## 6. 演示数据与账号规划

- 演示数据脚本 `db/data.sql` 与结构脚本分离，可独立执行、可重复初始化。
- 四种角色各提供至少一个本地开发演示账号，密码在文档中明确标注为"仅本地开发使用"。
- 演示数据覆盖：客户、设备、工单各状态样本、逾期工单、评价样本，确保统计与图表有真实数据可算。
- 生产配置不保留任何默认弱密码；敏感项通过环境变量注入。

---

## 7. 完成定义（Definition of Done）

一个阶段只有同时满足以下条件才算完成：

1. 功能按验收标准实现，无占位或空实现。
2. 相关测试真实运行并通过（有真实测试输出）。
3. 编译、类型检查、格式检查通过。
4. `git diff --check` 无问题，暂存区内容与阶段范围一致。
5. 无敏感文件、构建产物、临时文件入库。
6. 必要的文档已更新。
7. 提交信息符合 Conventional Commits。
8. 已合并到 `main` 并成功推送到 `github` 与 `gitee`（或如实记录失败原因与待推送状态）。
9. 已向用户输出阶段报告。

**未通过验收的阶段不得合并到 `main`，也不得对外声称已完成。**

---

## 8. 阶段进度表

| 阶段 | 名称 | 状态 | 验收结果 |
| --- | --- | --- | --- |
| Stage 0 | 环境检查、开发计划与技术设计 | 已完成（2026-10-15） | 通过：四份文档已产出；三个环境阻塞项（JDK 17 / MySQL 凭据 / Redis）经用户确认后处理完毕；首次提交 `38772a3` 已推送至 `github` 与 `gitee`，三处 `main` 指向同一提交 |
| Stage 1 | 工程骨架与基础设施 | 已完成 | 通过：后端 9 个测试与前端 10 个测试全部通过；`mvn package`、`npm run lint/test/build` 均通过；前后端联调实测成功（Vite 代理 → 后端 → MySQL/Redis 全 UP）。过程中修复 2 个真实缺陷：Logback 1.5 不再支持 `SizeAndTimeBasedFNATP`、Element Plus 2.8.8 `el-tag` 挂载钩子空指针；并按用户要求将前端由 TypeScript 迁移为 JavaScript（移除 `npm run type-check`）。详见 TECHNICAL_DESIGN.md 第 2.3.1 节 |
| Stage 2 | 数据库、认证与权限 | 已完成 | 通过：16 张表与基础数据脚本落地并实测可重复执行；后端 40 个单元测试 + 23 个集成测试全部通过；README/DATABASE_DESIGN 更新。过程中修复 3 个真实缺陷：登录失败计数被事务回滚导致锁定永不生效、`@PreAuthorize` 拒绝被兜底成 500、Redis 实例 RDB 持久化失败进入 MISCONF 导致黑名单写入静默失效 |
| Stage 3 | 客户与设备档案管理 | 已完成 | 通过：客户与设备档案的增删改查、分页筛选、参数校验、关联完整性（客户停用/删除保护、设备编号与序列号唯一、设备存在工单不可删）落地；后端 54 个单元测试 + 34 个集成测试全部通过；演示业务数据（4 客户 / 8 设备）与账号绑定脚本化并验证幂等。过程中修复 1 个真实授权缺口：客户角色缺少 `customer:view`，导致客户看不到自己的客户档案 |
| Stage 4 | 工单核心业务 | 已完成 | 通过：工单创建与编号生成、列表/详情/逾期查询、分派/接单/转派/退回/处理/提交/确认/重开/取消全流程、状态机与处理历史、乐观锁 + 条件更新 + Redis 锁并发保护落地；后端 73 个单元测试 + 47 个集成测试全部通过，并用打包产物做了真实 HTTP 全流程冒烟。过程中修复 3 个真实缺陷：并发建单在 REPEATABLE READ 下重试风暴并死锁（改用序列表原子取号）、不带状态筛选的工单列表空指针、`accept()` 的 1403 分支不可达 |
| Stage 5 | 评价、附件与操作记录 | 已完成 | 通过：客户评价（一单一评、仅已完成且未评价、仅所属客户、匿名隐藏）、附件上传/下载/删除（扩展名与大小白名单取自系统配置、文件名清洗、UUID 存储名、SHA-256、路径穿越防护、按工单权限校验、删除同步清理物理文件）、内部备注与客户沟通评论（客户强制公开、内部备注不对客户暴露）落地；后端 84 个单元测试 + 55 个集成测试全部通过。首次全量运行即通过，未发现新缺陷 |
| Stage 6 | 运营统计与 Redis 缓存 | 已完成 | 通过：总览、趋势（日/周/月）、故障类型分布、优先级分布、工程师工作量、客户满意度六个统计接口落地，数值与数据库真实聚合逐项比对；Redis 缓存（带数据范围隔离的键、60s/300s/600s TTL、事务提交后失效、Redis 不可用降级）与超期自动确认、逾期提醒定时任务落地；后端 98 个单元测试 + 64 个集成测试全部通过 |
| Stage 7 | 前端业务页面与体验完善 | 未开始 | — |
| Stage 8 | 整体验收、安全审查与打包 | 未开始 | — |
| Stage 9 | 文档与最终交付 | 未开始 | — |

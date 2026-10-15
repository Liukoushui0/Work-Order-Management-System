# ServiceFlow 测试报告

> 执行环境：Windows 11、JDK 17（Temurin 17.0.20.1）、Maven 3.10.0、Node.js 24.21.0、MySQL 8.0.46、Redis 5.0.14.1
> 最近一次完整执行：2026-10-15（Stage 8 验收）

## 1. 测试规模总览

| 层次 | 用例数 | 结果 |
| --- | --- | --- |
| 后端单元测试（Surefire，不依赖外部服务） | 98 | 全部通过 |
| 后端集成测试（Failsafe，真实 MySQL + Redis） | 64 | 全部通过 |
| 前端组件与路由测试（Vitest + Vue Test Utils） | 25 | 全部通过 |
| **合计** | **187** | **全部通过** |

## 2. 命令与结果

| 命令 | 覆盖范围 | 结果 |
| --- | --- | --- |
| `mvn clean test` | 后端单元测试 | `Tests run: 98, Failures: 0, Errors: 0` — BUILD SUCCESS |
| `mvn clean verify` | 单元 + 集成测试 | 单元 98 + 集成 64 全部通过；BUILD SUCCESS |
| `mvn clean package` | 打包 | 产出 `serviceflow-backend-1.0.0-SNAPSHOT.jar` |
| `npm run lint` | 前端规范（`--max-warnings 0`） | 退出码 0 |
| `npm run test` | 前端单元测试 | `Test Files 7 passed / Tests 25 passed` |
| `npm run build` | 前端构建 | 成功，主包 51 KB（gzip 21 KB） |

> 本项目前端使用 JavaScript（不使用 TypeScript），因此没有 `npm run type-check` 步骤；执行该命令会返回 `Missing script: "type-check"`。

## 3. 后端单元测试分布

| 测试类 | 用例数 | 覆盖内容 |
| --- | --- | --- |
| `ResultTest` | 3 | 统一响应结构、traceId、分页页数计算 |
| `PageQueryTest` | 3 | 分页默认值、排序白名单、排序方向归一化 |
| `TicketStatusTest` | 6 | 状态机矩阵：主流程、特殊流转、非法跳转、终态、状态解析 |
| `TicketNoGeneratorTest` | 2 | 编号格式（SO + yyyyMMdd + 6 位）、序号溢出保护 |
| `FileNameUtilsTest` | 6 | 文件名清洗、危险字符、超长截断、扩展名、路径穿越拒绝 |
| `JwtTokenProviderTest` | 6 | 令牌签发/解析、类型区分、篡改与过期拒绝、弱密钥拒绝、随机密钥兜底 |
| `TokenBlacklistServiceTest` | 4 | 黑名单写入与 TTL、命中判定、Redis 不可用降级 |
| `AccessGuardTest` | 5 | 数据域校验、权限校验、管理员放行、未登录拒绝 |
| `AuthServiceTest` | 8 | 登录成功/失败/锁定/禁用/不存在、刷新令牌、退出登录 |
| `LoginAttemptServiceTest` | 4 | 失败计数、阈值锁定、成功清零、未知用户处理 |
| `UserServiceTest` | 7 | 非法排序、账号重复、角色校验、BCrypt 写入、不能禁用/删除自己、不能移除最后管理员 |
| `CustomerServiceTest` | 7 | 编号重复、默认状态、数据域、客户不存在、停用保护、删除保护、停用客户不可用 |
| `DeviceServiceTest` | 7 | 编号重复、创建归属、客户角色限制、数据域、归属变更限制、设备不存在、有工单不可删 |
| `TicketServiceTest` | 11 | 设备与客户不匹配、停用客户、截止时间、状态校验、分派对象校验、非负责人接单、并发冲突、取消限制、完成前需处理记录、重开窗口 |
| `TicketEvaluationServiceTest` | 5 | 未完成不可评价、重复评价、跨客户、满意度映射、匿名标记 |
| `TicketScheduledServiceTest` | 5 | 自动确认开关、超期确认与 SYSTEM 记录、并发跳过、逾期提醒 |
| `StatisticsServiceTest` | 5 | 趋势按天补齐、周/月聚合、非法日期区间、非法粒度、客户角色拒绝 |
| `StatisticsCacheServiceTest` | 4 | 缓存键范围隔离、命中反序列化、未命中写入 TTL、Redis 不可用降级 |

## 4. 后端集成测试分布

| 测试类 | 用例数 | 覆盖内容 |
| --- | --- | --- |
| `AuthIntegrationTest` | 13 | 登录成功/失败、锁定、禁用、参数校验、未登录 401、伪造令牌、客户与工程师权限边界、退出登录令牌失效、修改密码、刷新令牌轮换 |
| `CustomerDeviceIntegrationTest` | 11 | 客户与设备 CRUD、编号与序列号唯一、关联完整性、筛选、客户数据隔离、角色限制、参数校验、停用与删除保护、有工单设备不可删 |
| `TicketWorkflowIntegrationTest` | 13 | 完整主流程与历史留痕、非法跳转、并发接单、并发建单编号唯一、跨角色隔离、权限限制、非负责人接单、逾期查询、取消后操作、转派后重新接单、退回、管理员视图、退回重做 |
| `TicketFeedbackIntegrationTest` | 8 | 评价条件与唯一性、评价权限、匿名隐藏、内部备注可见性、附件上传下载与 SHA-256、类型与大小限制（配置驱动）、附件权限与 ID 不匹配、附件删除权限与物理文件清理 |
| `DashboardIntegrationTest` | 9 | 总览与数据库比对、工程师范围、客户拒绝、分布一致性、工作量一致性、满意度真实性、缓存命中与写后失效、趋势区间、超期自动确认 |
| `UserManagementIntegrationTest` | 8 | 新增用户与登录、账号重复、禁用后登录、不能禁用自己、角色变更即时生效、重置密码、操作审计、失败审计脱敏 |
| `HealthIntegrationTest` | 2 | 健康检查（含依赖探测）与未知路径统一 404 |

## 5. 前端测试分布

| 测试文件 | 用例数 | 覆盖内容 |
| --- | --- | --- |
| `utils/__tests__/request.spec.js` | 6 | 错误归一化：后端业务码、HTTP 状态、超时、网络异常、重复包装、未知异常 |
| `components/__tests__/StatusTag.spec.js` | 3 | 状态/优先级标签映射与未知值回退 |
| `views/__tests__/LoginView.spec.js` | 4 | 空值校验、密码长度校验、登录成功保存令牌并跳转、登录失败提示且不跳转 |
| `views/__tests__/DashboardView.spec.js` | 3 | 真实统计数据渲染、趋势粒度切换触发请求、接口失败展示错误而非假数据 |
| `views/ticket/__tests__/TicketListView.spec.js` | 4 | 列表渲染与逾期标记、关键词查询参数、查询失败清空、详情跳转 |
| `router/__tests__/menu.spec.js` | 4 | 管理员/客户/工程师菜单过滤、系统管理路由角色声明 |
| `views/__tests__/NotFoundView.spec.js` | 1 | 404 页面渲染与返回仪表盘 |

## 6. 端到端验收（打包产物 + 真实 HTTP）

在 `mvn package` 产出的 jar 上执行完整链路，全部符合预期：

1. 客服创建 URGENT 工单 → 编号 `SO20261015000003`，状态 `PENDING_ASSIGN`，截止时间为创建后 4 小时（优先级 SLA 生效）。
2. 未分派时工程师接单 → `409 / 1402`；客服分派后 → `PENDING_ACCEPT`。
3. 客户尝试分派 → `403 / 1101`（越权拦截）。
4. 工程师接单 → `PROCESSING`；填写处理记录 → `200`；提交处理结果 → `PENDING_CONFIRM`。
5. 员工内部备注 + 客户评论 → 客户视角仅 1 条（内部备注不可见），员工视角 2 条。
6. 附件上传（文件名保持清洗后的原名、存储名 UUID）→ `200`；下载 → `200` 且字节一致；工程师删除 → `403 / 1101`；客服删除 → `200`。
7. 客户确认 → `COMPLETED`；评价 → `200`（`VERY_SATISFIED`）；重复评价 → `409 / 1502`。
8. 处理历史：`CREATE → ASSIGN → ACCEPT → PROCESS → COMPLETE → CONFIRM → EVALUATE`。
9. 统计：总览 `total=11`、`逾期=1`、`平均满意度=5`；满意度分布 `5 星:1`；工作量 `一号 完成3/共4`、`二号 完成0/共3`；趋势 30 个点，当日新建 6 / 完成 2；工单模块审计日志 17 条。
10. 缓存：连续两次总览请求 `cached=false → true`，建单后再次请求 `cached=false` 且总数 +1。

## 7. 测试局限与说明

- **并发用例的可重复性**：并发接单与并发建单使用线程池 + 起跑闩，断言"恰好一次成功"，在本地 MySQL 上稳定复现；该用例正是发现"并发建单重试风暴与死锁"的用例。
- **Redis 依赖**：缓存命中/失效与令牌黑名单用例需要本地 Redis 可写；若 Redis 处于 MISCONF 状态（写被拒绝），相关用例会失败并给出明确日志，这属于环境问题而非代码问题。
- **未覆盖**：未做浏览器端 E2E（无 Playwright 等工具依赖），前端通过组件与路由测试 + 构建 + 真实接口冒烟验证；未做压力测试与长时间稳定性测试。

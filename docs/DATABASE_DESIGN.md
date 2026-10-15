# ServiceFlow 企业售后工单管理系统 — 数据库设计

> 文档版本：v1.0（随 Stage 2 交付）
> 结构脚本：[backend/src/main/resources/db/schema.sql](../backend/src/main/resources/db/schema.sql)
> 基础数据脚本：[backend/src/main/resources/db/data.sql](../backend/src/main/resources/db/data.sql)
> 关联文档：[TECHNICAL_DESIGN.md](./TECHNICAL_DESIGN.md)

## 1. 总体约定

| 项 | 约定 |
| --- | --- |
| 数据库 | MySQL 8.x；开发库 `serviceflow`，测试库 `serviceflow_test` |
| 引擎 / 字符集 | InnoDB / `utf8mb4` + `utf8mb4_general_ci` |
| 主键 | `id BIGINT UNSIGNED AUTO_INCREMENT` |
| 时间字段 | `create_time`、`update_time`（`DATETIME`，默认 `CURRENT_TIMESTAMP`，更新时自动刷新） |
| 审计字段 | `create_by`、`update_by`（写入操作用户 ID） |
| 逻辑删除 | 主数据表 `deleted BIGINT NOT NULL DEFAULT 0`，删除时写入**本行 id**，配合唯一索引 `(业务唯一列, deleted)` 实现"未删除时唯一、删除后可重建同名数据" |
| 乐观锁 | 需要并发保护的 `service_ticket`、`sys_user` 使用 `version INT`，MyBatis-Plus `OptimisticLockerInnerInterceptor` 生效 |
| 命名 | 表名小写下划线；系统表前缀 `sys_`；工单相关表前缀 `ticket_` |

### 1.1 脚本分离

| 脚本 | 内容 | 使用场景 |
| --- | --- | --- |
| `db/schema.sql` | 16 张表的结构、索引与约束（`CREATE TABLE IF NOT EXISTS`，可重复执行） | 所有环境 |
| `db/data.sql` | 角色、权限、角色权限关系、演示账号、故障分类、系统配置 | 本地开发与测试环境 |
| `db/demo-data.sql` | 业务演示数据：4 个客户、8 台设备，并绑定客户演示账号的 `customer_id` | 本地开发与测试环境（工单与评价演示数据随 Stage 4~5 补充） |

> 生产环境只执行 `schema.sql`；`data.sql` 中的演示账号仅用于本地开发，生产不得保留。

## 2. 表清单

| # | 表名 | 中文名 | 说明 |
| --- | --- | --- | --- |
| 1 | `sys_user` | 用户表 | 员工与客户登录账号，BCrypt 密码哈希 |
| 2 | `sys_role` | 角色表 | 四种内置角色 |
| 3 | `sys_user_role` | 用户角色关系 | 支持一人多角色 |
| 4 | `sys_permission` | 权限表 | 菜单/按钮/接口权限编码 |
| 5 | `sys_role_permission` | 角色权限关系 | 角色 → 权限 |
| 6 | `fault_category` | 故障分类表 | 支持父子级 |
| 7 | `sys_config` | 系统配置表 | 业务参数唯一来源 |
| 8 | `customer` | 客户表 | 客户资料 |
| 9 | `device` | 设备表 | 客户设备档案 |
| 10 | `service_ticket` | 工单表 | 核心业务表 |
| 11 | `ticket_assignment` | 工单分派记录 | 分派/转派/退回，含有效性唯一约束 |
| 12 | `ticket_process_record` | 工单处理记录 | 状态流转与处理历史 |
| 13 | `ticket_comment` | 工单评论 | 内部备注与客户沟通 |
| 14 | `ticket_evaluation` | 服务评价 | 一单一评（唯一约束） |
| 15 | `ticket_attachment` | 工单附件 | 文件元数据 |
| 16 | `sys_operation_log` | 操作日志 | 审计 |
| 17 | `ticket_sequence` | 工单当日序列 | 原子取号，保证工单编号唯一（见 3.3） |

## 3. 关键约束与索引

### 3.1 唯一约束

| 表 | 约束 | 作用 |
| --- | --- | --- |
| `sys_user` | `uk_user_username (username, deleted)` | 账号唯一，删除后可复用 |
| `sys_user` | `uk_user_customer (customer_id)` | 一个客户档案最多绑定一个登录账号 |
| `sys_role` | `uk_role_code (role_code, deleted)` | 角色编码唯一 |
| `sys_user_role` | `uk_user_role (user_id, role_id)` | 防止重复分配同一角色 |
| `sys_role_permission` | `uk_role_perm (role_id, perm_id)` | 防止重复授权 |
| `sys_permission` | `uk_perm_code (perm_code)` | 权限编码唯一 |
| `fault_category` | `uk_category_code (category_code, deleted)` | 分类编码唯一 |
| `sys_config` | `uk_config_key (config_key)` | 配置键唯一 |
| `customer` | `uk_customer_code (customer_code, deleted)` | 客户编号唯一 |
| `device` | `uk_device_code (device_code, deleted)`、`uk_device_sn (sn_no, deleted)` | 设备编号与序列号唯一 |
| `service_ticket` | `uk_ticket_no (ticket_no)` | 工单编号全局唯一 |
| `ticket_assignment` | `uk_assignment_active (ticket_id, active_flag)` | **同一工单同时只有一条有效分派记录**（历史记录写入自身 id，规避与 1 冲突） |
| `ticket_evaluation` | `uk_evaluation_ticket (ticket_id)` | 一单一评 |

### 3.2 主要查询索引

| 表 | 索引 | 支撑的查询 |
| --- | --- | --- |
| `sys_user` | `idx_user_status`、`idx_user_phone` | 用户列表筛选 |
| `service_ticket` | `idx_ticket_status_create (status, create_time)` | 列表按状态 + 时间排序 |
| `service_ticket` | `idx_ticket_assignee_status (assignee_id, status)` | 工程师"我的工单" |
| `service_ticket` | `idx_ticket_customer_status (customer_id, status)` | 客户"我的工单" |
| `service_ticket` | `idx_ticket_deadline_status (deadline_time, status)` | 逾期工单查询 |
| `service_ticket` | `idx_ticket_device`、`idx_ticket_priority` | 设备维度、优先级筛选 |
| `ticket_process_record` | `idx_record_ticket (ticket_id, create_time)` | 工单处理历史 |
| `ticket_attachment` | `idx_attachment_ticket (ticket_id, deleted)` | 附件列表 |
| `sys_operation_log` | `idx_log_create_time`、`idx_log_user (user_id, create_time)`、`idx_log_module` | 审计检索 |
| `ticket_evaluation` | `idx_evaluation_customer`、`idx_evaluation_create` | 满意度统计 |

> 统计类查询（趋势、分布、工作量）在 Stage 6 实现时会基于上述索引设计聚合 SQL，必要时补充冗余统计字段或关联索引，并记录执行计划。

### 3.3 工单编号的并发取号策略

工单编号格式为 `SO + yyyyMMdd + 6 位当日序列`。取号使用独立序列表 `ticket_sequence`：

```sql
INSERT INTO ticket_sequence (seq_date, current_value) VALUES (:today, 1)
ON DUPLICATE KEY UPDATE current_value = current_value + 1;
SELECT current_value FROM ticket_sequence WHERE seq_date = :today;
```

**为什么不用"查询当日最大值 + 唯一索引冲突重试"**：在 MySQL 默认的 REPEATABLE READ 隔离级别下，
同一事务内重复执行 `SELECT MAX(...)` 会一直读到事务开始时的快照，并发建单时多个事务会反复生成同一个编号，
造成重试风暴并最终触发死锁（该问题由并发集成测试真实复现：`mvn verify` 中 5 线程并发建单曾出现 DuplicateKey 4 次重试 + Deadlock）。
改为序列表后：`INSERT ... ON DUPLICATE KEY UPDATE` 在同一行上加排他锁并原子自增，并发建单按顺序取到不同序列号，
事务内 `SELECT` 必定读到本事务写入的值，无需重试。代价是建单事务在取号时对该行加锁并在提交后释放，
对本系统的建单频率而言完全可接受。`uk_ticket_no` 唯一索引仍保留作为最终兜底。

## 4. 状态与枚举取值

| 字段 | 取值 |
| --- | --- |
| `service_ticket.status` | `PENDING_ASSIGN` 待分派、`PENDING_ACCEPT` 待接单、`PROCESSING` 处理中、`PENDING_CONFIRM` 待确认、`COMPLETED` 已完成、`CANCELLED` 已取消 |
| `service_ticket.priority` | `LOW`、`MEDIUM`、`HIGH`、`URGENT` |
| `service_ticket.source` | `PHONE`、`WEB`、`WECHAT`、`EMAIL` |
| `ticket_process_record.action` | `CREATE`、`ASSIGN`、`ACCEPT`、`TRANSFER`、`REJECT`、`PROCESS`、`COMPLETE`、`CONFIRM`、`REOPEN`、`CANCEL`、`AUTO_CONFIRM` |
| `ticket_assignment.assign_type` | `ASSIGN`、`TRANSFER`、`REJECT_TO_POOL` |
| `ticket_attachment.biz_type` | `FAULT_PHOTO`、`REPAIR_PHOTO`、`REPORT`、`OTHER` |
| `sys_user.status` / `device.status` / `customer.status` | 见 `schema.sql` 列注释 |

状态机转换规则见 [TECHNICAL_DESIGN.md 第 5 章](./TECHNICAL_DESIGN.md)。

## 5. 事务边界与并发控制

| 业务操作 | 事务范围 | 并发保护 |
| --- | --- | --- |
| 登录成功 | 独立事务（`REQUIRES_NEW`）更新失败计数、锁定状态与登录信息 | `uk_user_username`；锁定状态入库 |
| 登录失败计数 | **独立事务（`REQUIRES_NEW`）** | 若与登录同事务，业务异常回滚会导致计数永远不生效（已通过集成测试发现并修正） |
| 创建工单 | 插入工单 + 写创建处理记录 | 编号唯一索引兜底重试 |
| 分派/转派 | 更新工单 + 关闭旧分派记录 + 新增分派记录 + 处理记录 | 条件更新 `WHERE id=? AND status=? AND version=?`；`uk_assignment_active` 兜底 |
| 接单 | 更新工单状态与接单人 + 处理记录 | Redis 锁 + 条件更新；Redis 不可用时仅依赖条件更新 |
| 评价 | 校验状态与归属 + 插入评价 + 处理记录 | `uk_evaluation_ticket` 兜底，重复评价返回 1502 |
| 操作审计 | **独立事务（`REQUIRES_NEW`）** | 业务回滚时审计记录仍保留 |

**原则**：事务只包裹数据库操作，不包裹文件 IO 与网络调用；写缓存/删缓存放在事务提交之后。

## 6. 基础数据（data.sql）

| 数据 | 数量 | 说明 |
| --- | --- | --- |
| 角色 | 4 | `ROLE_ADMIN`、`ROLE_SERVICE`、`ROLE_ENGINEER`、`ROLE_CUSTOMER` |
| 权限 | 30 | 覆盖用户/角色/配置/日志/客户/设备/工单/评价/附件/统计 |
| 角色权限关系 | 64 | 按最小权限原则分配（客户角色额外拥有 `customer:view`，配合数据域只能看到自己的档案） |
| 演示账号 | 6 | 见下表 |
| 故障分类 | 8 | 硬件、软件、网络、电池、屏幕、传感器、固件、其他 |
| 系统配置 | 8 | 工单确认时限、重开窗口、SLA、附件限制、登录锁定策略 |

### 6.1 演示账号（仅本地开发）

统一密码：`Serviceflow@2026`（库中以 BCrypt 哈希存储）。

| 账号 | 姓名 | 角色 | 主要能力 |
| --- | --- | --- | --- |
| `admin` | 系统管理员 | `ROLE_ADMIN` | 全部权限，含用户/角色/配置/日志 |
| `service01` | 客服一号 | `ROLE_SERVICE` | 客户、设备、工单创建与分派 |
| `engineer01` | 维修工程师一号 | `ROLE_ENGINEER` | 接单、处理、提交结果（仅本人负责工单） |
| `engineer02` | 维修工程师二号 | `ROLE_ENGINEER` | 同上 |
| `customer01` | 客户联系人一 | `ROLE_CUSTOMER` | 查看本人工单、确认与评价 |
| `customer02` | 客户联系人二 | `ROLE_CUSTOMER` | 同上（用于验证数据隔离） |

> `customer01` / `customer02` 的 `customer_id` 已由 `db/demo-data.sql` 绑定到客户 1 与客户 2，用于验证数据隔离。

### 6.2 业务演示数据（demo-data.sql）

| 数据 | 数量 | 说明 |
| --- | --- | --- |
| 客户 | 4 | `CUS-0001`（A 级，重点客户）、`CUS-0002`（B 级）、`CUS-0003`（A 级，多仓库）、`CUS-0004`（C 级，新客户） |
| 设备 | 8 | 覆盖智能终端、检测设备、网络设备、手持终端、监测设备，含 1 台报废设备与 1 台保修即将到期设备 |
| 账号绑定 | 2 | `customer01` → 客户 1，`customer02` → 客户 2 |
| 工单 | 8 | 编号使用固定前缀 `SO202601010000xx`，覆盖待分派 2、待接单 2、处理中 1、待确认 1、已完成 1、已取消 1，其中 1 张为逾期工单 |
| 工单处理记录 | 12 | 演示创建、分派、接单、处理、提交、确认与退回重做的完整留痕 |
| 工单分派记录 | 2 | 当前有效的分派关系（`active_flag = 1`） |

`demo-data.sql` 可重复执行（`INSERT ... ON DUPLICATE KEY UPDATE` + 绑定 `UPDATE`），实测重复执行后数据量保持不变。

## 7. 初始化步骤

```powershell
# 1. 创建开发库与测试库
mysql -u root -p -e "CREATE DATABASE IF NOT EXISTS serviceflow DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci; CREATE DATABASE IF NOT EXISTS serviceflow_test DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;"

# 2. 执行结构脚本（两个库都执行）
mysql -u root -p serviceflow < backend/src/main/resources/db/schema.sql
mysql -u root -p serviceflow_test < backend/src/main/resources/db/schema.sql

# 3. 执行基础数据脚本（两个库都执行）
mysql -u root -p serviceflow < backend/src/main/resources/db/data.sql
mysql -u root -p serviceflow_test < backend/src/main/resources/db/data.sql

# 4. 执行业务演示数据脚本（仅本地开发/测试环境）
mysql -u root -p serviceflow < backend/src/main/resources/db/demo-data.sql
mysql -u root -p serviceflow_test < backend/src/main/resources/db/demo-data.sql
```

三个脚本均可重复执行：结构脚本使用 `CREATE TABLE IF NOT EXISTS`，数据脚本使用 `INSERT ... ON DUPLICATE KEY UPDATE`（不会重复插入，也不会删除业务数据）。

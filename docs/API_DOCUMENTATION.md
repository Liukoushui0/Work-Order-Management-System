# ServiceFlow API 文档说明

> 说明：本文档是接口清单与约定的说明，**可交互的完整文档由 Springdoc OpenAPI 自动生成**：
> Swagger UI `http://localhost:8080/swagger-ui.html`，JSON `http://localhost:8080/v3/api-docs`。
> 所有接口统一前缀 `/api/v1`，除标注"匿名"的接口外都需要 `Authorization: Bearer <accessToken>`。

## 1. 通用约定

### 1.1 统一响应结构

```json
{
  "code": 0,
  "message": "success",
  "data": {},
  "traceId": "3f1a...",
  "timestamp": "2026-10-15 20:30:00"
}
```

- `code = 0` 表示成功，非 0 为业务错误码；HTTP 状态码同时保持语义正确（400/401/403/404/409/500）。
- `traceId` 同时回传响应头 `X-Trace-Id`，与服务端日志一致，便于问题定位。
- 错误响应结构相同，`data` 为空；调试时可先看 `code`，再看 `traceId` 对应的服务端日志。

### 1.2 分页与排序

- 请求参数：`pageNum`（默认 1）、`pageSize`（默认 10，最大 100）、`sortField`、`sortOrder`（asc/desc）。
- 响应结构：`{ "records": [], "total": 0, "pageNum": 1, "pageSize": 10, "pages": 0 }`。
- `sortField` 受白名单限制，非白名单取值返回 `1202`，防止通过排序参数注入 SQL。

### 1.3 主要错误码

| 区间 | 含义 | 常用取值 |
| --- | --- | --- |
| 1000~1099 | 认证 | 1001 用户名或密码错误、1002 账号禁用、1003 账号锁定、1004 令牌无效 |
| 1100~1199 | 鉴权 | 1101 无权访问该资源、1102 数据域外访问 |
| 1200~1299 | 参数校验 | 1201 参数校验失败、1202 非法排序字段、1203 请求体格式错误 |
| 1300~1399 | 客户与设备 | 1301 客户不存在、1302 设备不存在、1303 设备不属于该客户 |
| 1400~1499 | 工单 | 1401 工单不存在、1402 状态不允许、1403 已被接单、1404 已取消、1405 无权限操作该工单 |
| 1500~1599 | 评价与附件 | 1501 未完成不可评价、1502 已评价、1503 类型不允许、1504 文件过大、1505 附件不存在 |
| 1600~1699 | 系统 | 1601 系统内部错误、1602 并发冲突、1603 依赖不可用、1604 资源不存在 |

## 2. 认证与个人中心（/auth）

| 方法 | 路径 | 权限 | 说明 |
| --- | --- | --- | --- |
| POST | `/auth/login` | 匿名 | 登录，返回 access/refresh token 与用户信息（角色、权限） |
| POST | `/auth/logout` | 登录 | 退出登录，将当前令牌 `jti` 写入 Redis 黑名单 |
| POST | `/auth/refresh` | 匿名（持有 refresh token） | 刷新令牌，旧 refresh token 立即失效 |
| GET | `/auth/me` | 登录 | 当前用户信息（含角色与权限集合） |
| PUT | `/auth/password` | 登录 | 修改密码（校验原密码），成功后当前令牌失效 |
| PUT | `/auth/profile` | 登录 | 更新个人资料（姓名、手机号、邮箱、头像） |

## 3. 用户、角色与系统（/users、/roles、/permissions、/fault-categories、/configs、/operation-logs）

| 方法 | 路径 | 权限 | 说明 |
| --- | --- | --- | --- |
| GET | `/users` | `user:view` | 用户分页列表（关键词/状态/角色筛选） |
| GET | `/users/{id}` | `user:view` | 用户详情（含角色） |
| GET | `/users/engineers` | `ticket:assign` 或 `ticket:transfer` | 可指派工程师下拉（仅返回必要字段） |
| POST | `/users` | `user:manage` | 新增用户（初始密码需满足复杂度） |
| PUT | `/users/{id}` | `user:manage` | 修改用户资料 |
| PUT | `/users/{id}/status` | `user:manage` | 启用/禁用（不能禁用自己或最后一个管理员） |
| PUT | `/users/{id}/roles` | `user:manage` | 分配角色（不能移除最后一个管理员的管理员角色） |
| POST | `/users/{id}/reset-password` | `user:manage` | 重置密码并解除登录锁定 |
| DELETE | `/users/{id}` | `user:manage` | 逻辑删除用户 |
| GET | `/roles` | `role:view` | 角色列表（系统内置，不可增删） |
| GET | `/permissions` | `role:view` | 权限清单（30 项） |
| GET/POST | `/fault-categories` | `fault-category:view` / `manage` | 故障分类分页与新增 |
| GET | `/fault-categories/enabled` | `fault-category:view` | 启用的分类（工单下拉） |
| PUT/DELETE | `/fault-categories/{id}` | `fault-category:manage` | 修改/删除分类（有子分类时拒绝） |
| GET | `/configs` | `config:view` | 系统配置列表 |
| PUT | `/configs/{configKey}` | `config:manage` | 修改配置（如确认时限、SLA、附件限制） |
| GET | `/operation-logs` | `log:view` | 操作日志（模块/操作人/结果/日期筛选） |

## 4. 客户与设备（/customers、/devices）

| 方法 | 路径 | 权限 | 说明 |
| --- | --- | --- | --- |
| GET | `/customers` | `customer:view` | 客户分页列表（关键词/等级/状态），客户角色只能看到自身档案 |
| GET | `/customers/options` | `customer:view`/`device:view`/`ticket:view` | 客户下拉 |
| GET | `/customers/{id}` | `customer:view` | 客户详情（含设备数、工单数） |
| POST | `/customers` | `customer:manage` | 新增客户（编号唯一、格式校验） |
| PUT | `/customers/{id}` | `customer:manage` | 修改客户（编号不可改） |
| PUT | `/customers/{id}/status` | `customer:manage` | 启用/停用（存在未完成工单时拒绝停用） |
| DELETE | `/customers/{id}` | `customer:manage` | 删除（名下存在设备或未完成工单时拒绝） |
| GET | `/devices` | `device:view` | 设备分页列表（关键词/客户/状态），客户角色仅见自身设备 |
| GET | `/devices/{id}` | `device:view` | 设备详情（含所属客户、工单数、是否在保） |
| GET | `/devices/options` | `device:view` | 指定客户的可用设备（工单下拉） |
| POST | `/devices` | `device:manage` | 新建设备（编号与序列号唯一、客户需存在且启用） |
| PUT | `/devices/{id}` | `device:manage` | 修改设备（客户角色不能变更归属） |
| DELETE | `/devices/{id}` | `device:manage` | 删除（存在工单记录时拒绝） |

## 5. 售后工单（/tickets）

| 方法 | 路径 | 权限 | 说明 |
| --- | --- | --- | --- |
| GET | `/tickets` | `ticket:view` | 工单分页列表（状态/优先级/客户/负责人/故障分类/时间范围/逾期/关键词），按角色收敛数据域 |
| GET | `/tickets/overdue` | `ticket:view` | 逾期工单（未结束且超过截止时间，按截止时间升序） |
| GET | `/tickets/{id}` | `ticket:view` | 工单详情（客户联系方式、设备信息、处理历史、评价、附件与评论数） |
| GET | `/tickets/{id}/records` | `ticket:view` | 处理历史 |
| POST | `/tickets` | `ticket:create` | 创建工单（生成唯一编号、按优先级计算 SLA） |
| POST | `/tickets/{id}/assign` | `ticket:assign` | 分派（待分派 → 待接单） |
| POST | `/tickets/{id}/accept` | `ticket:accept` | 接单（待接单 → 处理中，Redis 锁 + 条件更新） |
| POST | `/tickets/{id}/transfer` | `ticket:transfer` | 转派（待接单/处理中 → 待接单） |
| POST | `/tickets/{id}/reject` | `ticket:transfer` | 退回（待接单 → 待分派，仅当前负责人） |
| POST | `/tickets/{id}/process` | `ticket:process` | 填写处理记录（处理中，仅当前负责人） |
| POST | `/tickets/{id}/complete` | `ticket:complete` | 提交处理结果（需先有处理记录，处理中 → 待确认） |
| POST | `/tickets/{id}/confirm` | `ticket:confirm` | 客户确认（待确认 → 已完成，仅所属客户或管理员） |
| POST | `/tickets/{id}/reopen` | `ticket:reopen` | 退回重做（待确认 → 处理中）或重新打开（已完成 → 待分派，受重开窗口限制） |
| POST | `/tickets/{id}/cancel` | `ticket:cancel` | 取消（非终态 → 已取消） |
| GET/POST | `/tickets/{id}/comments` | `ticket:view` | 评论列表与新增（内部备注仅员工可见，客户评论强制公开） |
| POST/GET | `/tickets/{id}/evaluation` | `ticket:evaluate` / `ticket:view` | 提交评价与查询评价（一单一评、匿名对非评价人隐藏内容） |
| GET/POST | `/tickets/{id}/attachments` | `attachment:download` / `upload` | 附件列表与上传（multipart） |
| GET | `/tickets/{id}/attachments/{attachmentId}/download` | `attachment:download` | 下载（先校验工单访问权限，再校验路径安全） |
| DELETE | `/tickets/{id}/attachments/{attachmentId}` | `attachment:delete` | 删除附件（逻辑删除 + 清理物理文件） |

## 6. 运营统计（/dashboard）

| 方法 | 路径 | 权限 | 说明 |
| --- | --- | --- | --- |
| GET | `/dashboard/overview` | `dashboard:view` | 总览：总数、状态分布、进行中、逾期、已完成、平均处理时长、平均满意度；`cached` 标识是否命中缓存 |
| GET | `/dashboard/trend` | `dashboard:view` | 工单趋势，`granularity=DAY/WEEK/MONTH`，`startDate`/`endDate` 可选（默认近 30 天） |
| GET | `/dashboard/fault-distribution` | `dashboard:view` | 故障类型分布（名称、数量、占比） |
| GET | `/dashboard/priority-distribution` | `dashboard:view` | 优先级分布 |
| GET | `/dashboard/engineer-workload` | `dashboard:view` | 工程师工作量（负责数、完成数、进行中、平均处理时长） |
| GET | `/dashboard/satisfaction` | `dashboard:view` | 满意度（评价总数、平均分、星级分布） |

> 统计的数据域：管理员与客服为全局（`scope=ALL`），工程师仅本人（`scope=SELF`），客户无权限（403/1101）。

## 7. 系统健康检查

| 方法 | 路径 | 权限 | 说明 |
| --- | --- | --- | --- |
| GET | `/health` | 匿名 | 返回应用状态，并实际探测 MySQL 与 Redis 连通性 |

## 8. 调用示例

```powershell
# 1) 登录获取令牌
$login = Invoke-RestMethod -Method Post -Uri "http://localhost:8080/api/v1/auth/login" `
  -ContentType "application/json" -Body '{"username":"admin","password":"Serviceflow@2026"}'
$headers = @{ Authorization = "Bearer " + $login.data.accessToken }

# 2) 查询工单列表（分页 + 状态筛选）
Invoke-RestMethod -Uri "http://localhost:8080/api/v1/tickets?status=PENDING_ASSIGN&pageSize=5" -Headers $headers

# 3) 创建工单
Invoke-RestMethod -Method Post -Uri "http://localhost:8080/api/v1/tickets" -Headers $headers `
  -ContentType "application/json" `
  -Body '{"title":"设备无法启动","description":"上电无反应","customerId":1,"deviceId":1,"faultCategoryId":1,"priority":"HIGH"}'
```

> 附件上传使用 `multipart/form-data`，字段名为 `file`，可选 `bizType`（FAULT_PHOTO/REPAIR_PHOTO/REPORT/OTHER）。

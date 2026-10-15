# ServiceFlow 本地部署与故障排查

> 适用范围：不使用 Docker 的本地 Windows 开发环境。
> 安装配置的详细步骤见 [README.md](../README.md) 第 1~8 节，本文侧重**部署顺序、验证清单与故障排查**。

## 1. 部署顺序（推荐）

1. 安装 JDK 17 → 验证 `java -version` 为 17.x。
2. 安装 Maven 3.10.0 → 设置 `JAVA_HOME` 后验证 `mvn -v` 显示的 Java 为 17.x。
3. 安装 Node.js 20+ 与 npm → 验证 `node -v`、`npm -v`。
4. 安装并启动 MySQL 8 → 创建 `serviceflow`、`serviceflow_test` 两个库。
5. 依次执行 `db/schema.sql` → `db/data.sql` → `db/demo-data.sql`（开发库与测试库都要执行）。
6. 安装并启动 Redis → 验证 `redis-cli ping` 返回 `PONG`，并确认 `rdb_last_bgsave_status:ok`。
7. 设置后端环境变量（至少 `DB_PASSWORD`）→ 启动后端 → 访问健康检查。
8. 安装前端依赖 → 启动前端 → 浏览器登录。

## 2. 部署后验证清单

| 检查项 | 命令 / 操作 | 期望结果 |
| --- | --- | --- |
| 后端健康 | `GET /api/v1/health` | `code=0`，`database=UP`，`redis=UP` |
| 数据库表 | `SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='serviceflow'` | 17 |
| 基础数据 | `SELECT COUNT(*) FROM serviceflow.sys_user` / `sys_permission` / `sys_role_permission` | 6 / 30 / 65 |
| 演示业务数据 | `SELECT COUNT(*) FROM serviceflow.service_ticket` | 8（含 1 张逾期） |
| 接口文档 | 打开 `/swagger-ui.html` | 可浏览全部接口分组 |
| 前端页面 | 打开 `http://localhost:5173/` | 显示登录页，可用演示账号登录 |
| 登录链路 | 用 `admin` 登录 | 进入仪表盘，指标卡与图表有真实数据 |
| 权限隔离 | 用 `customer01` 登录后访问 `/users` | 返回 403/1101 |
| 附件存储目录 | 上传一个附件后检查 `backend/storage/attachments` | 出现 `ticket-{id}/{yyyy-MM}/{uuid}.png` |
| 缓存 | 连续两次 `GET /api/v1/dashboard/overview` | 第二次 `cached=true`；建单后再次请求 `cached=false` |

## 3. 故障排查

### 3.1 后端无法启动

| 现象 | 原因 | 处理 |
| --- | --- | --- |
| `invalid target release: 17` / 类版本错误 | Maven 使用 JDK 8 | 设置 `$env:JAVA_HOME="D:\Tools\envs\jdk17"` 后重试 |
| `Failed to configure a DataSource` | 未设置 `DB_PASSWORD` 或数据库未创建 | 设置环境变量并创建库 |
| `Communications link failure` | MySQL 未启动或端口不对 | 检查 `MySQL80` 服务与 `DB_PORT` |
| 启动后健康检查 `database=DOWN` | 库不存在 / 账号无权限 | 用 `mysql -u root -p -e "SHOW DATABASES"` 核对 |
| 健康检查 `redis=DOWN` | Redis 未启动 | 启动 Redis 服务并 `redis-cli ping` |
| 日志出现 `app.jwt.secret 未配置` 告警 | 未设置 `JWT_SECRET` | 本地开发可忽略（重启后旧令牌失效）；生产必须配置 |
| 端口 8080 被占用 | 已有实例在运行 | `netstat -ano | findstr :8080` 找到进程后停止，或修改 `server.port` |

### 3.2 Redis 相关

| 现象 | 原因 | 处理 |
| --- | --- | --- |
| `MISCONF Redis is configured to save RDB snapshots...` | 快照写入失败，写命令被全部拒绝（`PING` 仍正常） | 以安装目录为工作目录并用 `--dir` 指向可写目录重启；`redis-cli bgsave` 确认 `rdb_last_bgsave_status:ok` |
| 退出登录后旧令牌仍可用 | 上一条导致黑名单写入失败 | 修复 Redis 写入后重新验证 |
| 统计接口每次都 `cached=false` | Redis 不可用或写入失败 | 查看日志中的缓存 WARN，确认 Redis 状态 |

### 3.3 前端相关

| 现象 | 原因 | 处理 |
| --- | --- | --- |
| `npm install` 报 `EPERM ... node_cache` | 全局缓存目录对当前用户不可写 | `npm install --cache ./.npm-cache`，或管理员调整目录权限 |
| 页面请求 404 | 后端未启动或代理未生效 | 确认后端 8080 已启动；检查 `vite.config.js` 的 proxy 配置 |
| 请求 401 后被踢回登录页 | 令牌过期或已退出登录 | 重新登录（属预期行为） |
| 构建提示 chunk 体积告警 | vendor 分包体积较大 | 已通过 `manualChunks` 分离 ECharts 与 Element Plus；如需更小可改按需引入 |
| 演示账号按钮看不到 | 生产构建下默认隐藏 | 需要时设置 `VITE_ENABLE_DEMO_ACCOUNTS=true` 后重新构建 |

### 3.4 数据库脚本相关

| 现象 | 原因 | 处理 |
| --- | --- | --- |
| 执行脚本报 `Column count doesn't match value count` | SQL 语句列数与值不一致 | 按报错行号核对（本项目开发过程中真实遇到过该问题并已修复） |
| 重复执行脚本报主键冲突 | 使用了 `INSERT` 而非幂等写法 | 三个脚本均已改为幂等写法，直接重复执行即可 |
| 集成测试污染开发数据 | 未使用测试库 | 集成测试默认连接 `serviceflow_test`；执行 `mvn verify` 前请确保该库已初始化 |

### 3.5 Git 与远程同步

| 现象 | 原因 | 处理 |
| --- | --- | --- |
| `Failed to connect to github.com:443` | 直连不可用 | 启动本地代理后：`git -c http.proxy=http://127.0.0.1:7897 push github main` |
| `schannel: SEC_E_NO_CREDENTIALS` | 网络受限环境下的 TLS 失败 | 同上，或改用代理 |
| 推送要求输入凭据 | 未配置凭据助手 | 使用 HTTPS + Personal Access Token，由系统凭据管理器保存，不要写入仓库 |

## 4. 停止与清理

```powershell
# 停止后端 / 前端：在各自终端按 Ctrl+C

# 停止 Redis 服务
.\redis-server.exe --service-stop

# 清空演示数据（仅本地）：删除开发库后重新执行三个脚本
mysql -u root -p -e "DROP DATABASE serviceflow; DROP DATABASE serviceflow_test;"
```

> 附件文件位于 `backend/storage/attachments`，已在 `.gitignore` 中排除；如需清理，直接删除该目录即可（数据库中的附件元数据会保留，可在页面上删除记录后再清理）。

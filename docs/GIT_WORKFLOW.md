# ServiceFlow 企业售后工单管理系统 — Git 工作流规范

> 文档版本：v1.0
> 阶段：Stage 0
> 关联文档：[DEVELOPMENT_PLAN.md](./DEVELOPMENT_PLAN.md)、[TECHNICAL_DESIGN.md](./TECHNICAL_DESIGN.md)、[STAGE0_ENVIRONMENT_CHECK.md](./STAGE0_ENVIRONMENT_CHECK.md)

## 1. 仓库现状（Stage 0 只读检查结论）

| 项 | 现状 |
| --- | --- |
| 本地仓库路径 | `E:\Work Order Management System` |
| 是否已初始化 | 已初始化（存在 `.git`），**不得重复执行 `git init`** |
| 当前分支 | `main`（尚无任何提交，`HEAD` 未指向提交） |
| 远程 | `github` → `https://github.com/Liukoushui0/Work-Order-Management-System.git`；`gitee` → `https://gitee.com/liukoushui0/Work-Order-Management-System.git`（已存在，**不得重复添加或删除**） |
| 远程提交情况 | `git ls-remote` 实测：两个远程均无任何分支/提交（空仓库） |
| 本地提交人 | `user.name = Liukoushui0`，`user.email = 2964587219@qq.com`（仓库级与全局均已配置） |
| 凭据助手 | 未配置，无 `~/.git-credentials` |
| 网络 | `gitee.com:443` 直连可用；`github.com:443` 直连不可用，需经本机代理（`127.0.0.1:7897`） |

### 1.1 与远程不一致时的处理

由于两个远程仓库当前均为空，首次推送不存在历史冲突。后续若出现远程有新提交：

1. `git fetch github` / `git fetch gitee` 获取远程状态。
2. 用 `git log --oneline --graph --all` 比对本地与远程历史。
3. 若远程包含本地缺失的提交，先分析来源（可能是其他设备或 GitHub/Gitee 网页端初始化的 README），**不得强制覆盖**。
4. 制定安全整合方案（`git pull --rebase` 或人工合并）后再推送；方案与结论写入阶段报告。

---

## 2. 分支策略

采用适合个人开发的轻量分支模型：

| 分支 | 用途 | 生命周期 |
| --- | --- | --- |
| `main` | 稳定、可运行的主分支，只接收通过验收的整合结果 | 长期 |
| `feature/<功能名>` | 单项功能开发 | 阶段完成后合并并删除 |
| `fix/<问题名>` | 缺陷修复 | 修复验证后合并并删除 |
| `docs/<文档名>` | 独立文档修改 | 合并后删除 |

**命名规范**：小写英文 + 连字符，例如 `feature/ticket-state-machine`、`fix/duplicate-ticket-acceptance`、`docs/technical-design`。

### 2.1 当前分支结构确认

仓库当前只有 `main` 分支且无提交，属于合理初始状态（无历史分支持续沿用需求）。Stage 0 的文档提交之后，各阶段按上表新建分支。

### 2.2 使用规则

1. **禁止直接在 `main` 上进行大量未验证的开发。**
2. 每个阶段在独立 `feature/*` 分支开发，验收通过后合并到 `main`。
3. 例外：纯文档的极小修改（如补充一行说明）可直接在 `main` 提交，但必须在提交说明或阶段报告中说明原因。
4. 阶段内允许多次提交，每次提交对应一个完整、可验证的变更点；不得为凑提交次数制造无意义提交。
5. 合并使用普通合并（`git merge --no-ff`）保留阶段边界，不使用强制推送。

### 2.3 推荐执行序列（每阶段）

```bash
git status                      # 确认工作区干净、处于预期分支
git switch -c feature/<name>    # 从最新的 main 创建功能分支
# ... 开发、测试 ...
git switch main
git merge --no-ff feature/<name> -m "merge: <阶段名称>"
git push github main
git push gitee main
```

---

## 3. 提交规范（Conventional Commits）

### 3.1 格式

```
<type>(<scope>): <description>
```

- 使用英文小写描述，动词开头，说明"做了什么"，不加句号。
- **禁止模糊描述**：如 `update`、`修改一下`、`完成项目`、`fix bug`。
- 一个提交只包含一类逻辑变更。

### 3.2 允许的 type

| type | 含义 |
| --- | --- |
| `feat` | 新增功能 |
| `fix` | 修复缺陷 |
| `refactor` | 重构代码（不改变外部行为） |
| `perf` | 性能优化 |
| `test` | 增加或修正测试 |
| `docs` | 文档修改 |
| `style` | 格式调整，不改变业务逻辑 |
| `build` | 构建与依赖配置 |
| `ci` | 持续集成配置 |
| `chore` | 其他维护工作 |

### 3.3 scope 约定

`project`、`backend`、`frontend`、`web`、`auth`、`rbac`、`security`、`db`、`customer`、`device`、`ticket`、`evaluation`、`attachment`、`dashboard`、`cache`、`common`、`docs` 等。

### 3.4 示例

```
chore(project): initialize project structure
docs(architecture): add technical design
feat(auth): implement user login
feat(ticket): implement work order creation
feat(ticket): add assignment workflow
fix(security): prevent unauthorized ticket access
test(ticket): add ticket state transition tests
feat(dashboard): implement ticket statistics
docs(readme): document local deployment
```

### 3.5 提交前的固定检查清单

1. 明确本阶段目标与验收标准。
2. 检查当前分支与工作区状态（`git status`）。
3. 实现功能并运行相关测试。
4. 检查编译、类型、格式与静态检查结果。
5. `git diff --check`（排除空白/冲突标记问题）。
6. 审查 `git diff` 与 `git status`。
7. 确认无敏感文件、构建产物、临时文件、无关修改。
8. 更新必要文档。
9. `git add` 本阶段相关文件。
10. 再次检查暂存区差异（`git diff --cached`）。
11. 创建规范提交。

> 执行 `git add -A` 前必须确认 `.gitignore` 已生效；优先显式列出文件路径。

### 3.6 提交信息模板（可选）

```
<type>(<scope>): <description>

阶段：Stage N
变更：<要点>
测试：<实际执行的命令与结果>
```

---

## 4. 阶段划分与提交对应关系

| 阶段 | 分支示例 | 主要提交 |
| --- | --- | --- |
| Stage 0 | `docs/technical-design` | `docs(project): add development plan, technical design and git workflow` |
| Stage 1 | `feature/project-skeleton` | `build(backend): add spring boot project skeleton`、`build(frontend): add vite vue3 typescript skeleton`、`feat(common): add unified response and exception handling`、`chore(project): add gitignore and env examples` |
| Stage 2 | `feature/auth-rbac` | `feat(db): add schema and demo data scripts`、`feat(auth): implement jwt authentication`、`feat(rbac): implement role and permission control`、`test(auth): add authentication and authorization tests` |
| Stage 3 | `feature/customer-device` | `feat(customer): implement customer management`、`feat(device): implement device archive management`、`test(customer): add customer and device isolation tests` |
| Stage 4 | `feature/ticket-core` | `feat(ticket): implement work order creation and query`、`feat(ticket): implement assignment and acceptance workflow`、`feat(ticket): add process history and state machine`、`test(ticket): add state transition and concurrency tests` |
| Stage 5 | `feature/evaluation-attachment` | `feat(evaluation): implement customer service evaluation`、`feat(attachment): implement ticket attachment management`、`test(attachment): add attachment access control tests` |
| Stage 6 | `feature/dashboard-cache` | `feat(dashboard): implement ticket statistics`、`perf(cache): add redis cache with invalidation`、`test(cache): add cache invalidation and degradation tests` |
| Stage 7 | `feature/web-pages` | `feat(web): implement ticket management pages`、`feat(web): implement dashboard charts`、`feat(web): add user role and log pages`、`style(web): unify layout and form feedback`、`test(web): add component and router guard tests` |
| Stage 8 | `fix/<problem>` | 按实际发现问题命名，例如 `fix(security): prevent unauthorized ticket access` |
| Stage 9 | `docs/final-delivery` | `docs(readme): document local deployment`、`docs(database): add database design document`、`docs(api): document rest api`、`docs(test): add test report` |

阶段内的提交顺序必须体现"实现 → 测试 → 文档"的真实推进过程。

---

## 5. 双远程同步策略

### 5.1 远程别名

- `github` → GitHub 仓库
- `gitee` → Gitee 仓库

本地已配置这两个别名，**不得重复添加、不得删除、不得重命名**。

### 5.2 同步流程（每个已验收阶段）

1. 确认远程地址、目标分支与本地提交历史：

```bash
git remote -v
git log --oneline -10
git status
```

2. 推送前检查远程是否存在新提交（避免覆盖他人工作）：

```bash
git fetch github
git fetch gitee
git log --oneline main..github/main    # 正常情况下应无输出
git log --oneline main..gitee/main     # 正常情况下应无输出
```

3. 在远程没有冲突的前提下同步：

```bash
git push github main
git push gitee main
```

4. 分别确认两次推送的真实结果（输出中应出现分支更新信息，而不是仅凭命令退出码）。

### 5.3 一致性要求

- 两个远程仓库的 `main` 必须保持一致的历史。
- **两次推送都必须确认成功**；只有实际确认推送成功，才可以说明"已同步到相应远程仓库"。
- 一次推送失败时，不得声称双远程同步完成。

### 5.4 本机网络特例（已实测）

- `gitee.com:443` 可直连，`git push gitee main` 预期可正常执行。
- `github.com:443` 直连不可用（TCP 不可达）；访问需经本机代理 `127.0.0.1:7897`。可选处理方式（需用户确认后再配置，避免擅自修改环境）：

  - 单次命令临时指定：`git -c http.proxy=http://127.0.0.1:7897 push github main`
  - 或为该远程配置专属代理（仅影响该远程）：`git config --local remote.github.proxy http://127.0.0.1:7897`

  该配置属于仓库本地配置，不涉及权限或凭据变更；使用前会向用户说明。

### 5.5 认证说明

- 本机未配置 Git 凭据助手，也不存在 `.git-credentials`。
- HTTPS 推送 GitHub/Gitee 需要有效的访问令牌（GitHub 使用 Personal Access Token，Gitee 使用私人令牌）。凭据由用户提供或由用户在系统中配置，**不得写入仓库、不得出现在命令输出、提交信息或日志中**。
- 若认证方式当前不可用，将向用户说明具体问题，不反复尝试、不绕过安全验证。

---

## 6. 失败处理方式

| 场景 | 处理方式 |
| --- | --- |
| 测试失败 | 修复后重新测试；**不得把失败结果描述为通过**；不得将未验证功能合并为已完成阶段 |
| 环境限制导致无法运行测试 | 记录限制原因，执行可完成的替代验证，并在阶段报告中明确告知 |
| 需要保存未完成工作 | 使用清晰的 WIP 提交说明（如 `chore(ticket): wip assignment workflow`），**不得标记为阶段完成** |
| 推送失败（认证） | 保留本地已完成提交，记录待推送状态，向用户说明认证问题 |
| 推送失败（网络） | 保留本地提交，检查代理/网络后重试；未成功前不声称已同步 |
| 一个远程成功、另一个失败 | 如实报告各自结果，排查失败原因并重试失败的远程 |
| 远程有新提交 | 先 `fetch`、比对差异，制定安全整合方案；**禁止强制推送** |
| 合并冲突 | 在功能分支内解决冲突并重新测试，不破坏 `main` 的可用状态 |

### 6.1 明确禁止的操作

- `git init`（仓库已存在）
- 重复添加 `github` / `gitee` 远程
- `git remote remove`、`git reset --hard`、`git clean -fd`
- `git push --force` / `git push -f`
- 覆盖远程仓库已有的 README、LICENSE 或提交
- 在输出、提交或推送中携带真实密码、令牌、私钥等敏感信息
- 为同步而擅自修改仓库权限、凭据或安全配置

---

## 7. 敏感信息与忽略规则

### 7.1 禁止提交

- `.env`、`application-local.yml`、任何含真实密码/令牌/私钥的文件
- 数据库密码、JWT 密钥、Redis 密码、云服务凭据
- 构建产物（`target/`、`dist/`、`node_modules/`）
- IDE 与本地环境文件（`.idea/`、`.vscode/` 中个人配置、`*.iml`）
- 日志、临时文件、上传的附件文件（`storage/`、`uploads/`）

### 7.2 `.gitignore` 要点（Stage 1 落地）

```
# Java / Maven
target/
*.class
*.jar
!.mvn/wrapper/maven-wrapper.jar

# Node / Vite
node_modules/
dist/
dist-ssr/
*.local

# IDE
.idea/
.vscode/
*.iml

# 本地配置与敏感信息
.env
.env.local
application-local.yml
application-local.properties

# 运行时数据
logs/
*.log
storage/
uploads/

# 系统文件
.DS_Store
Thumbs.db
```

### 7.3 示例配置

- 仓库只保留 `.env.example`（占位值，明确标注"仅示例，请勿填入真实凭据"）。
- 本地开发凭据通过环境变量注入，说明写入 `README.md`。

---

## 8. 提交与推送前的最终自检

```bash
git status --short          # 仅包含本阶段预期改动
git diff --check            # 无空白错误、无冲突标记
git diff --cached --stat    # 暂存区内容与阶段范围一致
git log --oneline -5        # 提交信息规范、语义清晰
```

确认以下问题均为"是"后才可提交/推送：

- 本阶段功能已按验收标准完成？
- 相关测试已真实运行并通过？
- 暂存区不包含敏感文件、构建产物或无关修改？
- 提交信息准确描述了本次改动？
- 目标分支正确（功能分支开发、`main` 整合）？
- 远程推送前已检查远程无新提交？

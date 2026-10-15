# Stage 0 环境检查报告（只读检查原始记录）

> 检查时间：2026-10-15（Asia/Shanghai）
> 检查方式：全部为只读命令，未修改、未覆盖、未创建任何既有文件；未执行任何写操作的 Git 命令
> 关联文档：[DEVELOPMENT_PLAN.md](./DEVELOPMENT_PLAN.md)、[TECHNICAL_DESIGN.md](./TECHNICAL_DESIGN.md)、[GIT_WORKFLOW.md](./GIT_WORKFLOW.md)

## 1. 项目目录状态

| 检查项 | 实测结果 |
| --- | --- |
| 当前工作目录 | `E:\Work Order Management System`（与需求一致） |
| 目录内容 | 仅存在 `.git` 目录；无代码、无文档、无配置文件 |
| 是否已有 `.gitignore` | 不存在 |
| 是否存在 `.env` / 数据库密码 / 私钥 / 令牌 | 未发现任何敏感文件（目录内除 `.git` 外为空） |
| 是否已有 README / LICENSE | 本地不存在 |

**结论**：这是一个已初始化但完全空白的仓库，不存在需要保留或迁移的既有内容，也不存在覆盖风险。

## 2. Git 与远程仓库状态

| 检查项 | 实测结果 |
| --- | --- |
| 仓库是否已初始化 | 已初始化（`.git` 存在），未重复执行 `git init` |
| 当前分支 | `main` |
| 提交历史 | **无任何提交**（`git log` 报 `your current branch 'main' does not have any commits yet`） |
| 工作区状态 | `git status --porcelain` 无输出（干净） |
| 本地 `user.name` | `Liukoushui0`（仓库级与全局均已配置） |
| 本地 `user.email` | `2964587219@qq.com`（仓库级与全局均已配置） |
| 远程 `github` | `https://github.com/Liukoushui0/Work-Order-Management-System.git`（fetch/push 均已配置） |
| 远程 `gitee` | `https://gitee.com/liukoushui0/Work-Order-Management-System.git`（fetch/push 均已配置） |
| GitHub 远程提交情况 | `git ls-remote --heads github` 返回空 → 远程仓库存在且无任何分支/提交 |
| Gitee 远程提交情况 | `git ls-remote --heads gitee` 返回空 → 远程仓库存在且无任何分支/提交 |
| 远程是否存在 README / LICENSE / 初始提交 | 均不存在（两个远程均为空仓库） |
| 凭据助手 | 未配置（`credential.helper` 为空），`~/.git-credentials` 不存在 |
| 是否需要 `git init` / 添加远程 | **不需要**，已全部就绪 |

**结论**：本地与远程历史一致（双方都为空），首次推送不存在冲突风险，无需合并方案。

## 3. 本地开发环境状态

### 3.1 Java

| 检查项 | 实测结果 |
| --- | --- |
| `java -version` | `1.8.0_202`（Oracle JDK 8） |
| `javac -version` | `javac 1.8.0_202` |
| `JAVA_HOME` | `D:\Tools\envs\jdk8` |
| 已安装的 JDK 列表 | `D:\Tools\envs\jdk8`（8）、`D:\Tools\envs\jdk8-jre`（8） |
| 是否存在 JDK 17 | **不存在**（全盘检索仅发现 JetBrains Runtime 25，位于 IDEA/DataGrip/PyCharm 安装目录） |
| JetBrains Runtime | `D:\Tools\IDEA\IntelliJ IDEA 2026.2.3\jbr`，`JAVA_VERSION="25.0.4"`，含 `javac.exe`（IDE 自带运行时，不建议作为项目 JDK） |

**结论**：**不满足需求（Java 17）**，是当时最主要的阻塞项，见第 5 节 R1。该项已在用户确认后处理完毕，见第 8 节。

### 3.2 Maven

| 检查项 | 实测结果 |
| --- | --- |
| `mvn -v`（PATH） | 命令不可用（Maven 未加入 PATH） |
| 实际安装位置 | `D:\Tools\envs\Maven\apache-maven-3.10.0`（`mvn.cmd` 可用） |
| Maven 版本 | Apache Maven 3.10.0 |
| 当前 Maven 使用的 JDK | `1.8.0_202`（继承 `JAVA_HOME`，需切换至 JDK 17） |
| 本地仓库路径 | `D:\Tools\envs\Maven\apache-maven-3.10.0\repository`（存在但为空，无任何已下载依赖） |
| 用户级 `~/.m2/settings.xml` | 不存在 |
| 全局 settings.xml 镜像 | 已配置 `maven.aliyun.com/repository/public` 作为 central 镜像 |
| 镜像站点连通性 | `repo.maven.apache.org:443` 可达、`maven.aliyun.com:443` 可达 |

**结论**：Maven 可用（需全路径或通过 Maven Wrapper 调用），但首次构建需联网下载全部依赖。

### 3.3 Node.js 与 npm

| 检查项 | 实测结果 |
| --- | --- |
| `node -v` | `v24.21.0` |
| `npm -v` | `11.19.0` |
| Node 安装目录 | `D:\Tools\envs\Nodejs-v24.21.0` |
| npm registry | `https://registry.npmmirror.com`（国内镜像） |
| registry 连通性 | 可达 |

**结论**：满足前端开发要求（注意：Node 24 高于 Vite 5 官方主要验证版本区间，将在 Stage 1 实际构建中验证）。

### 3.4 MySQL

| 检查项 | 实测结果 |
| --- | --- |
| 客户端版本 | `mysql.exe Ver 8.0.46 for Win64` |
| 服务状态 | `MySQL80` 服务 **Running** |
| 监听端口 | `0.0.0.0:3306`、`[::]:3306`（另 33060 为 X Protocol） |
| 数据目录 | `D:/Tools/MySQL/MySQL Server 8.0.46\Data` |
| TCP 连通性 | 127.0.0.1:3306 可达 |
| 凭据可用性 | `mysql -u root`（无密码）返回 `ERROR 1045 Access denied` → **root 密码未知** |

**结论**：MySQL 8 已安装并运行，但**缺少可用凭据**，无法建库或执行集成测试，见第 5 节 R3。

### 3.5 Redis

| 检查项 | 实测结果 |
| --- | --- |
| 安装情况 | `D:\Tools\Redis\Redis-x64-5.0.14.1`（Windows 移植版） |
| 服务端版本 | `Redis server v=5.0.14.1` |
| 是否符合需求的 Redis 7 | **不符合**（实际为 5.0.14.1） |
| Windows 服务 | 未注册任何 Redis/ Memurai 服务 |
| 进程状态 | 无 redis 进程运行 |
| 端口 6379 | 未监听，`redis-cli ping` 返回连接被拒绝 |
| 配置文件 | `redis.windows.conf`：`bind 127.0.0.1`、`port 6379`、无 `requirepass` |
| WSL 可用性 | 未安装（`wsl --status` 提示需要安装 Linux 发行版） |

**结论**：Redis 未运行且版本低于需求声明；Windows 平台无官方 Redis 7 版本，见第 5 节 R4。该项已在用户确认后处理完毕，见第 8 节。

### 3.6 其他工具

| 检查项 | 实测结果 |
| --- | --- |
| Git | `git version 2.56.0.windows.2` |
| 磁盘空间 | E: 剩余 199.7 GB；C: 剩余 117.1 GB（充足） |
| 项目常用端口 | 8080（后端）、5173（前端）当前均未被占用 |

## 4. 网络与远程访问实测

| 目标 | 结果 |
| --- | --- |
| `gitee.com:443` | 可达（TCP 连接成功，`git ls-remote` 正常返回） |
| `github.com:443` | **直连不可达**；经本机代理 `127.0.0.1:7897`（Clash Verge Rev 运行中）时 `git ls-remote --heads github` 正常返回 |
| DNS 解析 | `github.com` → `20.205.243.166`；`gitee.com` → 解析正常 |
| Maven 镜像 | `repo.maven.apache.org`、`maven.aliyun.com` 均可达 |
| npm registry | `registry.npmmirror.com` 可达 |
| Git 代理配置 | git 全局无 `http.proxy`/`https.proxy`，环境变量无代理设置 |

**说明**：沙箱内直接访问 `github.com` 会返回 `schannel: SEC_E_NO_CREDENTIALS`（TLS 凭据不可用），此为网络限制表现；Stage 0 只读检查中改用代理访问并已成功取得结果。

## 5. 风险与处理方案

| 编号 | 风险 | 影响 | 建议处理方案 | 需要用户确认 |
| --- | --- | --- | --- | --- |
| R1 | 本机只有 JDK 8，无 JDK 17 | Spring Boot 3.x 无法编译与运行，Stage 1 起全部阻塞 | 安装 JDK 17（推荐 Eclipse Temurin 或 Microsoft OpenJDK 17），解压/安装到 `D:\Tools\envs\jdk17`，仅通过项目级配置使用，不改动全局 `JAVA_HOME`（或由用户确认后修改） | 是 |
| R2 | Maven 未加入 PATH | 命令行构建不便 | 使用 Maven Wrapper（`mvnw`）锁定 Maven 3.10.0；必要时以全路径调用 | 否（不改变系统环境） |
| R3 | MySQL root 密码未知 | 无法建库、无法执行集成测试 | 由用户提供本地开发库凭据，或由用户创建专用账号（如 `serviceflow`@`localhost`，仅本地开发库权限）；凭据通过环境变量注入，不写入仓库 | 是 |
| R4 | Redis 实际为 5.0.14.1 且未运行，需求声明为 Redis 7 | 与需求不一致；Windows 无官方 Redis 7 | 三种方案：(a) 沿用 5.0.14.1 并在文档中如实标注版本偏差；(b) 安装 Memurai（Windows 原生、兼容 Redis 7 语义）；(c) 安装 WSL2 + Redis 7（需下载与系统变更） | 是 |
| R5 | Maven 本地仓库为空 | 首次构建耗时较长 | 使用阿里云镜像加速；首个阶段预留下载时间 | 否 |
| R6 | GitHub 直连不可用 | GitHub 推送失败 | 通过本机代理（`127.0.0.1:7897`）访问；如需为 `github` 远程配置专属代理，先向用户说明后再配置 | 是（配置代理前） |
| R7 | 无 Git 凭据助手与凭据文件 | 推送时需要凭据 | 由用户提供 GitHub Personal Access Token 与 Gitee 私人令牌（或由用户在系统凭据管理器中配置），仓库内不留任何凭据 | 是 |
| R8 | Node.js 24 高于 Vite 5 主要验证区间 | 可能出现构建兼容问题 | Stage 1 实际构建验证；若失败则记录原因并升级 Vite 6 + Vitest 3，同步更新技术设计文档 | 否（先验证） |
| R9 | 同时开发前后端与双远程同步，操作面较大 | 误提交敏感文件或产物 | 严格执行每阶段提交前检查清单（`git status` / `git diff --check` / 暂存区复核），`.gitignore` 先行落地 | 否 |

## 6. 是否满足进入 Stage 1 的条件

| 条件 | 状态 |
| --- | --- |
| 目录与仓库结构正确 | 满足 |
| 远程 `github` / `gitee` 配置正确且可访问 | 满足（GitHub 需经代理） |
| 无历史冲突风险 | 满足（双方均为空仓库） |
| Node.js / npm / MySQL 可用 | 满足 |
| Java 17 可用 | 已满足（R1 已处理，见第 8 节） |
| MySQL 凭据可用 | 已满足（R3 已处理，见第 8 节） |
| Redis 可用 | 已满足（R4 已处理，见第 8 节，版本偏差经用户确认沿用） |

**结论**：Stage 0 的只读检查与文档产出已完成；R1、R3、R4 三个阻塞项在取得用户确认后已全部处理完毕（第 8 节），R2、R5~R9 已有明确处理方案，不阻塞后续开发。项目具备进入 Stage 1 的条件。

## 7. 本次检查执行的命令（只读）

```text
git status / git branch -a / git log / git rev-parse
git remote -v / git config --get user.name / git config --get user.email
git config --list --local / git ls-remote --heads github / git ls-remote --heads gitee
java -version / javac -version / mvn -v（及全路径 mvn.cmd -v）
node -v / npm -v / npm config get registry / git --version
mysql --version / mysql -u root -e "SELECT VERSION(), CURRENT_USER()"
redis-server --version / redis-cli -h 127.0.0.1 -p 6379 ping
Get-Service / Get-NetTCPConnection / netstat -ano / Test-NetConnection
Resolve-DnsName / netsh winhttp show proxy / Get-ChildItem / Get-Content（配置文件只读）
```

未执行任何写操作类命令：未 `git init`、未添加/删除远程、未 `add`、未 `commit`、未 `push`、未 `fetch` 到本地引用之外的变更、未修改任何环境变量或系统配置。

## 8. 用户确认后的处理结果（Stage 0 收尾）

用户于 2026-10-15 确认了四项决策：(1) 安装 JDK 17 到 `D:\Tools\envs\jdk17`；(2) 提供本地开发库账号 `root` 并确认密码可用；(3) Redis 沿用本机现有版本、无密码；(4) 首次提交按建议执行。据此完成的实际操作如下。

### 8.1 JDK 17（R1 已解决）

| 项 | 结果 |
| --- | --- |
| 来源 | 清华大学开源软件镜像（Adoptium 镜像）：`https://mirrors.tuna.tsinghua.edu.cn/Adoptium/17/jdk/x64/windows/` |
| 文件 | `OpenJDK17U-jdk_x64_windows_hotspot_17.0.20.1_1.zip`（182 MB，下载耗时 32 秒） |
| 安装目录 | `D:\Tools\envs\jdk17`（新建目录，未覆盖任何既有文件） |
| 版本 | Temurin 17.0.20.1+1（`javac 17.0.20.1` 可用） |
| 临时文件 | 下载的 zip 与解压暂存目录已清理 |
| 全局环境变量 | **未修改**（`JAVA_HOME` 仍指向 JDK 8）；项目构建时通过显式指定 JDK 17 使用 |

验证：`JAVA_HOME=D:\Tools\envs\jdk17` 时执行 `mvn -v`，输出 `Java version: 17.0.20.1, vendor: Eclipse Adoptium, runtime: D:\Tools\envs\jdk17`。

### 8.2 MySQL（R3 已解决）

- 使用用户提供的本地开发账号连接验证成功：`SELECT VERSION(), CURRENT_USER()` 返回 `8.0.46` / `root@localhost`。
- 当前实例仅存在系统库（`information_schema`、`mysql`、`performance_schema`、`sys`），无既有业务库，建库环境干净。
- 凭据仅用于本地开发连接，不写入仓库、配置文件或提交信息；应用侧通过环境变量注入。

### 8.3 Redis（R4 已按用户选定方案处理）

- 用户选择"沿用现有 5.0.14.1、无密码"。
- 已以隐藏窗口方式启动 `D:\Tools\Redis\Redis-x64-5.0.14.1\redis-server.exe redis.windows.conf`（工作目录为该 Redis 安装目录）。
- 验证：`redis-cli -h 127.0.0.1 -p 6379 ping` → `PONG`；`info server` → `redis_version:5.0.14.1`，`tcp_port:6379`。
- **版本偏差记录**：实际版本为 5.0.14.1，与需求文档中声明的 Redis 7 不一致；系统仅使用 5.0 已支持的命令（String/JSON 缓存、`SET NX EX` 锁、带 TTL 的黑名单键），功能不受影响。该偏差将在 README 与最终交付报告中如实说明。

### 8.4 首次提交与双远程同步

| 项 | 结果 |
| --- | --- |
| 提交哈希 | `38772a30c60e9e056dd63586b6fe3a4d110bed4f` |
| 提交信息 | `docs(project): add development plan, technical design and git workflow` |
| 提交分支 | `main`（root-commit，4 个文件 / 1523 行新增） |
| Gitee 推送 | 成功：`* [new branch] main -> main` |
| GitHub 推送 | 成功：`* [new branch] main -> main`（经本地代理 `127.0.0.1:7897`） |
| 一致性验证 | 本地 / `gitee/main` / `github/main` 三者均指向 `38772a30c60e9e056dd63586b6fe3a4d110bed4f` |
| 推送前检查 | 已确认两个远程均为空仓库，无新提交、无覆盖风险 |

### 8.5 遗留事项

- 全局 `JAVA_HOME` 与 PATH 未做任何修改；后续所有 Maven 命令均需在 JDK 17 环境下执行，Stage 1 通过 Maven Wrapper 与项目配置固化该要求。
- Redis 当前以手动启动的前台进程方式运行，机器重启后需重新启动；将在 README 中说明启动方式，并评估注册为 Windows 服务（需用户确认后再操作）。
- 首次推送时 Git 凭据由系统凭据助手（`credential.helper=manager`）提供，未在仓库或日志中留下任何凭据内容。

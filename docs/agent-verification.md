# 迭代检索与会话压缩验证

日期：2026-10-02。

## 实现范围

- 单次提问内执行检索、证据核验、针对缺口再次检索、最终流式回答。
- 最近问答与较早摘要同时用于问题改写和生成，压缩水位持久化，原始记录保留。
- 前端展示进度、每轮查询、资料缺口、停止原因及压缩统计。
- 函数库列表、分页、创建、读取、局部修改、软删除、调试、语法检查及执行接口。
- Python 只通过一次性 Docker 容器运行，提供 WSL Ubuntu 部署和隔离测试脚本。
- 局部修改应用配置时，未提供知识库 ID 列表就保留原关联。

## 测试与构建

Java clean package 成功，34 项测试全部通过：迭代检索 7 项、上下文 7 项、Gitee 辅助调用 3 项、Python 调度 8 项、文档解析 9 项。

Python 调度测试使用模拟容器进程验证生命周期，不代表真实容器隔离已验收。另用固定可信代码完成 4 项 runner 协议检查，涵盖参数、语法错误、辅助函数和异步函数；这不是宿主机执行用户代码的回退路径。

前端类型检查与生产构建通过，保留上游包体积等构建提示。

## 先前真实界面及接口验证（阈值策略调整前）

测试应用为“政法多轮问答验证”。

1. 一个问题同时询问行政复议期限及褚庙乡预算金额，触发三轮检索。依次新增 5、2、0 个片段。回答引用期限条文，并明确预算金额不能核实。
2. 临时保留一轮近期问答，触发自动压缩；追问最初的另一事项时，恢复出早期预算问题。
3. 辅助模型调用修正后，再次核实不可抗力影响申请期限，一轮检索判定资料充分，正确引用第二十条第二款。
4. Java 重启后摘要仍在；手动压缩生成修订号三，原始四条问答全部保留。
5. 继续追问“最初问的预算项目”，从摘要恢复具体项目，执行三轮检索，并明确无法核实正式名称及金额。界面显示已压缩三轮、保留近期一轮原文。
6. 测试应用历史保留设置已恢复为五轮；局部修改没有清空知识库关联。
7. 其他访客读取本会话摘要被拒绝；访客访问函数库返回 HTTP 403。
8. 函数库创建、分页、读取和局部更新成功；代码与参数保留。容器未就绪时执行接口明确拒绝，不在宿主机运行代码。

![压缩与迭代检索](verification/compact-agent-ui.jpg)

## Python 环境状态

WSL 官方安装包已验证微软签名并安装成功。Windows 可选组件启用遇到 `0x800f081f`（组件源缺失）。在线组件修复成功后仍未解决，提供的 ISO 已尝试作为组件源，仍返回相同错误；ISO 的组件版本落后于系统需要的累积更新。Ubuntu、Docker Engine 及真实容器执行仍需完成验收；未重启 Windows。

容器就绪后的命令：

```powershell
.\scripts\Setup-PythonSandbox.ps1 -Distribution Ubuntu
.\scripts\Test-PythonSandbox.ps1 -Distribution Ubuntu
```

任意原版工作流、函数导入导出、完整 Pylint 规则、联网函数及额外 Python 包仍需按各自范围验收。

## 2026-10-03 更新

- 自动和手动压缩仅在历史摘要与未压缩问答的估算 token 超预算时执行。低于或等于阈值不压缩，也不因轮数单独触发；真正压缩时才发送 compacting 事件。
- 模型生成改用 UniChatClient，移除业务层自建 HTTP 传输。客户端本地 HTTP 请求验证 2 项通过；Java 后端新增用户密码测试后共 38 项通过，前端生产构建通过。
- 两个普通账号已通过 API 验证：固定 USER 角色、登录、私有资源列表隔离、跨账号应用详情/删除/分享令牌与知识库详情/修改拒绝、管理接口拒绝、密码重置旧令牌失效、禁用后登录拒绝。
- 已有短会话主动压缩返回 compacted=false。
- 重启后数据库正常，应用服务已手动恢复。虚拟化固件启用，但 Windows 可选组件仍缺少源文件。按用户要求暂缓 Docker，不再继续系统修复。
- 前端补丁及逐文件校验清单见 customizations/2026-10-02；后续变化继续在该快照说明中记录。

当前界面补充验收：管理员“系统 → 用户”列表加载成功，“创建用户”对话框正常展示。API 已验证逻辑删除，全部 6 个临时验证账号已停用（其中一个已逻辑删除）。正式人员账号未代建。

通过统一客户端完成真实 UI 两轮问答：行政复议一般期限为六十日；追问不可抗力时正确引用同一资料第二十条。两次均一轮检索充分，短上下文未压缩。界面证明：

![统一客户端与近期历史](verification/unichat-followup-ui.jpg)

![官方用户创建界面](verification/create-user-ui.jpg)

最终回归：使用自身知识库路径嵌入其他账号文档 ID，详情与段落列表均返回 HTTP 403；自身问题列表为空，非法问题批量删除被拒绝，自身应用分页仍正常。最终 clean package 38 项测试通过，服务已恢复运行。

## 远程 Docker 引擎接入（2026-10-03）

本机仍然没有 WSL 发行版和 Docker，改用局域网主机 `192.168.31.97`（Docker Engine 29.8.1，linux/amd64）作为隔离 Python 执行器的容器引擎。数据库不变，仍为本机 `127.0.0.1:5432/max_kb`。

改动：

- `IsolatedPythonExecutor.dockerCommand()` 支持 `kb.python.docker.host`，作为 `--host` 全局参数加在子命令之前，`run` 与失败清理的 `rm -f` 都走同一地址。`DOCKER_HOST`、`DOCKER_CONTEXT`、`DOCKER_TLS_VERIFY`、`DOCKER_CERT_PATH` 仍在环境透传白名单里。
- 配套改动：runner 源码不再直接作为 `python -c` 的参数。Windows 的 `CreateProcess` 会破坏参数中的双引号，容器能启动但 Python 报 `SyntaxError: unterminated string literal`（WSL 方案同样会踩到，这也是此前容器验收一直没能完成的原因）。现在 `-c` 传入不含空格和双引号的 base64 引导脚本，stdin 上的请求 JSON 协议不变。
- `scripts/Test-PythonSandbox.ps1` 改为默认读取 `my.txt` 的实际配置（也可用参数覆盖），不再硬编码 WSL；`scripts/enable-remote-docker-tcp.sh` 在被接入主机上用 socat 容器转发 unix socket，并用 iptables 只放行指定来源。

验证结果：

- `Test-PythonSandbox.ps1` 在远程引擎上通过：容器内 uid 65534、根文件系统只读、无网络、`GITEE_API_KEY` 不可见。
- `POST /api/function_lib/debug` 真实执行成功，返回容器内事实：`uid=65534`、主机名等于容器 ID、内核 `6.1.0-53-amd64`（证明执行发生在远程 Linux 容器而不是 Windows 宿主）、Python 3.12.15、根目录不可写、网络 blocked。
- 参数与入口函数：按前端契约传 `init_params` + `debug_field_list` + `input_field_list` 得到 `2+40=42`；缺少必填参数返回“缺少函数参数：a”。
- 错误路径：容器内 `1/0` 返回 `division by zero`；返回值超过 256 KiB 返回“函数返回结果超过 256 KiB”；无顶层函数返回“代码需要至少一个顶层函数”；`/pylint` 对语法错误返回行列信息。
- 执行结束后远程 `maxkb-python-*` 容器为空，`--rm` 与失败清理都生效。
- 引擎不可用时（2375 尚未放开时实测）仍返回“Python 容器启动或执行失败”，不使用宿主机 Python 回退。
- 后端回归 44 项通过（新增 `runnerIsPassedAsSpaceAndQuoteFreeBootstrap` 与 `remoteEngineHostIsPassedAsGlobalFlagBeforeRun`），clean package 与 jar 内容均已核对。

风险与后续：

- `tcp://192.168.31.97:2375` 是明文且无认证的 Docker API，能连上即等于该主机 root 权限。当前只用一条 `DOCKER-USER` 规则放行 `192.168.31.225`，需要人工确认规则存在；主机重启后规则会丢失，需要重跑 `enable-remote-docker-tcp.sh`。更稳妥的做法是 2376 双向 TLS 或 `ssh://` 隧道，代码侧已具备对应参数的支持位置。
- 尚未做前端按钮点击级的验收；接口契约按前端实际字段验证。任意工作流、函数导入导出、完整 Pylint 规则、联网函数及额外 Python 包仍需按各自范围验收。

## 宿主 Python 沙箱（Linux，2026-10-03）

按“Windows 用 Docker over TCP、Linux 允许宿主提权沙箱”的要求，执行器改为两个 runner，`kb.python.runner=docker`（默认，原行为不变）或 `host`。`host` 不需要 Docker：用 util-linux 的 `runuser` 切到沙箱用户，在独立目录里跑 `python3 -I -B -c <base64 引导>`，请求 JSON 仍走 stdin，协议与 Docker runner 完全一致，也不落脚本文件。

关键发现：**不能用 `su` 做默认**。`/bin/su` 是 setuid 程序，而 JDK 默认用 posix_spawn 创建进程——实测由 JVM 启动 `su` 会在 `ProcessBuilder.start()` 卡死；加 `-Djdk.lang.Process.launchMechanism=FORK` 后不再卡死，但子进程返回 `su: Authentication failure`（exit 1）；同一台机上用 shell 调用同样的 argv 却正常。改用以 `runuser`（无 setuid、argv 直传、不走 shell）为默认后，JVM 启动正常。代码里保留了 `kb.python.sandbox.su` 作为显式逃生口，并注明上述限制。

`host` runner 的前置校验，任一条不满足即拒绝执行、绝不退回 Java 进程身份：类 Unix 平台、沙箱用户存在、沙箱用户不是 root、Java 进程为 root、沙箱目录存在且不是所有人可写、能找到 `runuser`。子进程只继承白名单环境变量（`PATH`、`LANG`、`LC_*`、`TZ`、`TERM`）加固定的 `HOME`/`TMPDIR`/`USER`/`LOGNAME`/`SHELL`；超时用 `ProcessHandle.descendants()` 连同用户代码进程整棵树终止。

验证方式与结果：

- 单元测试 `IsolatedPythonExecutorTest` 增至 19 项：runuser 命令形状（不含 shell、不含 su）、显式 `su` 形式的 shell 安全性（引导脚本不含 `$`、反引号、反斜杠、双引号，且能解码回 runner.py）、六条拒绝路径、环境白名单不泄漏父进程变量。
- 真实 Java 代码在 Linux 上执行：把 Temurin 21 JRE 与 `maxkb-business` 的 classes 复制进一个 Linux 容器（`python:3.12-slim`），以 root 运行 `RunHostProbe`（调用真实的 `IsolatedPythonExecutor.execute`），并把 `GITEE_API_KEY=must-not-leak` 注入 JVM 环境。结果：`uid=1001`、`user=sandbox`、`cwd`/`HOME` 等于沙箱目录、可见环境变量只有白名单那 7 个、`secret_absent=true`，同时 `JAVA_ENV_HAS_SECRET=true`——即 JVM 自己持有密钥而沙箱里看不到。
- `scripts/setup-python-host-sandbox.sh` 在同一 Linux 环境执行通过：创建用户与 0750 目录并用 `runuser` 校验 uid。
- 改动后 Docker runner 复测：`POST /api/function_lib/debug` 返回 `uid=65534`、`secret_absent=true`；`Test-PythonSandbox.ps1` 仍为只读根、无网络、密钥不可见。
- 后端回归 53 项通过（执行器 19 项），clean package 成功。

如实说明的限制：

- 上述 Linux 验证是在容器里模拟 Linux 宿主并以 root 运行真实 Java 代码，不是裸机 Linux 上以真实 java-maxkb 进程（含数据库启动）验收。
- `host` runner 不提供断网、只读根文件系统、内存/CPU/pids 上限，也不提供一次性文件系统；沙箱用户可以读宿主上 others 可读的任何文件，所以 `my.txt`、`secrets.txt` 必须 `chmod 600`。需要这些保证时继续用 `kb.python.runner=docker`。
- `host` runner 要求 java-maxkb 以 root 运行；非 root 部署只能用 Docker runner。
- 官方 MaxKB 是“写脚本文件 + `su -c`”的方式，这里刻意不落文件：脚本文件的属主处理在 java-maxkb 非 root 时做不到，而走 stdin 既避免属主问题，也不留残留文件。

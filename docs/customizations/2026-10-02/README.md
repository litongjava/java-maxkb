# MossKB 前端定制恢复记录

用户约定：前端以官方代码为基线，Java 后端优先适配官方接口；必须保留的定制单独记录。Java/TypeScript 新增或修改的 if 语句使用花括号。Maven 构建加 `-Dgpg.skip=true`。

## 本次快照的边界

当前工作区尚在合并中，未提交、未重置，也未改写已有暂存区。补丁基准是当前 fork 的合并目标 `847755b1c2bba658a2062e0f47dd97fa8ae37247`；来源是 `litongjava/MossKB`，不能把它标为经核实的官方上游提交。本补丁保存基准与当前前端之间的完整差异，包含已存在的定制，不代表全部是本次新增。

- `frontend-core.patch`：源代码、构建配置及新增组件。
- `frontend-lock.patch`：单独保存当前依赖锁文件，升级后通常应重新生成，默认不应用。
- `environment.example`：API 地址与开发代理模板，不含密钥。按需合入 `ui/env/.env.development`、`.env.production`，不要覆盖已有环境配置。
- `manifest.json`：基准提交、文件清单、换行归一化后的 SHA-256。导出时在临时目录应用补丁并逐文件核对，未触碰源仓库索引。

## 修改分类

以下路径均相对于 MossKB 仓库。

| 类别 | 文件 | 用途与升级核对点 |
| --- | --- | --- |
| 独立前端 | `ui/src/router/index.ts`、`ui/src/stores/modules/application.ts` | Hash 路由与分享地址，不依赖后端页面路由；保留原有需求 |
| API 地址 | `ui/src/api/application.ts`、`ui/src/request/index.ts`、`ui/vite.config.ts` | 普通请求和 SSE 使用相同 API 地址；开发代理到 Java 后端 |
| 文件 ID | `ui/src/views/dataset/UploadDocumentDataset.vue` | 提交 `id: item.id`，让后端关联已解析文件；必须保留 |
| 文档类型 | `ui/src/views/dataset/component/UploadComponent.vue`、`ui/src/utils/utils.ts` | DOC、图像及扫描件入口，与后端检测和 OCR 策略对应 |
| 登录能力 | `ui/src/stores/modules/user.ts`、`ui/src/views/login/index.vue` | 按后端 `captcha_enabled` 决定是否请求验证码 |
| SSE 兼容与 Agent | `ui/src/components/ai-chat/index.vue` | 保留 UTF-8 解码状态、识别命名事件与错误、读取最终检索详情 |
| Agent 展示 | `ui/src/components/ai-chat/AgentProgress.vue`、`component/answer-content/index.vue`、`ui/src/api/type/application.ts` | 展示检索轮次、缺口、停止原因、压缩统计；`compacting` 仅在超 token 预算且实际压缩时出现 |
| 构建兼容 | `ui/package.json`、`ui/tsconfig.*.json`、CodeMirror/Markdown 包装组件、TableCheckbox/TableRadio、document/log/problem 页面 | 显式依赖、属性透传与表格类型；上游修复后删除对应补丁段 |
| 原有业务定制 | `ui/src/utils/application.ts`、应用创建/列表及工作流 DropdownMenu | 部门机构类型；本次保留已有内容，不能算作新增的官方功能 |
| 原有检索默认值 | `ui/src/views/hit-test/index.vue` | 相似度 0、结果数 10；升级时按业务需求决定是否保留 |
| 本地文件 | `ui/.gitignore` | 忽略 `.local/` 日志和运行文件 |

## 升级与恢复

先保存或提交当前工作，正常完成已有合并。升级官方代码时记录真实 upstream 仓库及提交；先检查 Java 接口是否可直接兼容，再决定保留哪些补丁。不要把旧锁文件自动覆盖到新版本。

在 Java 仓库目录执行（路径按实际位置调整）：

```powershell
# 默认仅检查，不修改目标仓库
.\scripts\Apply-FrontendCustomizations.ps1 -Repository ..\MossKB -Bundle .\docs\customizations\2026-10-02
# 检查通过且确认适用后应用
.\scripts\Apply-FrontendCustomizations.ps1 -Repository ..\MossKB -Bundle .\docs\customizations\2026-10-02 -Apply
```

脚本遇到未结束的合并或补丁冲突会停止。新上游提交不保证自动适用；按表中行为人工合并冲突，不要强行覆盖。`-IncludeLock` 仅用于明确需要恢复本快照锁文件时。

恢复后安装依赖并执行 `npm run build`，从 UI 验证登录、上传解析、文件 ID、独立路由、分享聊天、中文 SSE、迭代检索和长上下文；短对话不应显示压缩进度。历史截图中“按轮数压缩”的旧测试结果不代表当前触发条件。

重新生成新基准快照：

```powershell
python .\scripts\Export-FrontendCustomizations.py --repo ..\MossKB --base <已核对的基准提交> --output .\docs\customizations\<日期>
```

## Java 后端和客户端记录

Java 后端是独立实现，不对 Python 后端套补丁。主要改动：

- `IterativeRetrievalService`：问题改写、检索与证据核验循环。
- `ConversationContextService`、`ContextBudget`、`ChatExecution`：token 阈值、摘要持久化、会话互斥。只有超出 token 预算才压缩；轮数只作为压缩后近期原文的保留目标。
- `KnowledgeModelService`：业务提示词、模型配置及结果校验；生成调用 `UniChatClient.generate`，向量调用客户端的 embeddings API。不再自建 Gitee HTTP 客户端。
- `MossKbApplicationChatMessageService`：最终流式回答使用 `UniChatClient.streamOpenAi`，沿用取消句柄和聊天记录持久化回调。
- `ApiChatContextController`：读取摘要和检查压缩接口；手动入口同样遵守 token 阈值。
- `IsolatedPythonExecutor`、`PythonFunctionService`、`ApiFunctionLibController`：容器执行和函数库接口；Docker 未就绪时拒绝执行。
- `005-agent-context.sql`、`006-python-functions.sql`：持久化迁移；由数据库初始化脚本执行。
- `MossKbApplicationService`：局部更新缺省知识库列表时保留原关联。
- `java-openai`：统一请求支持 `thinking`，同步与流式请求共用转换逻辑；新增 OpenAI 兼容原始回调流式入口，保持可取消能力。新增本地 HTTP 协议测试。

后端配置、接口及验收见 [验收记录](../../agent-verification.md) 和框架知识库文档第 32 章。Windows 的 WSL 组件源问题尚未解决时，不应将模拟容器测试视为真实隔离验收。

## 2026-10-03 独立用户补充

前端用户管理页保持原代码。Java 新增 UserManagementService、UserPassword 和 007-user-sessions.sql，补齐创建、资料编辑、启停、密码重置及逻辑删除；ApiValidController 增加用户创建检查。认证检查有效状态及 token_version，密码重置或启停后旧令牌失效。普通用户权限按自身资源生成，应用分页过滤拥有者，关联知识库校验归属，ModelAccess 控制公共模型使用与私有模型访问。

Docker 按用户要求暂缓；容器代码与部署脚本保留。使用说明同步到知识库文档第 34 章。

## 后端恢复材料

`java-backend.patch` 保存当前 Java 源码、测试、解析运行资源及脚本相对该仓库 HEAD 的差异；`java-openai.patch` 保存统一客户端扩展及其测试。相应 manifest 记录各自基准提交和文件校验值，均在临时目录应用并逐文件核对。快照包含保留的先前源代码修改，不包含 my.txt、secrets.txt、日志、数据库内容及构建产物。Maven POM 和依赖环境不在后端源码补丁范围，升级时按现有项目配置处理。

在对应仓库先执行 `git apply --check <补丁绝对路径>`，确认基准与接口兼容后再 `git apply <补丁绝对路径>`；不得将 java-openai 补丁应用到 Java MossKB 仓库。新环境需执行数据库迁移并重新安装修改后的统一客户端，再构建 Java 后端。运行文件不包含任何凭证。

# Java MaxKB

基于 tio-boot 的 Java 知识库后端，复用 MaxKB 前端。通过文档检测、结构提取、远程 OCR、向量检索和多轮问答，将文档转为可追溯的知识库回答。模型生成复用 `java-openai` 的 `UniChatClient`，本机无需部署模型权重。

[前端仓库](https://github.com/litongjava/MaxKB/tree/main/ui) · [Java 后端仓库](https://github.com/litongjava/java-maxkb) · [演示视频](https://www.bilibili.com/video/BV1yJU8YHEgg/)

开发文档维护在 `tio-boot-docs/docs/zh/61_knowledge-base`。本地文档根目录为 `D:/code/markdown/project-litongjava/tio-boot-docs/docs/zh/61_knowledge-base`，主要章节：

| 章节 | 内容 |
| --- | --- |
| `01.md` / `29.md` | 数据库设计、Windows pgvector 安装、配置与启动 |
| `03.md` / `35.md` | 模型管理、数据库模型目录、平台接入与自定义模型 ID |
| `31.md` | 前端适配与 UI 验证 |
| `32.md` | 单次问题内迭代检索、会话历史、token 阈值压缩、隔离 Python |
| `33.md` | 官方前端升级与定制恢复 |
| `34.md` | 独立用户和资源权限 |

## 当前能力

- 用户、知识库、文档、分段、问题及应用管理；普通账号按所属资源隔离，支持公共模型和分享聊天。
- 文档先检测再解析：文字 PDF、本地结构化提取、扫描页和图片 OCR、混合 PDF 逐页处理，解析预览保留文件 ID。
- 向量、关键词及混合检索；文档和查询使用知识库选定的同一个向量模型。
- 单次提问执行“检索 → 判断资料是否充足 → 补充检索 → 最终回答”，保留资料引用与检索过程。
- 读取当前会话最近问答及摘要；**只有 token 超过预算才压缩上下文**，超过对话轮数不会单独触发压缩。
- 平台目录、基础模型目录和表单入库；支持 OpenAI 兼容厂商与中转平台，允许手动输入模型 ID。
- 隔离 Python 执行器代码及部署脚本保留，需要 Docker 容器环境。当前本机 Docker 安装暂缓，环境未就绪时拒绝执行，不使用宿主机 Python 回退。

统计分析、Milvus 替代方案和完整官方工作流能力，不因前端有菜单或文档有设计示例就视为已全部实现。

## 目录与运行环境

推荐将两个仓库放在同一父目录：

```text
project-maxkb/
  java-maxkb/
    maxkb-business/       业务逻辑与测试
    maxkb-web/            HTTP 入口和本地配置
    scripts/             初始化、启动、目录同步与补丁导出
    docs/                验证记录、定制说明和补丁
  MaxKB/
    ui/                  官方前端及已记录的必要定制
```

需要 JDK 21、Maven、Node.js/npm、已启动的 PostgreSQL，以及安装在数据库服务端的 pgvector 和 pg_trgm。默认远程服务需要可用的 Gitee API Key。Docker 仅为隔离 Python 执行功能所需。

## 配置与初始化

在 `maxkb-web/my.txt` 创建本地配置，每行一项：

```properties
jdbc.url=jdbc:postgresql://127.0.0.1:5432/<实际数据库名>
jdbc.user=<数据库用户>
jdbc.pswd=<数据库密码>
app.admin.secret.key=<本机生成的随机签名密钥>
server.port=10060
app.env=dev
jdbc.showSql=false
```

在 `maxkb-web/secrets.txt` 设置：

```properties
GITEE_API_KEY=<自己的密钥>
```

这两个文件、运行日志和测试令牌保持在 Git 忽略范围，不放入前端配置或补丁。

如果出现 `extension "vector" is not available`，需要先在实际运行 PostgreSQL 的机器上安装扩展文件，再连接业务数据库启用扩展；仅执行 SQL 不能安装缺失的二进制。Windows 编译安装步骤见文档第 29 章。

已有数据库不需要删除重建。在 Java 仓库根目录执行：

```powershell
.\scripts\Initialize-Database.ps1 -PostgresBin '<PostgreSQL安装目录>\bin'
```

脚本读取 `my.txt`，执行 `init-db.sql` 与 `002` 至 `009` 的迁移，包括默认模型、会话摘要、用户令牌版本、数据库模型目录及目录快照。升级已有数据库前先备份；幂等脚本不会自动转换所有历史表结构。

## 构建与启动

先在 `MaxKB/ui` 安装前端依赖：

```powershell
npm install
```

然后在 Java 仓库根目录构建并启动：

```powershell
.\scripts\Start-Local.ps1 -Build
```

该脚本后台启动 Java 与 Vite；`-Build` 构建会跳过测试并跳过 GPG 签名。可用 `-JavaExecutable`、`-NodeExecutable`、`-MavenExecutable` 指定实际可执行文件路径。已有端口监听会被复用；修改代码后需停止确认属于本项目的旧进程，再重新构建启动。

- 前端：<http://localhost:3000/ui/>
- Java API：<http://localhost:10060/api>
- 后端日志：`maxkb-web/logs/startup.log`、`startup-error.log`
- 前端日志：`MaxKB/ui/.local/vite.log`、`vite-error.log`

也可以分别执行：

```powershell
mvn clean '-DskipTests' '-Dmaven.javadoc.skip=true' '-Dgpg.skip=true' -Pproduction package
.\scripts\Start-Local.ps1
```

Java 运行工作目录为 `maxkb-web`，以便加载本地配置。实际可运行 JAR 位于 `maxkb-web/target`，不要使用旧 README 中不存在的根目录 JAR 路径。

## 创建用户与使用知识库

管理员登录后，在“系统 → 用户 → 创建用户”新增账号。普通账号登录后拥有自己的应用、知识库和私有模型；停用或密码重置会使旧令牌失效。管理员仍有管理权限；需要数据库和进程也完全独立时，应分别部署实例。

基本流程：创建模型 → 创建知识库并选择向量模型 → 上传文档并检查解析预览 → 确认分段 → 创建应用并关联知识库和聊天模型 → 在 UI 调试及追问。前端提交分段时必须保留解析返回的文件 `id`。

## 模型配置

平台信息存于 `max_kb_model_provider`，候选模型存于 `max_kb_model_catalog`，用户创建的实例与凭据存于 `max_kb_model`。运行时不再读取固定供应商 JSON。

在“系统 → 模型 → 添加模型”选择平台，选择或手动输入完整模型 ID 后按回车，再填写 API 地址及 Key。保存校验使用实际填写的模型 ID；编辑时掩码 Key 保留原凭据，更换 API 地址必须重新填写新平台 Key。

预置 OpenAI、Gitee、OpenRouter、硅基流动、DeepSeek、百炼、Kimi、智谱、Gemini 兼容接口、火山方舟和自定义中转入口。Claude 可以通过兼容中转使用，不代表已接通全部厂商原生协议。不同平台需要各自的有效密钥。

默认 Gitee 配置：

| 用途 | 模型 |
| --- | --- |
| 最终回答默认模型、问题改写、证据核验和摘要 | `deepseek-v4.1-flash` |
| 默认远程向量 | `Qwen3-Embedding-8B`，1024 维 |
| 扫描页与图片 OCR | `PaddleOCR-VL-1.5` |

应用选择的模型用于最终回答；辅助步骤仍使用配置的 Gitee 模型。向量模型必须返回 1024 维，已有文档的知识库不能直接切换向量空间，需要新建知识库并重新导入文档。

目录快照包含获取时的平台模型，不代表永久可用或所有模型已实测。管理员可刷新 OpenRouter/Gitee 目录：

```powershell
python scripts/Sync-ModelCatalog.py --source openrouter --token-file <管理员令牌文件>
# Gitee 需先在当前进程环境中设置 GITEE_API_KEY
python scripts/Sync-ModelCatalog.py --source gitee --token-file <管理员令牌文件>
```

其他兼容平台可通过管理员接口 `POST /api/provider/catalog/discover` 发现 ID，再用 `PUT /api/provider/catalog` 明确类型并登记。同步不自动删除未返回项，停用项由管理员明确设置 `enabled:false`。目录维护不需要重新编译。

## 验证

本次后端回归命令：

```powershell
mvn '-Dtest=ModelCatalogTest,UserPasswordTest,IterativeRetrievalServiceTest,ConversationContextServiceTest,IsolatedPythonExecutorTest,GiteeAuxiliaryModelTest,DocumentParsingServiceTest' '-Dsurefire.failIfNoSpecifiedTests=false' '-Dmaven.javadoc.skip=true' '-Dgpg.skip=true' test
```

2026-10-03 的构建回归共 42 项通过。另验证了真实 Gitee 创建/掩码编辑、跨平台密钥保护、UI 目录及手动 ID、知识库连续追问。其他平台没有提供真实 Key，尚未进行全部平台的推理验收；容器模拟测试不等于真实 Docker 隔离验收。前端构建在 `MaxKB/ui` 执行 `npm run build`。

## 前端基线与定制恢复

前端以官方接口和代码为基线，Java 优先适配接口；独立路由、文件 ID、SSE 与 Agent 展示等必要定制单独保存。本次模型管理没有新增前端修改。

最新补丁与完整清单见 [2026-10-03 定制记录](docs/customizations/2026-10-03/README.md)，历史行为清单见 [2026-10-02 记录](docs/customizations/2026-10-02/README.md)。当前快照基准是已有 fork 的合并目标，不能将其称为已经核实的官方 upstream 提交；当前已有合并状态保留。

```powershell
# 默认只检查；正在合并的仓库需要先正常完成合并
.\scripts\Apply-FrontendCustomizations.ps1 -Repository ..\MaxKB -Bundle .\docs\customizations\2026-10-03
```

通过检查后再用 `-Apply` 应用；锁文件补丁单独保存，升级时不要盲目覆盖。前端和后端导出脚本会在临时目录应用补丁并校验内容，不修改工作仓库索引。Java 新增或修改的 `if` 语句必须使用 `{}`，Maven 构建可加 `-Dgpg.skip=true` 跳过签名。

## 许可证

Java 项目许可证以 [LICENSE](LICENSE) 为准。前端及其他依赖遵循各自仓库的许可证。

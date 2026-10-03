# 2026-10-03 定制快照与模型目录更新

本目录保存可核对和恢复的差异。前端仍以官方代码和接口为目标基线，Java 后端优先适配。具体历史定制原因见[上一份记录](../2026-10-02/README.md)。

## 基准与验证

- 前端基准：fork 合并目标 `847755b1c2bba658a2062e0f47dd97fa8ae37247`，不是已核实的官方 upstream 提交。
- 前端 33 个文件的校验值与上一份快照一致，本次模型管理没有修改前端。
- 后端相对于 `java-backend-manifest.json` 记录的基准导出 90 个源码、测试及脚本文件，包含保留的历史定制。
- 前后端补丁均已在临时目录进行 `git apply --check`、实际应用和换行归一化后的内容校验。
- `java-openai.patch` 和清单沿用上一份已验证快照，本次没有新增客户端修改。
- 不包含本地配置、密钥、日志或 POM。构建依赖按项目现有配置维护，不用补丁覆盖本地凭据。

## 本次后端修改

1. `ModelCatalogService` 与 `ApiProviderController` 从数据库查询平台、候选模型及表单，保留前端 API 结构。
2. `008-model-catalog.sql` 创建平台与目录表；`009-model-catalog-snapshot.sql` 保存平台目录快照；初始化脚本顺序执行两者。
3. `Sync-ModelCatalog.py` 从 OpenRouter/Gitee 获取候选目录，再通过管理员接口登记；批处理专用、非文本输出等项目不作为聊天目录导入。
4. 模型保存校验实际模型 ID，生成复用 `UniChatClient`；详情隐藏密钥，掩码编辑保留密钥，换地址要求新 Key。
5. 文档导入、分段编辑和查询使用知识库选择的向量模型，跨库查询按模型分组；当前固定 1024 维，保护已使用的向量空间。
6. `ModelCatalogTest` 验证自定义 ID、实际请求内容、错误维度及密钥隔离。
7. README 和正确的 `docs/zh/61_knowledge-base` 文档同步更新；文档不属于源码补丁范围。

## 使用

先保存工作区并正常完成当前合并，记录升级后的真实 upstream 提交。前端补丁通过 `scripts/Apply-FrontendCustomizations.ps1` 检查后按需应用。Java 补丁相对于其清单中的独立仓库基准，不能应用到 Python 后端。升级冲突应按行为记录人工处理，不能强制覆盖。

重新导出：

```powershell
python scripts/Export-FrontendCustomizations.py --repo ../MaxKB --base <核对过的基准> --output docs/customizations/<日期>
python scripts/Export-BackendCustomizations.py --repo . --base <Java基准> --output docs/customizations/<日期>
```

本次 42 项后端回归通过，真实 Gitee 模型创建/编辑与 UI 连续追问通过。其他平台推理需要用户对应平台的 Key；Docker 暂缓，不能把模拟容器测试称为真实容器验收。

## 文档维护位置

用户最终确认的目录为 `tio-boot-docs/docs/zh/61_knowledge-base`。已更新相关章节和导航，并补全 Java 项目 `readme.md` 的环境、配置、初始化、启动、模型、用户、测试及补丁恢复步骤。此前误指定目录中的编辑不作为当前维护入口；其本轮新增侧栏入口已撤回。

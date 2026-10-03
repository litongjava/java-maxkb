# 豫自然资办发〔2021〕18号文档解析实测

测试日期：2026-09-26。使用用户指定的原始 PDF，35 个物理页，12,218,806 字节。

SHA-256：`9715ec1ac15de4127e53225694d2d6c411d2073e56f8ce3de5e550ef5abdf9dc`。

## 结论

**针对这份文件的正文与重要日期保留，HunyuanOCR 表现最好；作为 MaxKB 默认的整份 PDF 解析服务，建议优先 PaddleOCR-VL-1.5。**

- HunyuanOCR 的抽样正文无文字差异，22 项页内关键内容全部命中，同时识别了首页印章覆盖区域的日期和末页印发日期。但需要将 PDF 逐页转为图片，累计请求时间约 8 分 50 秒，表格结构仍有错误。
- PaddleOCR-VL-1.5 的抽样正文也无文字差异，保留首页日期，整份文件约 95 秒完成，接口接入更直接。不过第 35 页仅返回空内容，遗漏末页印发信息。
- MinerU2.5-Pro 约 52 秒完成，抽样正文同样很好，可作为速度优先的选择，但遗漏首页日期和末页印发信息。
- **第 27 页的 72 列超宽表格，没有任何模型在本次测试中可靠还原。** 尤其不能把长 HTML 或更多字符当成准确率高。该页有原生 PDF 文本层，实际应用应优先利用原生文本和表格坐标，或切分表格后重新识别并人工核验。

这是这份文件、此次参数下的服务表现，不是所有文档的通用模型排名。尚未将模型选择自动接入业务解析流程。

## 全部结果

| 模型 | 调用方式 | 耗时 | 抽样字符编辑率 | 22 项页内关键项命中 | 主要情况 |
| --- | --- | ---: | ---: | ---: | --- |
| HunyuanOCR | Gitee 同步图片 OCR，逐页 | 529.817 秒累计请求时间 | 0 / 5,316，0.000% | 22 / 22 | 日期、落款完整；普通表格文本较好，结构有误；宽表严重重复 |
| PaddleOCR-VL-1.5 | Gitee 异步 PDF | 95.485 秒 | 0 / 5,316，0.000% | 20 / 22 | 首页日期正确，末页为空；宽表遗漏大量内容 |
| MinerU2.5-Pro | Gitee 异步 PDF | 52.470 秒 | 0 / 5,316，0.000% | 19 / 22 | 正文和速度较好；漏日期、末页；宽表出现异常列跨度 |
| PDF-Extract-Kit-1.0 | Gitee 异步 PDF | 22.579 秒 | 5 / 5,316，0.094% | 18 / 22 | 最快；跨页段落合并，日期遗漏；宽表不可靠 |
| DeepSeek-OCR | Gitee 异步 PDF | 750.092 秒 | 5 / 5,316，0.094% | 20 / 22 | 最慢；漏首页日期和末页日期；宽表大量重复、错字 |
| Unlimited-OCR | Gitee 异步 PDF | 145.500 秒 | 12 / 5,316，0.226% | 21 / 22 | 末页印发信息保留；首页日期遗漏；夹杂布局坐标、英文说明 |
| MinerU2.5 | Gitee 异步 PDF | 72.290 秒 | 15 / 5,316，0.282% | 19 / 22 | 正文有少量差异；漏日期、末页；宽表仅少量内容 |
| DeepSeek 官方 deepseek-ocr | 官方模型列表核验 | 不适用 | 未测试 | 不适用 | 当前账号模型列表没有 OCR 模型 |

异步耗时采用服务端 `completed_at - created_at`，包含排队，未计本地上传时间。HunyuanOCR 是 35 页成功请求耗时之和，不含图片渲染、人工排查停顿和尺寸拒绝请求，与异步服务端耗时口径不同，不能据此推断纯推理速度。恢复运行日志只显示该次执行时间，应以逐页记录累加值为准。

6 个异步成功任务均返回 `price: 0.7`、`currency: CNY`，合计 4.2 元。HunyuanOCR 同步响应没有价格字段，未估算其费用，因此 4.2 元不是全部测试的总费用。

## 评价方法

1. 6 个 PDF 接口提交完全相同的原文件，不裁页、不设置 `end_pages`。关闭 `X-Failover-Enabled`，避免模型降级干扰结果。DeepSeek-OCR 使用客户端已有 grounding 提示词；其他模型使用平台默认提示词。比较的是服务默认解析方案，而非统一参数的裸模型。
2. HunyuanOCR 使用 PDFBox 按 200 DPI 渲染每页。普通页实际图片为 1653×2338；第 27 页为 3307×2338，被服务拒绝后等比例缩小为 2048×1448。各服务内部缩放策略未知，宽表比较存在预处理差异。
3. 原 PDF 第 1—26、34—35 页没有可提取文本层，第 27—33 页有文本层。正文抽样采用人工核对抄录的第 3、12 页，以及原始文本层提取并抽查的第 28—32 页，共 7 页、5,316 个归一化字符。其中扫描页样本 1,017 字符，文本层页样本 4,299 字符。
4. 删除布局标签、坐标、HTML/Markdown 标记、空白和标点，统一 Unicode 字符形式，移除 LaTeX 格式命令。仅比较汉字、字母和数字。**该指标不检验标点、数学运算符、印章图像、表格网格或全文完整性。**
5. 使用编辑距离计算抽样字符编辑率。为避免把跨页合并误判为漏字，在相邻三个结果页组成的窗口内对齐样本。
6. 另行核对首页、文号、日期、数字条款、附件表格、末页等 22 项内容。命中分数按对应结果页精确匹配。PDF-Extract-Kit 将“2021年12月底前完成85%以上”合并到前页，页内不命中，但全文存在，不能描述为实际漏字。
7. 观察全部页的返回情况，重点核对物理页 1、3、12、27、28、30、33、34、35。没有逐字人工校对全部 35 页。**0.000% 仅表示抽样正文无归一化文字差异，不表示全文 100% 准确。**

人工基准：[henan-2021-18-reference.json](../maxkb-business/src/test/resources/ocr/henan-2021-18-reference.json)。离线评价：[evaluate_ocr.py](../maxkb-business/src/test/scripts/evaluate_ocr.py)。详细差异保存在结果目录的 `evaluation/*-diffs.json`。

## 人工核对证据

### 日期和末页

首页日期“2021年6月17日”与红色印章重叠；物理第 35 页有“河南省自然资源厅办公室”和“2021年6月17日印发”。

- HunyuanOCR：两个位置的日期均识别出来。
- PaddleOCR-VL-1.5：首页日期正确，末页为空。
- MinerU2.5-Pro、MinerU2.5、PDF-Extract-Kit：首页日期遗漏，末页内容未返回。
- DeepSeek-OCR：首页日期遗漏，末页只保留机关名称。
- Unlimited-OCR：首页日期遗漏；末页关键项均存在，但额外生成“The image contains no discernible text or characters...”等原文没有的英文解释。

### 第 27 页超宽表格

原表有 1—72 的连续列编号，所有结果均存在明显错误：

- MinerU2.5-Pro：出现 `colspan="228"`，并将“宗地图”识别成“表地图”等，生成大量空单元格。
- MinerU2.5：只保留极少量表头和单元格。
- PaddleOCR-VL-1.5：返回表格 HTML，但大量表头和列缺失。
- Unlimited-OCR：仅保留部分顶层分类，HTML 不完整。
- PDF-Extract-Kit：出现表格结构，但大部分列内容未能还原。
- DeepSeek-OCR：大量重复“宅基地情况”，出现“收入的人房数量”等错误。
- HunyuanOCR：缩小后返回文本，但出现“打证代理”，大量重复“开展工作户数（家）”。

### 普通附件表格

第 33 页分类表的正文项目多数能识别，但合并单元格关系需检查。例如 HunyuanOCR 将“土地”分类附近的省略行放进“房屋”分组。

第 34 页领取签字簿，原表为 7 列、表头加 5 个空白数据行。MinerU2.5-Pro、MinerU2.5、PaddleOCR-VL-1.5、PDF-Extract-Kit 输出 42 个单元格，行数一致；HunyuanOCR 输出 49 个单元格，多出一个空白行；Unlimited-OCR 将表头连为文本，没有还原单元格结构。

## DeepSeek 官方核验

已查阅 [官方模型列表文档](https://api-docs.deepseek.com/api/list-models/) 和 [DeepSeek-OCR 官方仓库](https://github.com/deepseek-ai/DeepSeek-OCR)。使用配置的 `DEEPSEEK_API_KEY`，通过新增的 `DeepSeekClient.getModels()` 实际查询 `https://api.deepseek.com/v1/models`，返回：

```json
["deepseek-flash", "deepseek-v4-pro"]
```

没有 `deepseek-ocr`。官方 OCR 仓库提供 vLLM / Transformers 自行部署方案，不能据此构造凭官方 API Key 使用的托管 OCR 端点。因此没有伪造同名接口，也没有用其他视觉模型替代后标成 DeepSeek-OCR。Gitee 的 DeepSeek-OCR 已实测，但不能当成“官方平台 OCR”的结果。

## 代码和故障处理

- 新增 [DocumentOcrBenchmarkTest.java](../maxkb-business/src/test/java/nexus/io/maxkb/ocr/DocumentOcrBenchmarkTest.java)：默认跳过付费网络调用，显式开启后提交、轮询、保存原始响应与结果，保存任务 ID 支持续跑。
- java-openai 增加 `GiteeClient.ocr(...)`、`GiteeOcrResponse`、可注入 HTTP 客户端的构造方法。官方示例返回 `text`，实测返回 `text_result`，现兼容两种形式。
- 增加 `DeepSeekClient.getModels()`，密钥使用 `EnvUtils.get`，未写入源码和日志。默认运行目录 ID 使用 `nexus.io.tio.utils.snowflake.SnowflakeIdUtils.id()`。
- maxkb-business 显式引用更新后的客户端；修复 SLF4J 与 Logback 不兼容导致的启动 `NoClassDefFoundError`。
- PDF-Extract-Kit 首次因并发任务数超限失败，待其他任务结束后重试成功，两次响应都保留。
- HunyuanOCR 首次通过异步 PDF 接口返回“暂不支持该接口”，按 [Gitee 官方文档](https://ai.gitee.com/docs/openapi/v1) 改为同步图片接口。字段映射修正后复用已保存的成功响应，未重新上传这些页面。
- Gitee OCR 适配层 3 项测试通过；maxkb-business 编译通过，普通测试运行跳过付費 OCR 用例。同步更新 java-openai 文档和 tio-boot 的 Gitee OCR 说明。

## 复跑

确认当前 java-openai 已安装到本地 Maven 仓库，且 `maxkb-business/secrets.txt` 中有密钥。从项目根目录运行：

```powershell
.\maxkb-business\src\test\scripts\run-ocr-benchmark.ps1 -Pdf '原始PDF的完整路径' -OutputDirectory 'target/ocr-benchmark/new-run'
```

用 `-Models 'PaddleOCR-VL-1.5'` 指定模型。同一输出目录复用已有任务与成功页面，PDF 哈希不同则拒绝复用。明确失败的异步任务应使用新输出目录进行有意重试，不要删除成功记录后盲目重跑。被明确拒绝尺寸的 HunyuanOCR 页会在恢复运行时缩小后重试。

离线复核不调用模型，也不需要密钥：

```powershell
python .\maxkb-business\src\test\scripts\extract_ocr_reference_text.py '原始PDF的完整路径' '.\tmp\ocr-source'
python .\maxkb-business\src\test\scripts\evaluate_ocr.py --runs '.\output\ocr-benchmark\henan-2021-18\henan-2021-18' '.\output\ocr-benchmark\henan-2021-18\henan-2021-18-pdf-retry' '.\output\ocr-benchmark\henan-2021-18\henan-2021-18-hunyuan' --reference '.\maxkb-business\src\test\resources\ocr\henan-2021-18-reference.json' --source-text '.\tmp\ocr-source' --output '.\tmp\ocr-evaluation'
```

原生文本提取需要 `pypdf`，评价脚本只使用 Python 标准库。

## 原始结果

结果另存于 [output/ocr-benchmark/henan-2021-18](../output/ocr-benchmark/henan-2021-18)，避免 Maven clean 删除唯一副本：

- `henan-2021-18`：首轮请求、任务响应、解析文本及 DeepSeek 官方模型列表。
- `henan-2021-18-pdf-retry`：PDF-Extract-Kit 的成功重试。
- `henan-2021-18-hunyuan`：HunyuanOCR 35 页文本、原始响应、尺寸拒绝记录。
- `evaluation`：完整评价 JSON、逐模型逐页文本、抽样差异。
- `reference`：人工关键项和原 PDF 文本层。

结果目录已加入 Git 忽略规则，其中包含文档内容和服务端签名图片链接，未发布或提交。评价不依赖图片链接继续有效。

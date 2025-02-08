# 智能知识库系统（java-maxkb）

## 功能演示

[!演示视频](https://www.bilibili.com/video/BV1yJU8YHEgg/?vd_source=69e3cff470444b21e8c322dddec00def)

## 前端和后端代码

[前端代码github](https://github.com/litongjava/MaxKB/tree/main/ui)
[后端代码gitee](https://github.com/litongjava/java-maxkb)

## 开发文档

[!开发文档](https://www.tio-boot.com/zh/63_knowlege_base/01.html)

## 项目简介

**java-maxkb** 是一个基于 Java 开发的智能知识库系统，利用先进的自然语言处理和向量检索技术，为用户提供高效、准确的问答服务。通过集成文档管理、片段向量化、语义搜索和大语言模型（如 GPT-4）等功能，系统能够理解用户的问题，并从知识库中检索最相关的内容生成回答。

## 功能特性

1. **数据库设计** [查看 01.html](https://www.tio-boot.com/zh/63_knowlege_base/01.html)  
   设计并实现项目所需的数据表结构与数据库方案，为后续的数据操作打下坚实基础。

2. **用户登录** [查看 02.html](https://www.tio-boot.com/zh/63_knowlege_base/02.html)  
   实现了安全可靠的用户认证系统，保护用户数据并限制未经授权的访问。

3. **模型管理** [查看 03.html](https://www.tio-boot.com/zh/63_knowlege_base/03.html)  
   支持针对不同平台的模型（如 OpenAI、Google Gemini、Claude）进行管理与配置。

4. **知识库管理** [查看 04.html](https://www.tio-boot.com/zh/63_knowlege_base/04.html)  
   提供创建、更新及删除知识库的功能，方便用户维护与管理文档内容。

5. **文档拆分** [查看 05.html](https://www.tio-boot.com/zh/63_knowlege_base/05.html)  
   可将文档拆分为多个片段，便于后续向量化和检索操作。

6. **片段向量** [查看 06.html](https://www.tio-boot.com/zh/63_knowlege_base/06.html)  
   将文本片段进行向量化处理，以便进行语义相似度计算及高效检索。

7. **命中率测试** [查看 07.html](https://www.tio-boot.com/zh/63_knowlege_base/07.html)  
   通过语义相似度和 Top-N 算法，检索并返回与用户问题最相关的文档片段，用于评估检索的准确性。

8. **文档管理** [查看 08.html](https://www.tio-boot.com/zh/63_knowlege_base/08.html)  
   提供上传和管理文档的功能，上传后可自动拆分为片段便于进一步处理。

9. **片段管理** [查看 09.html](https://www.tio-boot.com/zh/63_knowlege_base/09.html)  
   允许对已拆分的片段进行增、删、改、查等操作，确保内容更新灵活可控。

10. **问题管理** [查看 10.html](https://www.tio-boot.com/zh/63_knowlege_base/10.html)  
    为片段指定相关问题，以提升检索时的准确性与关联度。

11. **应用管理** [查看 11.html](https://www.tio-boot.com/zh/63_knowlege_base/11.html)  
    提供创建和配置应用（智能体）的功能，并可关联指定模型和知识库。

12. **向量检索** [查看 12.html](https://www.tio-boot.com/zh/63_knowlege_base/12.html)  
    基于语义相似度，在知识库中高效检索与用户问题最匹配的片段。

13. **推理问答调试** [查看 13.html](https://www.tio-boot.com/zh/63_knowlege_base/13.html)  
    提供检索与问答性能的评估工具，帮助开发者进行系统优化与调试。

14. **对话问答** [查看 14.html](https://www.tio-boot.com/zh/63_knowlege_base/14.html)  
    为用户提供友好的人机交互界面，结合检索到的片段与用户问题实时生成回答。

15. **统计分析** [查看 15.html](https://www.tio-boot.com/zh/63_knowlege_base/15.html)  
    对用户的提问与系统回答进行数据化分析，并以可视化图表的形式呈现系统使用情况。

16. **用户管理** [查看 16.html](https://www.tio-boot.com/zh/63_knowlege_base/16.html)  
    提供多用户管理功能，包括用户的增删改查及权限控制。

17. **API 管理** [查看 17.html](https://www.tio-boot.com/zh/63_knowlege_base/17.html)  
    对外提供标准化 API，便于外部系统集成和调用本系统的功能。

18. **存储文件到 S3** [查看 18.html](https://www.tio-boot.com/zh/63_knowlege_base/18.html)  
    将用户上传的文件存储至 S3 等对象存储平台，提升文件管理的灵活性与可扩展性。

19. **文档解析优化** [查看 19.html](https://www.tio-boot.com/zh/63_knowlege_base/19.html)  
    介绍与对比常见的文档解析方案，并提供提升文档解析速度和准确性的优化建议。

20. **片段汇总** [查看 20.html](https://www.tio-boot.com/zh/63_knowlege_base/20.html)  
    对片段内容进行汇总，以提升总结类问题的查询与回答效率。

21. **文档多分块与检索** [查看 21.html](https://www.tio-boot.com/zh/63_knowlege_base/21.html)  
    将片段进一步拆分为句子并进行向量检索，提升检索的准确度与灵活度。

22. **多文档支持** [查看 22.html](https://www.tio-boot.com/zh/63_knowlege_base/22.html)  
    兼容多种文档格式，包括 `.doc`, `.docx`, `.xls`, `.xlsx`, `.ppt`, `.pptx` 等。

23. **对话日志** [查看 23.html](https://www.tio-boot.com/zh/63_knowlege_base/23.html)  
    记录并展示对话日志，用于后续分析和问题回溯。

24. **检索性能优化** [查看 24.html](https://www.tio-boot.com/zh/63_knowlege_base/24.html)  
    提供整库扫描和分区检索等多种方式，进一步提高检索速度和效率。

25. **Milvus** [查看 25.html](https://www.tio-boot.com/zh/63_knowlege_base/25.html)  
    将向量数据库切换至 Milvus，以在大规模向量检索场景中获得更佳的性能与可扩展性。

26. **文档解析方案和费用对比** [查看 26.html](https://www.tio-boot.com/zh/63_knowlege_base/26.html)  
    对比不同文档解析方案在成本、速度、稳定性等方面的差异，为用户提供更加经济高效的选择。

27. **爬取网页数据** [查看 27.html](https://www.tio-boot.com/zh/63_knowlege_base/27.html)  
    支持从网页中抓取所需内容，后续处理流程与本地文档一致：分段、向量化、存储与检索。

## 项目地址

后端
- GitHub：[https://github.com/litongjava/java-maxkb](https://github.com/litongjava/java-maxkb)
- Gitee：[https://gitee.com/ppnt/java-maxkb](https://gitee.com/ppnt/java-maxkb)

前端
- GitHub：[https://github.com/litongjava/MaxKB/tree/main/ui](https://github.com/litongjava/MaxKB/tree/main/ui)


## 安装与使用指南

### 环境要求

- **操作系统**：Windows、Linux 或 macOS
- **Java 开发工具包（JDK）**：版本 8 或更高
- **Maven**：用于项目构建和依赖管理
- **PostgreSQL**：用于数据库存储

### 安装步骤

1. **克隆项目代码**

   ```bash
   # 从 GitHub 克隆
   git clone https://github.com/litongjava/java-maxkb.git
   cd java-maxkb

   # 或从 Gitee 克隆
   git clone https://gitee.com/ppnt/java-maxkb.git
   cd java-maxkb
   ```

2. **配置数据库**

   - 安装并启动 PostgreSQL 数据库。
   - 创建项目所需的数据库和用户。
   - 在 `src/main/resources/application.properties` 中配置数据库连接信息。

3. **构建项目**

   ```bash
   mvn clean install
   ```

4. **运行项目**

   ```bash
   java -jar target/java-maxkb.jar
   ```

5. **访问系统**

   启动前端,打开浏览器，访问 `http://localhost:3000`，即可进入系统登录界面。

## 贡献指南

我们欢迎所有对本项目感兴趣的开发者贡献代码和建议。请按照以下步骤参与贡献：

1. **Fork 仓库**

   点击页面右上角的 “Fork” 按钮，将项目仓库复制到您的账户下。

2. **克隆仓库到本地**

   ```bash
   # 从 GitHub 克隆
   git clone https://github.com/yourusername/java-maxkb.git
   ```

3. **创建新分支**

   ```bash
   git checkout -b feature/your-feature-name
   ```

4. **提交更改**

   ```bash
   git commit -am '添加新功能: your-feature-name'
   ```

5. **推送到远程仓库**

   ```bash
   git push origin feature/your-feature-name
   ```

6. **创建 Pull Request**

   在 GitHub 上提交 Pull Request，描述您的更改内容和目的。

## 许可证信息

本项目采用 MIT 许可证，详细信息请参阅 [LICENSE](LICENSE) 文件。
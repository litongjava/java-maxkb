# 智能知识库系统（java-maxkb）

## 项目简介

**java-maxkb** 是一个基于 Java 开发的智能知识库系统，利用先进的自然语言处理和向量检索技术，为用户提供高效、准确的问答服务。通过集成文档管理、片段向量化、语义搜索和大语言模型（如 GPT-4）等功能，系统能够理解用户的问题，并从知识库中检索最相关的内容生成回答。

## 功能特性

- **用户登录**：安全的用户认证系统，保护数据并限制未经授权的访问。
- **知识库管理**：支持知识库的创建、更新和删除，方便用户维护和管理内容。
- **文档管理**：上传并管理知识库中的文档，支持自动将文档拆分为片段。
- **片段管理**：管理拆分后的文档片段，支持片段的增删改查操作。
- **片段向量化**：将文本片段向量化，便于语义相似度计算。
- **向量检索**：基于语义相似度，从知识库中检索相关的片段。
- **问题管理**：为片段指定相关问题，提高检索的准确性。
- **应用管理**：创建和配置应用（智能体），关联特定的模型和知识库。
- **实时问答**：基于检索的片段和用户问题，实时生成回答。
- **命中率测试**：评估系统的检索和问答性能，优化系统效果。

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
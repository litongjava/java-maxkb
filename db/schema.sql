-- Java MossKB 数据库结构
--
-- 幂等脚本：既能在空库上初始化，也能在已有库上重复执行。
-- 只创建缺失的表、列和索引，不删除任何数据；需要清空重建时先执行 reset.sql。
--
-- 用法（Windows 与 Linux 相同）：
--   psql -h 127.0.0.1 -p 5432 -U <数据库用户> -d <数据库名> -v ON_ERROR_STOP=1 -f schema.sql
--   psql -h 127.0.0.1 -p 5432 -U <数据库用户> -d <数据库名> -v ON_ERROR_STOP=1 -f seed.sql
--
-- 结构脚本与种子脚本分开：本文件只建结构，seed.sql 写初始数据。

BEGIN;

-- 扩展 ---------------------------------------------------------------------
-- vector 提供向量列与余弦距离算子，pg_trgm 提供关键词检索用的文本相似度。
-- 扩展以数据库为单位注册，需要数据库服务端已经安装扩展文件。
CREATE EXTENSION IF NOT EXISTS vector;
CREATE EXTENSION IF NOT EXISTS pg_trgm;

-- 系统设置 -----------------------------------------------------------------
-- 按类型保存单条系统级配置：type=0 是邮箱设置，type=1 是 RSA 私钥与公钥。
-- 这张表沿用上游项目的命名约定，不带 moss_kb_ 前缀。
CREATE TABLE IF NOT EXISTS "public"."system_setting" (
  "type" INTEGER NOT NULL PRIMARY KEY,
  "meta" JSONB NOT NULL DEFAULT '{}'::jsonb,
  "create_time" TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "update_time" TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 用户、令牌与密钥 ---------------------------------------------------------
-- 平台用户。token_version 用于让改密、停用后签发的旧令牌立即失效。
CREATE TABLE IF NOT EXISTS "public"."moss_kb_user" (
  "id" BIGINT NOT NULL PRIMARY KEY,
  "email" VARCHAR,
  "phone" VARCHAR NOT NULL,
  "nick_name" VARCHAR NOT NULL,
  "username" VARCHAR NOT NULL,
  "password" VARCHAR NOT NULL,
  "role" VARCHAR NOT NULL,
  "is_active" BOOLEAN NOT NULL,
  "source" VARCHAR NOT NULL,
  "remark" VARCHAR(256),
  "creator" VARCHAR(64) DEFAULT '',
  "create_time" TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "updater" VARCHAR(64) DEFAULT '',
  "update_time" TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "deleted" SMALLINT DEFAULT 0,
  "tenant_id" BIGINT NOT NULL DEFAULT 0,
  "token_version" BIGINT NOT NULL DEFAULT 0,
  -- 界面语言，取 zh-CN / zh-Hant / en-US，为空表示跟随浏览器语言（与公开分享链接的 language 同义）。
  "language" VARCHAR(16)
);

CREATE INDEX IF NOT EXISTS "user_email_index_like" ON "public"."moss_kb_user" USING btree (
  "email" varchar_pattern_ops
);
CREATE INDEX IF NOT EXISTS "user_username_index_like" ON "public"."moss_kb_user" USING btree (
  "username" varchar_pattern_ops
);

-- 登录令牌。
CREATE TABLE IF NOT EXISTS "public"."moss_kb_user_token" (
  "id" BIGINT NOT NULL PRIMARY KEY,
  "token" VARCHAR NOT NULL,
  "remark" VARCHAR(256),
  "creator" VARCHAR(64) DEFAULT '',
  "create_time" TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "updater" VARCHAR(64) DEFAULT '',
  "update_time" TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "deleted" SMALLINT DEFAULT 0,
  "tenant_id" BIGINT NOT NULL DEFAULT 0
);

-- 账号级开放接口密钥。只保存明文前缀加随机串，删除为逻辑删除，停用后立即失效。
CREATE TABLE IF NOT EXISTS "public"."moss_kb_user_api_key" (
  "id" BIGINT NOT NULL PRIMARY KEY,
  "secret_key" VARCHAR(128) NOT NULL,
  "user_id" BIGINT NOT NULL,
  "is_active" BOOLEAN NOT NULL DEFAULT TRUE,
  "allow_cross_domain" BOOLEAN NOT NULL DEFAULT FALSE,
  "cross_domain_list" VARCHAR[] NOT NULL DEFAULT '{}',
  "remark" VARCHAR(256),
  "creator" VARCHAR(64) DEFAULT '',
  "create_time" TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "updater" VARCHAR(64) DEFAULT '',
  "update_time" TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "deleted" SMALLINT DEFAULT 0,
  "tenant_id" BIGINT NOT NULL DEFAULT 0
);

CREATE UNIQUE INDEX IF NOT EXISTS "moss_kb_user_api_key_secret_key"
  ON "public"."moss_kb_user_api_key" ("secret_key");
CREATE INDEX IF NOT EXISTS "moss_kb_user_api_key_user_id"
  ON "public"."moss_kb_user_api_key" ("user_id");

-- 模型与模型目录 -----------------------------------------------------------
-- 平台接入信息：接口地址、协议、能力与表单定义。模型目录以平台为单位挂在下面。
CREATE TABLE IF NOT EXISTS "public"."moss_kb_model_provider" (
  "provider" VARCHAR(128) NOT NULL PRIMARY KEY,
  "name" VARCHAR(200) NOT NULL,
  "icon" TEXT NOT NULL DEFAULT '',
  "api_base" TEXT NOT NULL DEFAULT '',
  "protocol" VARCHAR(40) NOT NULL DEFAULT 'OPENAI_COMPATIBLE',
  "model_types" JSONB NOT NULL,
  "credential_form" JSONB NOT NULL,
  "params_form" JSONB NOT NULL,
  "enabled" BOOLEAN NOT NULL DEFAULT TRUE,
  "sort_order" INT NOT NULL DEFAULT 0,
  "updated_at" TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

-- 平台候选模型目录：管理员选择或同步得到的模型 ID。目录不代表模型一定可用。
CREATE TABLE IF NOT EXISTS "public"."moss_kb_model_catalog" (
  "provider" VARCHAR(128) NOT NULL,
  "model_type" VARCHAR(30) NOT NULL,
  "model_id" VARCHAR(256) NOT NULL,
  "description" TEXT,
  "source" TEXT NOT NULL DEFAULT 'seed',
  "enabled" BOOLEAN NOT NULL DEFAULT TRUE,
  "updated_at" TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
  PRIMARY KEY ("provider", "model_type", "model_id")
);

-- 用户创建的模型实例：平台、模型 ID、凭据和参数表单。
CREATE TABLE IF NOT EXISTS "public"."moss_kb_model" (
  "id" BIGINT NOT NULL PRIMARY KEY,
  "name" VARCHAR NOT NULL,
  "model_type" VARCHAR NOT NULL,
  "model_name" VARCHAR NOT NULL,
  "provider" VARCHAR NOT NULL,
  "credential" VARCHAR NOT NULL,
  "user_id" BIGINT NOT NULL,
  "meta" JSONB,
  "status" VARCHAR,
  "permission_type" VARCHAR NOT NULL,
  "remark" VARCHAR(256),
  "creator" VARCHAR(64) DEFAULT '',
  "create_time" TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "updater" VARCHAR(64) DEFAULT '',
  "update_time" TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "deleted" SMALLINT DEFAULT 0,
  "tenant_id" BIGINT NOT NULL DEFAULT 0,
  "model_params_form" JSONB NOT NULL DEFAULT '[]'
);

CREATE INDEX IF NOT EXISTS "idx_user_id" ON "public"."moss_kb_model" USING btree ("user_id");

-- 应用与访问凭据 -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS "public"."moss_kb_application" (
  "id" BIGINT PRIMARY KEY,
  "code" INT,
  "course_name" VARCHAR,
  "owner_name" VARCHAR,
  "state" INT,
  "name" VARCHAR NOT NULL,
  "desc" VARCHAR NOT NULL,
  "prompt" VARCHAR,
  "prologue" VARCHAR NOT NULL,
  "dialogue_number" INT NOT NULL,
  "dataset_setting" JSONB NOT NULL,
  "model_setting" JSONB NOT NULL,
  "problem_optimization" BOOLEAN NOT NULL,
  "model_id" BIGINT,
  "user_id" BIGINT NOT NULL,
  "icon" VARCHAR,
  "type" VARCHAR NOT NULL,
  "work_flow" JSONB,
  "show_source" BOOLEAN DEFAULT TRUE,
  "multiple_rounds_dialogue" BOOLEAN DEFAULT TRUE,
  "model_params_setting" JSONB NOT NULL,
  "stt_model_id" BIGINT,
  "stt_model_enable" BOOLEAN NOT NULL,
  "tts_model_id" BIGINT,
  "tts_model_enable" BOOLEAN NOT NULL,
  "tts_type" VARCHAR NOT NULL,
  "problem_optimization_prompt" VARCHAR,
  "tts_model_params_setting" JSONB,
  "clean_time" INT,
  "remark" VARCHAR(256),
  "creator" VARCHAR(64) DEFAULT '',
  "create_time" TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "updater" VARCHAR(64) DEFAULT '',
  "update_time" TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "deleted" SMALLINT DEFAULT 0,
  "tenant_id" BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS "application_model_id" ON "public"."moss_kb_application" USING btree ("model_id");
CREATE INDEX IF NOT EXISTS "application_stt_model_id" ON "public"."moss_kb_application" USING btree ("stt_model_id");
CREATE INDEX IF NOT EXISTS "application_tts_model_id" ON "public"."moss_kb_application" USING btree ("tts_model_id");
CREATE INDEX IF NOT EXISTS "application_user_id" ON "public"."moss_kb_application" USING btree ("user_id");

-- 公开分享链接。language 为空表示跟随访客浏览器语言。
CREATE TABLE IF NOT EXISTS "public"."moss_kb_application_access_token" (
  "application_id" BIGINT PRIMARY KEY,
  "access_token" BIGINT NOT NULL,
  "is_active" BOOLEAN NOT NULL,
  "access_num" INT NOT NULL,
  "white_active" BOOLEAN NOT NULL,
  "white_list" VARCHAR[] NOT NULL,
  "show_source" BOOLEAN NOT NULL,
  "language" VARCHAR(16),
  "remark" VARCHAR(256),
  "creator" VARCHAR(64) DEFAULT '',
  "create_time" TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "updater" VARCHAR(64) DEFAULT '',
  "update_time" TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "deleted" SMALLINT DEFAULT 0,
  "tenant_id" BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS "moss_kb_application_access_token_access_token" ON "public"."moss_kb_application_access_token" USING btree ("access_token");

COMMENT ON COLUMN "public"."moss_kb_application_access_token"."language" IS '公开分享链接的对话语言，为空表示跟随浏览器语言';

-- 应用级开放接口密钥。
CREATE TABLE IF NOT EXISTS "public"."moss_kb_application_api_key" (
  "id" BIGINT NOT NULL PRIMARY KEY,
  "secret_key" VARCHAR(128) NOT NULL,
  "application_id" BIGINT NOT NULL,
  "user_id" BIGINT NOT NULL,
  "is_active" BOOLEAN NOT NULL DEFAULT TRUE,
  "allow_cross_domain" BOOLEAN NOT NULL DEFAULT FALSE,
  "cross_domain_list" VARCHAR[] NOT NULL DEFAULT '{}',
  "remark" VARCHAR(256),
  "creator" VARCHAR(64) DEFAULT '',
  "create_time" TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "updater" VARCHAR(64) DEFAULT '',
  "update_time" TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "deleted" SMALLINT DEFAULT 0,
  "tenant_id" BIGINT NOT NULL DEFAULT 0
);

CREATE UNIQUE INDEX IF NOT EXISTS "moss_kb_application_api_key_secret_key"
  ON "public"."moss_kb_application_api_key" ("secret_key");
CREATE INDEX IF NOT EXISTS "moss_kb_application_api_key_application_id"
  ON "public"."moss_kb_application_api_key" ("application_id");

-- 文件、知识库与文档 -------------------------------------------------------
CREATE TABLE IF NOT EXISTS "public"."moss_kb_file" (
  "id" BIGINT PRIMARY KEY,
  "md5" VARCHAR(32) NOT NULL,
  "filename" TEXT NOT NULL,
  "file_size" BIGINT NOT NULL,
  "user_id" BIGINT,
  "platform" VARCHAR(64) NOT NULL,
  "region_name" VARCHAR(32),
  "bucket_name" VARCHAR(64) NOT NULL,
  "file_id" VARCHAR(64),
  "target_name" VARCHAR(255) NOT NULL,
  "tags" JSON,
  "creator" VARCHAR(64) DEFAULT '',
  "create_time" TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "updater" VARCHAR(64) DEFAULT '',
  "update_time" TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "deleted" SMALLINT NOT NULL DEFAULT 0,
  "tenant_id" BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS "public"."moss_kb_dataset" (
  "id" BIGINT PRIMARY KEY,
  "name" VARCHAR NOT NULL,
  "desc" VARCHAR,
  "type" VARCHAR,
  "embedding_mode_id" BIGINT,
  "llm_mode_id" BIGINT,
  "meta" JSONB,
  "user_id" BIGINT,
  "remark" VARCHAR(256),
  "creator" VARCHAR(64) DEFAULT '',
  "create_time" TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "updater" VARCHAR(64) DEFAULT '',
  "update_time" TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "deleted" SMALLINT DEFAULT 0,
  "tenant_id" BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS "public"."moss_kb_application_dataset_mapping" (
  "id" BIGINT PRIMARY KEY,
  "application_id" BIGINT NOT NULL,
  "dataset_id" BIGINT NOT NULL
);

CREATE TABLE IF NOT EXISTS "public"."moss_kb_document" (
  "id" BIGINT NOT NULL PRIMARY KEY,
  "file_id" BIGINT,
  "user_id" BIGINT,
  "title" VARCHAR,
  "name" VARCHAR NOT NULL,
  "type" VARCHAR NOT NULL,
  "url" VARCHAR,
  "content" TEXT,
  "char_length" INT,
  "status" VARCHAR,
  "is_active" BOOLEAN,
  "meta" JSONB,
  "dataset_id" BIGINT NOT NULL,
  "hit_handling_method" VARCHAR,
  "directly_return_similarity" FLOAT8,
  "paragraph_count" INT,
  "files" JSON,
  "creator" VARCHAR(64) DEFAULT '',
  "create_time" TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "updater" VARCHAR(64) DEFAULT '',
  "update_time" TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "deleted" SMALLINT NOT NULL DEFAULT 0,
  "tenant_id" BIGINT NOT NULL DEFAULT 0
);

-- 解析任务进度。
CREATE TABLE IF NOT EXISTS "public"."moss_kb_task" (
  "id" BIGINT NOT NULL PRIMARY KEY,
  "file_id" BIGINT,
  "file_name" VARCHAR NOT NULL,
  "file_size" BIGINT,
  "dataset_id" BIGINT NOT NULL,
  "document_id" BIGINT,
  "progress" SMALLINT DEFAULT 0,
  "status" VARCHAR NOT NULL,
  "creator" VARCHAR(64) DEFAULT '',
  "create_time" TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "updater" VARCHAR(64) DEFAULT '',
  "update_time" TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "deleted" SMALLINT NOT NULL DEFAULT 0,
  "tenant_id" BIGINT NOT NULL DEFAULT 0
);

-- 分段预览任务：上传后立刻返回 task_id，解析在后台线程里按页推进，
-- 前端轮询这个表拿到进度或最终分段。result 直接存前端要用的分段列表。
CREATE TABLE IF NOT EXISTS "public"."moss_kb_document_split_task" (
  "id" BIGINT NOT NULL PRIMARY KEY,
  "user_id" BIGINT,
  "file_id" BIGINT,
  "file_name" VARCHAR NOT NULL,
  "file_size" BIGINT,
  "status" VARCHAR(16) NOT NULL DEFAULT 'running',
  "progress" SMALLINT NOT NULL DEFAULT 0,
  "total" INT NOT NULL DEFAULT 0,
  "result" JSONB,
  "error_message" TEXT,
  "creator" VARCHAR(64) DEFAULT '',
  "create_time" TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "updater" VARCHAR(64) DEFAULT '',
  "update_time" TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "deleted" SMALLINT NOT NULL DEFAULT 0,
  "tenant_id" BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS "moss_kb_document_split_task_user_id" ON "public"."moss_kb_document_split_task" USING btree ("user_id");

-- 分段与向量。embedding 与 title_embedding 使用知识库选定的同一个向量空间。
CREATE TABLE IF NOT EXISTS "public"."moss_kb_paragraph" (
  "id" BIGINT PRIMARY KEY,
  "source_id" BIGINT,
  "source_type" VARCHAR,
  "title" VARCHAR,
  "content" VARCHAR NOT NULL,
  "md5" VARCHAR NOT NULL,
  "status" VARCHAR,
  "hit_num" INT,
  "is_active" BOOLEAN,
  "dataset_id" BIGINT NOT NULL,
  "document_id" BIGINT NOT NULL,
  "embedding" VECTOR,
  "title_embedding" VECTOR,
  "meta" JSONB,
  "search_vector" TSVECTOR,
  "creator" VARCHAR(64) DEFAULT '',
  "create_time" TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "updater" VARCHAR(64) DEFAULT '',
  "update_time" TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "deleted" SMALLINT DEFAULT 0,
  "tenant_id" BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS "public"."moss_kb_sentence" (
  "id" BIGINT PRIMARY KEY,
  "type" INT,
  "content" VARCHAR NOT NULL,
  "md5" VARCHAR NOT NULL,
  "hit_num" INT NOT NULL,
  "dataset_id" BIGINT NOT NULL,
  "document_id" BIGINT NOT NULL,
  "paragraph_id" BIGINT NOT NULL,
  "embedding" VECTOR,
  "meta" JSONB,
  "search_vector" TSVECTOR,
  "creator" VARCHAR(64) DEFAULT '',
  "create_time" TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "updater" VARCHAR(64) DEFAULT '',
  "update_time" TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "deleted" SMALLINT DEFAULT 0,
  "tenant_id" BIGINT NOT NULL DEFAULT 0
);

-- 问题库与问题分段映射。
CREATE TABLE IF NOT EXISTS "public"."moss_kb_problem" (
  "id" BIGINT PRIMARY KEY,
  "content" VARCHAR NOT NULL,
  "hit_num" INT NOT NULL,
  "dataset_id" BIGINT NOT NULL,
  "creator" VARCHAR(64) DEFAULT '',
  "create_time" TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "updater" VARCHAR(64) DEFAULT '',
  "update_time" TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "deleted" SMALLINT DEFAULT 0,
  "tenant_id" BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS "moss_kb_problem_dataset_id" ON "public"."moss_kb_problem" USING btree ("dataset_id");

CREATE TABLE IF NOT EXISTS "public"."moss_kb_problem_paragraph_mapping" (
  "id" BIGINT PRIMARY KEY,
  "dataset_id" BIGINT NOT NULL,
  "document_id" BIGINT NOT NULL,
  "paragraph_id" BIGINT NOT NULL,
  "problem_id" BIGINT NOT NULL,
  "creator" VARCHAR(64) DEFAULT '',
  "create_time" TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "updater" VARCHAR(64) DEFAULT '',
  "update_time" TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "deleted" SMALLINT DEFAULT 0,
  "tenant_id" BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS "moss_kb_problem_paragraph_mapping_dataset_id"   ON "public"."moss_kb_problem_paragraph_mapping" USING btree ("dataset_id");
CREATE INDEX IF NOT EXISTS "moss_kb_problem_paragraph_mapping_document_id"  ON "public"."moss_kb_problem_paragraph_mapping" USING btree ("document_id");
CREATE INDEX IF NOT EXISTS "moss_kb_problem_paragraph_mapping_paragraph_id" ON "public"."moss_kb_problem_paragraph_mapping" USING btree ("paragraph_id");
CREATE INDEX IF NOT EXISTS "moss_kb_problem_paragraph_mapping_problem_id"   ON "public"."moss_kb_problem_paragraph_mapping" USING btree ("problem_id");

-- 缓存 ---------------------------------------------------------------------
-- 向量缓存：m 记录模型标识和维度，md5 是内容摘要，避免跨模型复用向量。
CREATE TABLE IF NOT EXISTS "public"."moss_kb_embedding_cache" (
  "id" BIGINT PRIMARY KEY,
  "t" TEXT,
  "m" VARCHAR,
  "v" VECTOR,
  "md5" VARCHAR
);

CREATE INDEX IF NOT EXISTS "idx_moss_kb_embedding_cache_md5" ON "public"."moss_kb_embedding_cache" USING btree ("md5");
CREATE INDEX IF NOT EXISTS "idx_moss_kb_embedding_cache_md5_m" ON "public"."moss_kb_embedding_cache" USING btree ("md5", "m");

CREATE TABLE IF NOT EXISTS "public"."moss_kb_document_markdown_cache" (
  "id" VARCHAR PRIMARY KEY,
  "target" VARCHAR,
  "content" TEXT
);

CREATE TABLE IF NOT EXISTS "public"."moss_kb_document_markdown_page_cache" (
  "id" VARCHAR PRIMARY KEY,
  "target" VARCHAR,
  "content" TEXT,
  "elapsed" BIGINT,
  "model" VARCHAR,
  "system_fingerprint" VARCHAR,
  "completion_tokens" INT,
  "prompt_tokens" INT,
  "total_tokens" INT
);

CREATE TABLE IF NOT EXISTS "public"."moss_kb_paragraph_summary_cache" (
  "id" BIGINT PRIMARY KEY,
  "md5" VARCHAR,
  "src" TEXT,
  "content" TEXT,
  "elapsed" BIGINT,
  "model" VARCHAR,
  "system_fingerprint" VARCHAR,
  "completion_tokens" INT,
  "prompt_tokens" INT,
  "total_tokens" INT
);

-- 会话与多轮上下文 ---------------------------------------------------------
CREATE TABLE IF NOT EXISTS "public"."moss_kb_application_chat" (
  "id" BIGINT PRIMARY KEY,
  "abstract" VARCHAR,
  "application_id" BIGINT NOT NULL,
  "client_id" BIGINT,
  "chat_type" INT NOT NULL,
  "is_deleted" BOOLEAN NOT NULL DEFAULT FALSE,
  "creator" VARCHAR(64) DEFAULT '',
  "create_time" TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "updater" VARCHAR(64) DEFAULT '',
  "update_time" TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "deleted" SMALLINT DEFAULT 0,
  "tenant_id" BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS "moss_kb_application_chat_application_id" ON "public"."moss_kb_application_chat" USING btree ("application_id");

CREATE TABLE IF NOT EXISTS "public"."moss_kb_application_chat_record" (
  "id" BIGINT PRIMARY KEY,
  "vote_status" VARCHAR NOT NULL DEFAULT '-1',
  "problem_text" VARCHAR NOT NULL,
  "answer_text" VARCHAR,
  "message_tokens" INT NOT NULL,
  "answer_tokens" INT,
  "const" INT NOT NULL DEFAULT 0,
  "details" JSONB,
  "improve_paragraph_id_list" BIGINT[],
  "run_time" FLOAT8,
  "index" INT,
  "chat_id" BIGINT NOT NULL,
  "creator" VARCHAR(64) DEFAULT '',
  "create_time" TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "updater" VARCHAR(64) DEFAULT '',
  "update_time" TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "deleted" SMALLINT DEFAULT 0,
  "tenant_id" BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS "moss_kb_application_chat_record_chat_id" ON "public"."moss_kb_application_chat_record" USING btree ("chat_id");

-- 会话持久摘要：只有历史 token 超出预算时才压缩，压缩水位记录在 through_record_id。
CREATE TABLE IF NOT EXISTS "public"."moss_kb_chat_context" (
  "chat_id" BIGINT NOT NULL PRIMARY KEY,
  "summary" TEXT NOT NULL DEFAULT '',
  "through_record_id" BIGINT NOT NULL DEFAULT 0,
  "compacted_rounds" INT NOT NULL DEFAULT 0,
  "revision" INT NOT NULL DEFAULT 0,
  "update_time" TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS "idx_chat_record_context" ON "public"."moss_kb_application_chat_record" USING btree ("chat_id", "id") WHERE "answer_text" IS NOT NULL;

-- 调试配置按会话保存，避免多个调试窗口互相覆盖。
CREATE TABLE IF NOT EXISTS "public"."moss_kb_application_temp_setting" (
  "id" BIGINT PRIMARY KEY,
  "setting" JSONB
);

-- 公开访问的访客计数。
CREATE TABLE IF NOT EXISTS "public"."moss_kb_application_public_access_client" (
  "id" BIGINT PRIMARY KEY,
  "access_num" INT NOT NULL,
  "intraday_access_num" INT NOT NULL,
  "application_id" BIGINT NOT NULL,
  "client_id" BIGINT,
  "creator" VARCHAR(64) DEFAULT '',
  "create_time" TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "updater" VARCHAR(64) DEFAULT '',
  "update_time" TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "deleted" SMALLINT DEFAULT 0,
  "tenant_id" BIGINT NOT NULL DEFAULT 0
);

-- 函数库与文档转换 ---------------------------------------------------------
-- 函数库：内置模板与用户函数共用一张表，function_type 区分 PUBLIC 内置模板和 INTERNAL 用户函数。
CREATE TABLE IF NOT EXISTS "public"."moss_kb_function_lib" (
  "id" BIGINT PRIMARY KEY,
  "user_id" BIGINT NOT NULL,
  "name" VARCHAR(128) NOT NULL,
  "desc" TEXT NOT NULL DEFAULT '',
  "code" TEXT NOT NULL,
  "input_field_list" JSONB NOT NULL DEFAULT '[]',
  "init_field_list" JSONB NOT NULL DEFAULT '[]',
  "init_params" JSONB NOT NULL DEFAULT '{}',
  "permission_type" VARCHAR(16) NOT NULL DEFAULT 'PRIVATE',
  "is_active" BOOLEAN NOT NULL DEFAULT true,
  "deleted" BOOLEAN NOT NULL DEFAULT false,
  "create_time" TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT now(),
  "update_time" TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT now(),
  "function_type" VARCHAR(16) NOT NULL DEFAULT 'PUBLIC',
  "icon" VARCHAR(256) NOT NULL DEFAULT '/ui/favicon.ico',
  "template_id" BIGINT,
  CONSTRAINT moss_kb_function_lib_function_type_check CHECK (function_type IN ('PUBLIC', 'INTERNAL'))
);

CREATE INDEX IF NOT EXISTS "idx_function_owner" ON "public"."moss_kb_function_lib" USING btree ("user_id") WHERE "deleted" = false;
CREATE INDEX IF NOT EXISTS "idx_function_type" ON "public"."moss_kb_function_lib" USING btree ("function_type") WHERE "deleted" = false;

CREATE TABLE IF NOT EXISTS "public"."libre_office_converted_mapping" (
  "id" BIGINT NOT NULL PRIMARY KEY,
  "input_file_id" BIGINT,
  "input_md5" VARCHAR,
  "output_file_id" BIGINT,
  "output_md5" VARCHAR,
  "remark" VARCHAR(256),
  "creator" VARCHAR(64) DEFAULT '',
  "create_time" TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "updater" VARCHAR(64) DEFAULT '',
  "update_time" TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "deleted" SMALLINT DEFAULT 0,
  "tenant_id" BIGINT NOT NULL DEFAULT 0
);

-- 外键 ---------------------------------------------------------------------
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'moss_kb_chat_context_chat_id_fkey') THEN
    ALTER TABLE "public"."moss_kb_chat_context"
      ADD CONSTRAINT "moss_kb_chat_context_chat_id_fkey"
      FOREIGN KEY ("chat_id") REFERENCES "public"."moss_kb_application_chat"("id") ON DELETE CASCADE;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'moss_kb_model_catalog_provider_fkey') THEN
    ALTER TABLE "public"."moss_kb_model_catalog"
      ADD CONSTRAINT "moss_kb_model_catalog_provider_fkey"
      FOREIGN KEY ("provider") REFERENCES "public"."moss_kb_model_provider"("provider");
  END IF;
END $$;

-- 旧库补齐 -----------------------------------------------------------------
-- 下面语句让早期版本的数据库补齐后来新增的列。全新初始化时它们是空操作。
ALTER TABLE "public"."moss_kb_dataset" ADD COLUMN IF NOT EXISTS "llm_mode_id" BIGINT;
ALTER TABLE "public"."moss_kb_user" ADD COLUMN IF NOT EXISTS "token_version" BIGINT NOT NULL DEFAULT 0;
-- 界面语言，为空表示跟随浏览器语言：老账号升级后语言不变，直到用户自己切换一次。
ALTER TABLE "public"."moss_kb_user" ADD COLUMN IF NOT EXISTS "language" VARCHAR(16);
ALTER TABLE "public"."moss_kb_model" ADD COLUMN IF NOT EXISTS "model_params_form" JSONB NOT NULL DEFAULT '[]';
ALTER TABLE "public"."moss_kb_application_access_token" ADD COLUMN IF NOT EXISTS "language" VARCHAR(16);
ALTER TABLE "public"."moss_kb_function_lib" ADD COLUMN IF NOT EXISTS "function_type" VARCHAR(16) NOT NULL DEFAULT 'PUBLIC';
ALTER TABLE "public"."moss_kb_function_lib" ADD COLUMN IF NOT EXISTS "icon" VARCHAR(256) NOT NULL DEFAULT '/ui/favicon.ico';
ALTER TABLE "public"."moss_kb_function_lib" ADD COLUMN IF NOT EXISTS "template_id" BIGINT;

-- 文件名保存完整描述性名称，早期版本使用的是较短的类型。
ALTER TABLE "public"."moss_kb_file" ALTER COLUMN "filename" TYPE TEXT;

COMMIT;

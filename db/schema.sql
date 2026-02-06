DROP TABLE IF EXISTS "public"."max_kb_user";

CREATE TABLE "public"."max_kb_user" (
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
  "tenant_id" BIGINT NOT NULL DEFAULT 0
);

-- 创建索引
CREATE INDEX "user_email_index_like" ON "public"."max_kb_user" USING btree (
  "email" varchar_pattern_ops
);
CREATE INDEX "user_username_index_like" ON "public"."max_kb_user" USING btree (
  "username" varchar_pattern_ops
);

-- 插入初始记录（管理员账户）
INSERT INTO "public"."max_kb_user"
VALUES (
  1,
  '',
  '',
  '系统管理员',
  'admin',
  '608fd4367502762e76a77ee348c9f1c7',
  'ADMIN',
  TRUE,
  'LOCAL',
  '',
  '',
  CURRENT_TIMESTAMP,
  '',
  CURRENT_TIMESTAMP,
  0,
  0
);

-- 备注：默认密码 “Kimi@2024” 的 MD5（或其他加密形式）

DROP TABLE IF EXISTS "public"."max_kb_user_token";

CREATE TABLE "public"."max_kb_user_token" (
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

DROP TABLE IF EXISTS "public"."max_kb_model";

CREATE TABLE "public"."max_kb_model" (
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
  "tenant_id" BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX "idx_user_id" ON "public"."max_kb_model" USING btree ("user_id");


DROP TABLE IF EXISTS "public"."max_kb_application";

CREATE TABLE "public"."max_kb_application" (
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

CREATE INDEX "application_model_id" ON "public"."max_kb_application" USING btree ("model_id");
CREATE INDEX "application_stt_model_id" ON "public"."max_kb_application" USING btree ("stt_model_id");
CREATE INDEX "application_tts_model_id" ON "public"."max_kb_application" USING btree ("tts_model_id");
CREATE INDEX "application_user_id" ON "public"."max_kb_application" USING btree ("user_id");

DROP TABLE IF EXISTS "public"."max_kb_application_access_token";

CREATE TABLE "public"."max_kb_application_access_token" (
  "application_id" BIGINT PRIMARY KEY,
  "access_token" BIGINT NOT NULL,
  "is_active" BOOLEAN NOT NULL,
  "access_num" INT NOT NULL,
  "white_active" BOOLEAN NOT NULL,
  "white_list" VARCHAR[] NOT NULL,
  "show_source" BOOLEAN NOT NULL,
  "remark" VARCHAR(256),
  "creator" VARCHAR(64) DEFAULT '',
  "create_time" TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "updater" VARCHAR(64) DEFAULT '',
  "update_time" TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "deleted" SMALLINT DEFAULT 0,
  "tenant_id" BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX "max_kb_application_access_token_access_token" ON "public"."max_kb_application_access_token" USING btree ("access_token");

DROP TABLE IF EXISTS "public"."max_kb_task";

CREATE TABLE "public"."max_kb_task" (
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

DROP TABLE IF EXISTS "public"."max_kb_file";

CREATE TABLE "public"."max_kb_file" (
  "id" BIGINT PRIMARY KEY,
  "md5" VARCHAR(32) NOT NULL,
  "filename" VARCHAR(64) NOT NULL,
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

DROP TABLE IF EXISTS "public"."max_kb_dataset";

CREATE TABLE "public"."max_kb_dataset" (
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

DROP TABLE IF EXISTS "public"."max_kb_application_dataset_mapping";

CREATE TABLE "public"."max_kb_application_dataset_mapping" (
  "id" BIGINT PRIMARY KEY,
  "application_id" BIGINT NOT NULL,
  "dataset_id" BIGINT NOT NULL
);

DROP TABLE IF EXISTS "public"."max_kb_document";

CREATE TABLE "public"."max_kb_document" (
  "id" BIGINT NOT NULL PRIMARY KEY,
  "file_id" BIGINT,
  "user_id" BIGINT,
  "title" VARCHAR,
  "name" VARCHAR NOT NULL,
  "type" VARCHAR NOT NULL,
  "url" VARCHAR,
  "content" text,
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

DROP TABLE IF EXISTS "public"."max_kb_paragraph";

CREATE TABLE "public"."max_kb_paragraph" (
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
  "meta" JSONB,
  "search_vector" TSVECTOR,
  "creator" VARCHAR(64) DEFAULT '',
  "create_time" TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "updater" VARCHAR(64) DEFAULT '',
  "update_time" TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "deleted" SMALLINT DEFAULT 0,
  "tenant_id" BIGINT NOT NULL DEFAULT 0
);


DROP TABLE IF EXISTS "public"."max_kb_sentence";

CREATE TABLE "public"."max_kb_sentence" (
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


DROP TABLE IF EXISTS "public"."max_kb_problem";

CREATE TABLE "public"."max_kb_problem" (
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

CREATE INDEX "max_kb_problem_dataset_id" ON "public"."max_kb_problem" USING btree ("dataset_id");

DROP TABLE IF EXISTS "public"."max_kb_problem_paragraph_mapping";

CREATE TABLE "public"."max_kb_problem_paragraph_mapping" (
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

CREATE INDEX "max_kb_problem_paragraph_mapping_dataset_id"   ON "public"."max_kb_problem_paragraph_mapping" USING btree ("dataset_id");
CREATE INDEX "max_kb_problem_paragraph_mapping_document_id"  ON "public"."max_kb_problem_paragraph_mapping" USING btree ("document_id");
CREATE INDEX "max_kb_problem_paragraph_mapping_paragraph_id" ON "public"."max_kb_problem_paragraph_mapping" USING btree ("paragraph_id");
CREATE INDEX "max_kb_problem_paragraph_mapping_problem_id"   ON "public"."max_kb_problem_paragraph_mapping" USING btree ("problem_id");


DROP TABLE IF EXISTS "public"."max_kb_embedding_cache";

CREATE TABLE "public"."max_kb_embedding_cache" (
  "id" BIGINT PRIMARY KEY,
  "t" TEXT,
  "m" VARCHAR,
  "v" VECTOR,
  "md5" VARCHAR
);

CREATE INDEX "idx_max_kb_embedding_cache_md5" ON "public"."max_kb_embedding_cache" USING btree ("md5");
CREATE INDEX "idx_max_kb_embedding_cache_md5_m" ON "public"."max_kb_embedding_cache" USING btree ("md5", "m");

DROP TABLE IF EXISTS "public"."max_kb_document_markdown_cache";

CREATE TABLE "public"."max_kb_document_markdown_cache" (
  "id" VARCHAR PRIMARY KEY,
  "target" VARCHAR,
  "content" TEXT
);


DROP TABLE IF EXISTS "public"."max_kb_document_markdown_page_cache";

CREATE TABLE "public"."max_kb_document_markdown_page_cache" (
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


DROP TABLE IF EXISTS "public"."max_kb_application_chat";

CREATE TABLE "public"."max_kb_application_chat" (
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

CREATE INDEX "max_kb_application_chat_application_id" ON "public"."max_kb_application_chat" USING btree ("application_id");

DROP TABLE IF EXISTS "public"."max_kb_application_chat_record";

CREATE TABLE "public"."max_kb_application_chat_record" (
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

CREATE INDEX "max_kb_application_chat_record_chat_id" ON "public"."max_kb_application_chat_record" USING btree ("chat_id");

DROP TABLE IF EXISTS "public"."max_kb_application_temp_setting";

CREATE TABLE "public"."max_kb_application_temp_setting" (
  "id" BIGINT PRIMARY KEY,
  "setting" JSONB
);

DROP TABLE IF EXISTS "public"."max_kb_paragraph_summary_cache";

CREATE TABLE "public"."max_kb_paragraph_summary_cache" (
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


DROP TABLE IF EXISTS "public"."max_kb_application_public_access_client";

CREATE TABLE "public"."max_kb_application_public_access_client" (
  "id" BIGINT PRIMARY KEY,
  "access_num" INT NOT NULL,
  "intraday_access_num" INT NOT NULL,
  "application_id" BIGINT NOT NULL,
  "creator" VARCHAR(64) DEFAULT '',
  "create_time" TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "updater" VARCHAR(64) DEFAULT '',
  "update_time" TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "deleted" SMALLINT DEFAULT 0,
  "tenant_id" BIGINT NOT NULL DEFAULT 0
);

DROP TABLE IF EXISTS "public"."libre_office_converted_mapping";

CREATE TABLE "public"."libre_office_converted_mapping" (
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

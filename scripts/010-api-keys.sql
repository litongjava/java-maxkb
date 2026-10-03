-- API 密钥：应用级密钥用于第三方调用应用问答接口，账号级密钥用于调用平台开放接口。
-- 密钥只保存明文前缀加随机串，删除采用逻辑删除，停用后立即失效。
BEGIN;

CREATE TABLE IF NOT EXISTS max_kb_application_api_key (
  id BIGINT PRIMARY KEY,
  secret_key VARCHAR(128) NOT NULL,
  application_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  is_active BOOLEAN NOT NULL DEFAULT TRUE,
  allow_cross_domain BOOLEAN NOT NULL DEFAULT FALSE,
  cross_domain_list VARCHAR[] NOT NULL DEFAULT '{}',
  remark VARCHAR(256),
  creator VARCHAR(64) DEFAULT '',
  create_time TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updater VARCHAR(64) DEFAULT '',
  update_time TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted SMALLINT DEFAULT 0,
  tenant_id BIGINT NOT NULL DEFAULT 0
);

CREATE UNIQUE INDEX IF NOT EXISTS max_kb_application_api_key_secret_key
  ON max_kb_application_api_key (secret_key);
CREATE INDEX IF NOT EXISTS max_kb_application_api_key_application_id
  ON max_kb_application_api_key (application_id);

CREATE TABLE IF NOT EXISTS max_kb_user_api_key (
  id BIGINT PRIMARY KEY,
  secret_key VARCHAR(128) NOT NULL,
  user_id BIGINT NOT NULL,
  is_active BOOLEAN NOT NULL DEFAULT TRUE,
  allow_cross_domain BOOLEAN NOT NULL DEFAULT FALSE,
  cross_domain_list VARCHAR[] NOT NULL DEFAULT '{}',
  remark VARCHAR(256),
  creator VARCHAR(64) DEFAULT '',
  create_time TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updater VARCHAR(64) DEFAULT '',
  update_time TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted SMALLINT DEFAULT 0,
  tenant_id BIGINT NOT NULL DEFAULT 0
);

CREATE UNIQUE INDEX IF NOT EXISTS max_kb_user_api_key_secret_key
  ON max_kb_user_api_key (secret_key);
CREATE INDEX IF NOT EXISTS max_kb_user_api_key_user_id
  ON max_kb_user_api_key (user_id);

COMMIT;

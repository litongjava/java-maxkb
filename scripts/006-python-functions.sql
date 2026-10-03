BEGIN;
CREATE TABLE IF NOT EXISTS max_kb_function_lib (
  id BIGINT PRIMARY KEY, user_id BIGINT NOT NULL,
  name VARCHAR(128) NOT NULL, "desc" TEXT NOT NULL DEFAULT '', code TEXT NOT NULL,
  input_field_list JSONB NOT NULL DEFAULT '[]', init_field_list JSONB NOT NULL DEFAULT '[]',
  init_params JSONB NOT NULL DEFAULT '{}', permission_type VARCHAR(16) NOT NULL DEFAULT 'PRIVATE',
  is_active BOOLEAN NOT NULL DEFAULT true, deleted BOOLEAN NOT NULL DEFAULT false,
  create_time TIMESTAMP NOT NULL DEFAULT now(), update_time TIMESTAMP NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_function_owner ON max_kb_function_lib(user_id) WHERE deleted=false;
COMMIT;

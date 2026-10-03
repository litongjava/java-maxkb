BEGIN;
CREATE TABLE IF NOT EXISTS max_kb_chat_context (
  chat_id BIGINT PRIMARY KEY REFERENCES max_kb_application_chat(id) ON DELETE CASCADE,
  summary TEXT NOT NULL DEFAULT '',
  through_record_id BIGINT NOT NULL DEFAULT 0,
  compacted_rounds INTEGER NOT NULL DEFAULT 0,
  revision INTEGER NOT NULL DEFAULT 0,
  update_time TIMESTAMP NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_chat_record_context ON max_kb_application_chat_record(chat_id,id) WHERE answer_text IS NOT NULL;
COMMIT;

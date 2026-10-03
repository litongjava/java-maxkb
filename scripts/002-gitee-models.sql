BEGIN;
INSERT INTO max_kb_model(id,name,model_type,model_name,provider,credential,user_id,status,permission_type,meta)
VALUES (1001,'Gitee DeepSeek V4.1 Flash','LLM','deepseek-v4.1-flash','model_openai_provider','{}',1,'SUCCESS','PUBLIC','{}'),
(1002,'Gitee Qwen3 Embedding 8B','EMBEDDING','Qwen3-Embedding-8B','model_openai_provider','{}',1,'SUCCESS','PUBLIC','{}')
ON CONFLICT (id) DO NOTHING;
ALTER TABLE max_kb_dataset ADD COLUMN IF NOT EXISTS llm_mode_id bigint;
COMMIT;

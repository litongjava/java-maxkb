-- 公开分享链接的对话语言：分享访客打开 /ui/#/chat/{access_token} 后按该语言渲染界面。
-- 为空表示跟随访客浏览器语言，因此存量记录不需要回填。
BEGIN;

ALTER TABLE max_kb_application_access_token
  ADD COLUMN IF NOT EXISTS language VARCHAR(16);

COMMENT ON COLUMN max_kb_application_access_token.language IS '公开分享链接的对话语言，为空表示跟随浏览器语言';

COMMIT;

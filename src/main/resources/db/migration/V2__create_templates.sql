-- templates table for user project templates
CREATE TABLE IF NOT EXISTS templates (
  id BIGSERIAL PRIMARY KEY,
  user_preferences_id BIGINT NOT NULL REFERENCES user_preferences(id) ON DELETE CASCADE,
  name TEXT NOT NULL,
  description TEXT,
  repository_url TEXT NOT NULL UNIQUE,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_templates_user ON templates(user_preferences_id);

DROP TRIGGER IF EXISTS set_timestamp_templates ON templates;
CREATE TRIGGER set_timestamp_templates
BEFORE UPDATE ON templates
FOR EACH ROW EXECUTE FUNCTION trg_set_timestamp();

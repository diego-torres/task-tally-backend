-- Create extensions (safe if already present)
CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- user_preferences table
CREATE TABLE IF NOT EXISTS user_preferences (
  id               BIGSERIAL PRIMARY KEY,
  user_id          TEXT NOT NULL UNIQUE,
  ui               JSONB NOT NULL DEFAULT '{}'::jsonb,
  default_git_provider TEXT,
  version          INTEGER NOT NULL DEFAULT 0,
  created_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at       TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- credential_refs table
CREATE TABLE IF NOT EXISTS credential_refs (
  id                  BIGSERIAL PRIMARY KEY,
  user_preferences_id BIGINT NOT NULL REFERENCES user_preferences(id) ON DELETE CASCADE,
  name                TEXT NOT NULL,
  provider            TEXT NOT NULL,
  scope               TEXT NOT NULL,
  secret_ref          TEXT NOT NULL,
  created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  CONSTRAINT credential_refs_provider_chk CHECK (provider IN ('github')),
  CONSTRAINT credential_refs_scope_chk CHECK (scope IN ('read','write')),
  CONSTRAINT credential_refs_unique_name_per_user UNIQUE (user_preferences_id, name)
);

-- Helpful indexes
CREATE INDEX IF NOT EXISTS idx_user_prefs_user_id ON user_preferences (user_id);
CREATE INDEX IF NOT EXISTS idx_user_prefs_ui_gin ON user_preferences USING GIN (ui);
CREATE INDEX IF NOT EXISTS idx_cred_refs_provider ON credential_refs (provider);

-- Trigger to auto-update updated_at on user_preferences
CREATE OR REPLACE FUNCTION trg_set_timestamp()
RETURNS TRIGGER AS $$
BEGIN
  NEW.updated_at = NOW();
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS set_timestamp_user_preferences ON user_preferences;
CREATE TRIGGER set_timestamp_user_preferences
BEFORE UPDATE ON user_preferences
FOR EACH ROW EXECUTE FUNCTION trg_set_timestamp();

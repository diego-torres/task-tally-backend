-- Create extensions (safe if already present)
CREATE EXTENSION IF NOT EXISTS pgcrypto;
CREATE SCHEMA IF NOT EXISTS tasktally;

-- user_preferences table
CREATE TABLE IF NOT EXISTS tasktally.user_preferences (
  id               BIGSERIAL PRIMARY KEY,
  user_id          TEXT NOT NULL UNIQUE,
  ui               JSONB NOT NULL DEFAULT '{}'::jsonb,
  default_git_provider TEXT,
  version          INTEGER NOT NULL DEFAULT 0,
  created_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at       TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Comments for clarity
COMMENT ON TABLE tasktally.user_preferences IS 'Per-user non-secret settings and defaults (JSONB)';
COMMENT ON COLUMN tasktally.user_preferences.ui IS 'Flexible UI preferences and defaults, stored as JSONB';

-- credential_refs table
CREATE TABLE IF NOT EXISTS tasktally.credential_refs (
  id                  BIGSERIAL PRIMARY KEY,
  user_preferences_id BIGINT NOT NULL REFERENCES tasktally.user_preferences(id) ON DELETE CASCADE,
  name                TEXT NOT NULL,
  provider            TEXT NOT NULL,
  scope               TEXT NOT NULL,
  secret_ref          TEXT NOT NULL,
  known_hosts_ref     TEXT,
  passphrase_ref      TEXT,
  created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  CONSTRAINT credential_refs_provider_chk CHECK (provider IN ('github','ssh_key')),
  CONSTRAINT credential_refs_scope_chk CHECK (scope IN ('read','write')),
  CONSTRAINT credential_refs_unique_name_per_user UNIQUE (user_preferences_id, name)
);

COMMENT ON TABLE tasktally.credential_refs IS 'References to external secrets (e.g., k8s or vault) for Git access';

-- Helpful indexes
CREATE INDEX IF NOT EXISTS idx_user_prefs_user_id ON tasktally.user_preferences (user_id);
CREATE INDEX IF NOT EXISTS idx_user_prefs_ui_gin ON tasktally.user_preferences USING GIN (ui);
CREATE INDEX IF NOT EXISTS idx_cred_refs_provider ON tasktally.credential_refs (provider);

-- Lowercase guard-rails
CREATE OR REPLACE FUNCTION tasktally.enforce_lowercase_provider_scope()
RETURNS TRIGGER AS $$
BEGIN
  NEW.provider := lower(NEW.provider);
  NEW.scope := lower(NEW.scope);
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS lowercase_provider_scope ON tasktally.credential_refs;
CREATE TRIGGER lowercase_provider_scope
BEFORE INSERT OR UPDATE ON tasktally.credential_refs
FOR EACH ROW EXECUTE FUNCTION tasktally.enforce_lowercase_provider_scope();

CREATE OR REPLACE FUNCTION tasktally.trg_set_timestamp()
RETURNS TRIGGER AS $$
BEGIN
  NEW.updated_at = NOW();
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS set_timestamp_user_preferences ON tasktally.user_preferences;
CREATE TRIGGER set_timestamp_user_preferences
BEFORE UPDATE ON tasktally.user_preferences
FOR EACH ROW EXECUTE FUNCTION tasktally.trg_set_timestamp();

-- templates table for user project templates
CREATE TABLE IF NOT EXISTS tasktally.templates (
  id BIGSERIAL PRIMARY KEY,
  user_preferences_id BIGINT NOT NULL REFERENCES user_preferences(id) ON DELETE CASCADE,
  name TEXT NOT NULL,
  description TEXT,
  repository_url TEXT NOT NULL UNIQUE,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_templates_user ON tasktally.templates(user_preferences_id);

DROP TRIGGER IF EXISTS set_timestamp_templates ON tasktally.templates;
CREATE TRIGGER set_timestamp_templates
BEFORE UPDATE ON tasktally.templates
FOR EACH ROW EXECUTE FUNCTION tasktally.trg_set_timestamp();

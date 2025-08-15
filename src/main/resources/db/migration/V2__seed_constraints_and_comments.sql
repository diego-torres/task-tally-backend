-- Comments for clarity
COMMENT ON TABLE user_preferences IS 'Per-user non-secret settings and defaults (JSONB)';
COMMENT ON COLUMN user_preferences.ui IS 'Flexible UI preferences and defaults, stored as JSONB';
COMMENT ON TABLE credential_refs IS 'References to external secrets (e.g., k8s or vault) for GitHub access';

-- Lowercase guard-rails
CREATE OR REPLACE FUNCTION enforce_lowercase_provider_scope()
RETURNS TRIGGER AS $$
BEGIN
  NEW.provider := lower(NEW.provider);
  NEW.scope := lower(NEW.scope);
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS lowercase_provider_scope ON credential_refs;
CREATE TRIGGER lowercase_provider_scope
BEFORE INSERT OR UPDATE ON credential_refs
FOR EACH ROW EXECUTE FUNCTION enforce_lowercase_provider_scope();

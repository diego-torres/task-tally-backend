ALTER TABLE credential_refs
  ADD COLUMN IF NOT EXISTS known_hosts_ref TEXT,
  ADD COLUMN IF NOT EXISTS passphrase_ref TEXT;

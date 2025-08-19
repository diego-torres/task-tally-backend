-- Add new columns to templates table for provider, default branch, and SSH key reference
ALTER TABLE tasktally.templates 
ADD COLUMN provider TEXT,
ADD COLUMN default_branch TEXT DEFAULT 'main',
ADD COLUMN ssh_key_name TEXT;

-- Add constraint to ensure provider is either github or gitlab
ALTER TABLE tasktally.templates 
ADD CONSTRAINT templates_provider_chk CHECK (provider IN ('github', 'gitlab'));

-- Add foreign key constraint to reference credential_refs table
ALTER TABLE tasktally.templates 
ADD CONSTRAINT templates_ssh_key_fk 
FOREIGN KEY (user_preferences_id, ssh_key_name) 
REFERENCES tasktally.credential_refs(user_preferences_id, name) 
ON DELETE SET NULL;

-- Add index for better query performance
CREATE INDEX IF NOT EXISTS idx_templates_provider ON tasktally.templates(provider);
CREATE INDEX IF NOT EXISTS idx_templates_ssh_key ON tasktally.templates(ssh_key_name);

-- Comments for documentation
COMMENT ON COLUMN tasktally.templates.provider IS 'Git provider: github or gitlab';
COMMENT ON COLUMN tasktally.templates.default_branch IS 'Default branch for the repository';
COMMENT ON COLUMN tasktally.templates.ssh_key_name IS 'Reference to SSH key name in credential_refs table';

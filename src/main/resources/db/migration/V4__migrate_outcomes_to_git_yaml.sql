-- Migration to move outcomes from database to Git-based YAML storage
-- This migration removes the outcomes table and adds yaml_path to templates

-- Add yaml_path column to templates table
ALTER TABLE tasktally.templates 
ADD COLUMN yaml_path TEXT DEFAULT 'outcomes.yml';

-- Add comment for documentation
COMMENT ON COLUMN tasktally.templates.yaml_path IS 'Path to the outcomes.yml file in the Git repository';

-- Drop the outcomes table and its constraints
DROP TABLE IF EXISTS tasktally.outcomes CASCADE;

-- Remove indexes that are no longer needed
DROP INDEX IF EXISTS idx_outcomes_template_id;
DROP INDEX IF EXISTS idx_outcomes_phase;
DROP INDEX IF EXISTS idx_outcomes_track;
DROP INDEX IF EXISTS idx_outcomes_product;
DROP INDEX IF EXISTS idx_outcomes_environment;

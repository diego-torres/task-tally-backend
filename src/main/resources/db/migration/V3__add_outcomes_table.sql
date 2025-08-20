-- Create outcomes table for template outcomes
CREATE TABLE tasktally.outcomes (
  id BIGSERIAL PRIMARY KEY,
  template_id BIGINT NOT NULL,
  phase TEXT NOT NULL,
  track TEXT NOT NULL,
  product TEXT NOT NULL,
  environment TEXT NOT NULL,
  prefix TEXT NOT NULL,
  outcome_text TEXT NOT NULL,
  scoping_notes TEXT,
  created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
  
  -- Foreign key constraint to templates table
  CONSTRAINT outcomes_template_fk 
    FOREIGN KEY (template_id) 
    REFERENCES tasktally.templates(id) 
    ON DELETE CASCADE
);

-- Add indexes for better query performance
CREATE INDEX IF NOT EXISTS idx_outcomes_template_id ON tasktally.outcomes(template_id);
CREATE INDEX IF NOT EXISTS idx_outcomes_phase ON tasktally.outcomes(phase);
CREATE INDEX IF NOT EXISTS idx_outcomes_track ON tasktally.outcomes(track);
CREATE INDEX IF NOT EXISTS idx_outcomes_product ON tasktally.outcomes(product);
CREATE INDEX IF NOT EXISTS idx_outcomes_environment ON tasktally.outcomes(environment);

-- Comments for documentation
COMMENT ON TABLE tasktally.outcomes IS 'Outcomes expected from consulting efforts for each template';
COMMENT ON COLUMN tasktally.outcomes.phase IS 'Phase of the consulting effort';
COMMENT ON COLUMN tasktally.outcomes.track IS 'Track or category of the outcome';
COMMENT ON COLUMN tasktally.outcomes.product IS 'Product related to the outcome';
COMMENT ON COLUMN tasktally.outcomes.environment IS 'Environment where the outcome applies';
COMMENT ON COLUMN tasktally.outcomes.prefix IS 'Prefix for the outcome';
COMMENT ON COLUMN tasktally.outcomes.outcome_text IS 'Description of the expected outcome';
COMMENT ON COLUMN tasktally.outcomes.scoping_notes IS 'Additional scoping notes for the outcome';

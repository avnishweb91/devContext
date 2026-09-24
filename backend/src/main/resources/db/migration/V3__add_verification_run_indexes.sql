CREATE INDEX IF NOT EXISTS idx_verification_runs_workspace_created
    ON verification_runs(workspace_id, created_at DESC);

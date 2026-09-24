CREATE INDEX IF NOT EXISTS idx_integrations_workspace
    ON integrations(workspace_id, provider);

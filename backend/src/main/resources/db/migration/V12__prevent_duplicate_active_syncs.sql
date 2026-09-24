CREATE UNIQUE INDEX IF NOT EXISTS uq_github_sync_jobs_active_workspace
    ON github_sync_jobs(workspace_id)
    WHERE status IN ('PENDING', 'RUNNING');

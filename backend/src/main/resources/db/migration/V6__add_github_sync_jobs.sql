CREATE TABLE IF NOT EXISTS github_sync_jobs (
    id UUID PRIMARY KEY,
    workspace_id UUID NOT NULL REFERENCES workspaces(id),
    principal_name VARCHAR(200) NOT NULL,
    status VARCHAR(30) NOT NULL,
    repositories INTEGER NOT NULL DEFAULT 0,
    pull_requests INTEGER NOT NULL DEFAULT 0,
    error_message VARCHAR(1000),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    started_at TIMESTAMPTZ,
    completed_at TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_github_sync_jobs_workspace_created
    ON github_sync_jobs(workspace_id, created_at DESC);

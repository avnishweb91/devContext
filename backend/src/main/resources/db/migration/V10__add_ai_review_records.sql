CREATE TABLE IF NOT EXISTS ai_review_records (
    id UUID PRIMARY KEY,
    workspace_id UUID NOT NULL REFERENCES workspaces(id),
    title VARCHAR(300) NOT NULL,
    context_digest VARCHAR(64) NOT NULL,
    summary TEXT NOT NULL,
    requested_by VARCHAR(320) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_ai_review_records_workspace_created
    ON ai_review_records(workspace_id, created_at DESC);

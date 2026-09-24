CREATE TABLE IF NOT EXISTS connected_repositories (
    id UUID PRIMARY KEY,
    workspace_id UUID NOT NULL REFERENCES workspaces(id),
    external_id BIGINT NOT NULL,
    name VARCHAR(180) NOT NULL,
    full_name VARCHAR(360) NOT NULL,
    private_repository BOOLEAN NOT NULL,
    url VARCHAR(700) NOT NULL,
    last_synced_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(workspace_id, external_id)
);

CREATE TABLE IF NOT EXISTS pull_request_records (
    id UUID PRIMARY KEY,
    repository_id UUID NOT NULL REFERENCES connected_repositories(id),
    external_number INTEGER NOT NULL,
    title VARCHAR(500) NOT NULL,
    author_login VARCHAR(180) NOT NULL,
    state VARCHAR(30) NOT NULL,
    url VARCHAR(700) NOT NULL,
    verification_status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(repository_id, external_number)
);

CREATE INDEX IF NOT EXISTS idx_repositories_workspace ON connected_repositories(workspace_id);
CREATE INDEX IF NOT EXISTS idx_pull_requests_repository ON pull_request_records(repository_id);


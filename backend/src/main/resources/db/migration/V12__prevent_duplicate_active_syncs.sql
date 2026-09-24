WITH ranked_active_jobs AS (
    SELECT id,
           ROW_NUMBER() OVER (PARTITION BY workspace_id ORDER BY created_at DESC, id DESC) AS position
    FROM github_sync_jobs
    WHERE status IN ('PENDING', 'RUNNING')
)
UPDATE github_sync_jobs
SET status = 'FAILED',
    error_message = 'Superseded by a newer synchronization job during migration',
    completed_at = CURRENT_TIMESTAMP
WHERE id IN (SELECT id FROM ranked_active_jobs WHERE position > 1);

CREATE UNIQUE INDEX IF NOT EXISTS uq_github_sync_jobs_active_workspace
    ON github_sync_jobs(workspace_id)
    WHERE status IN ('PENDING', 'RUNNING');

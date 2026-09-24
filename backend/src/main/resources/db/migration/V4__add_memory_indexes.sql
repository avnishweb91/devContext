CREATE INDEX IF NOT EXISTS idx_memories_workspace_created
    ON engineering_memories(workspace_id, created_at DESC);

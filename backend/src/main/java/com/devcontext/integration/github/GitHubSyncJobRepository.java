package com.devcontext.integration.github;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;
import java.util.Collection;
import java.time.Instant;

public interface GitHubSyncJobRepository extends JpaRepository<GitHubSyncJob, UUID> {
    List<GitHubSyncJob> findTop20ByWorkspaceIdOrderByCreatedAtDesc(UUID workspaceId);
    long countByWorkspaceIdAndStatusIn(UUID workspaceId, Collection<String> statuses);
    List<GitHubSyncJob> findByStatusAndCreatedAtBefore(String status, Instant cutoff);
    List<GitHubSyncJob> findByStatusAndStartedAtBefore(String status, Instant cutoff);
}

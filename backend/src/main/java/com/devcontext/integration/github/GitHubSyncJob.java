package com.devcontext.integration.github;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "github_sync_jobs")
public class GitHubSyncJob {
    @Id private UUID id;
    @Column(nullable = false) private UUID workspaceId;
    @Column(nullable = false, length = 200) private String principalName;
    @Column(nullable = false, length = 30) private String status;
    @Column(nullable = false) private int repositories;
    @Column(nullable = false) private int pullRequests;
    @Column(length = 1000) private String errorMessage;
    @Column(nullable = false) private Instant createdAt;
    private Instant startedAt;
    private Instant completedAt;

    protected GitHubSyncJob() {}

    public GitHubSyncJob(UUID workspaceId, String principalName) {
        this.id = UUID.randomUUID();
        this.workspaceId = workspaceId;
        this.principalName = principalName;
        this.status = "PENDING";
        this.createdAt = Instant.now();
    }

    public void start() { status = "RUNNING"; startedAt = Instant.now(); }
    public void complete(int repositories, int pullRequests) {
        this.status = "COMPLETED";
        this.repositories = repositories;
        this.pullRequests = pullRequests;
        this.completedAt = Instant.now();
    }
    public void fail(String message) {
        this.status = "FAILED";
        this.errorMessage = message == null ? "GitHub synchronization failed" : message.substring(0, Math.min(message.length(), 1000));
        this.completedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getWorkspaceId() { return workspaceId; }
    public String getStatus() { return status; }
    public int getRepositories() { return repositories; }
    public int getPullRequests() { return pullRequests; }
    public String getErrorMessage() { return errorMessage; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getStartedAt() { return startedAt; }
    public Instant getCompletedAt() { return completedAt; }
}

package com.devcontext.integration;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "integrations")
public class WorkspaceIntegration {
    @Id private UUID id;
    @Column(nullable = false) private UUID workspaceId;
    @Column(nullable = false, length = 40) private String provider;
    @Column(nullable = false, length = 30) private String status;
    @Column(length = 180) private String externalAccountId;
    @Column(nullable = false) private Instant createdAt;
    @Column private Instant lastSyncedAt;
    @Column(length = 1000) private String lastSyncError;

    protected WorkspaceIntegration() {}

    public WorkspaceIntegration(UUID workspaceId, String provider, String status, String externalAccountId) {
        this.id = UUID.randomUUID();
        this.workspaceId = workspaceId;
        this.provider = provider;
        this.status = status;
        this.externalAccountId = externalAccountId;
        this.createdAt = Instant.now();
    }

    public void connect(String status, String externalAccountId) {
        this.status = status;
        this.externalAccountId = externalAccountId;
        this.lastSyncError = null;
    }

    public void markSynced(Instant syncedAt) {
        this.status = "CONNECTED";
        this.lastSyncedAt = syncedAt;
        this.lastSyncError = null;
    }

    public void markSyncFailed(String error) {
        this.status = "SYNC_ERROR";
        this.lastSyncError = error == null ? "Provider sync failed" : error.substring(0, Math.min(error.length(), 1000));
    }

    public UUID getId() { return id; }
    public UUID getWorkspaceId() { return workspaceId; }
    public String getProvider() { return provider; }
    public String getStatus() { return status; }
    public String getExternalAccountId() { return externalAccountId; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getLastSyncedAt() { return lastSyncedAt; }
    public String getLastSyncError() { return lastSyncError; }
}

package com.devcontext.integration.github;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "connected_repositories")
public class ConnectedRepository {
    @Id private UUID id;
    @Column(nullable = false) private UUID workspaceId;
    @Column(nullable = false) private long externalId;
    @Column(nullable = false, length = 180) private String name;
    @Column(nullable = false, length = 360) private String fullName;
    @Column(nullable = false) private boolean privateRepository;
    @Column(nullable = false, length = 700) private String url;
    @Column(nullable = false) private Instant lastSyncedAt;

    protected ConnectedRepository() {}

    public ConnectedRepository(UUID workspaceId, long externalId, String name, String fullName, boolean privateRepository, String url) {
        this.id = UUID.randomUUID();
        this.workspaceId = workspaceId;
        this.externalId = externalId;
        this.name = name;
        this.fullName = fullName;
        this.privateRepository = privateRepository;
        this.url = url;
        this.lastSyncedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getWorkspaceId() { return workspaceId; }
    public long getExternalId() { return externalId; }
    public String getName() { return name; }
    public String getFullName() { return fullName; }
    public boolean isPrivateRepository() { return privateRepository; }
    public String getUrl() { return url; }
    public Instant getLastSyncedAt() { return lastSyncedAt; }
}

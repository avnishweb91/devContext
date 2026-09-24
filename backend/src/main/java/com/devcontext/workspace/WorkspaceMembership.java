package com.devcontext.workspace;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "workspace_memberships")
public class WorkspaceMembership {
    public enum Role { OWNER, ADMIN, ENGINEERING_LEAD, DEVELOPER, QA, DEVOPS, VIEWER }

    @Id
    private UUID id;
    @Column(nullable = false)
    private UUID workspaceId;
    @Column(nullable = false)
    private UUID userId;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private Role role;
    @Column(nullable = false)
    private Instant createdAt;

    protected WorkspaceMembership() {}

    public WorkspaceMembership(UUID workspaceId, UUID userId, Role role) {
        this.id = UUID.randomUUID();
        this.workspaceId = workspaceId;
        this.userId = userId;
        this.role = role;
        this.createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getWorkspaceId() { return workspaceId; }
    public UUID getUserId() { return userId; }
    public Role getRole() { return role; }
}


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
@Table(name = "workspace_invites")
public class WorkspaceInvite {
    @Id private UUID id;
    @Column(nullable = false) private UUID workspaceId;
    @Column(nullable = false, length = 320) private String email;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 40) private WorkspaceMembership.Role role;
    @Column(nullable = false, unique = true, length = 64) private String tokenHash;
    @Column(nullable = false) private Instant expiresAt;
    private Instant acceptedAt;
    @Column(nullable = false) private Instant createdAt;

    protected WorkspaceInvite() {}

    public WorkspaceInvite(UUID workspaceId, String email, WorkspaceMembership.Role role, String tokenHash, Instant expiresAt) {
        this.id = UUID.randomUUID();
        this.workspaceId = workspaceId;
        this.email = email.toLowerCase().trim();
        this.role = role;
        this.tokenHash = tokenHash;
        this.expiresAt = expiresAt;
        this.createdAt = Instant.now();
    }

    public boolean activeAt(Instant now) { return acceptedAt == null && expiresAt.isAfter(now); }
    public void accept() { acceptedAt = Instant.now(); }
    public UUID getId() { return id; }
    public UUID getWorkspaceId() { return workspaceId; }
    public String getEmail() { return email; }
    public WorkspaceMembership.Role getRole() { return role; }
    public String getTokenHash() { return tokenHash; }
    public Instant getExpiresAt() { return expiresAt; }
}

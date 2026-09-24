package com.devcontext.incident;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

@Entity
@Table(name = "incidents")
public class Incident {
    @Id private UUID id;
    @Column(nullable = false) private UUID workspaceId;
    @Column(nullable = false, length = 300) private String title;
    @Column(nullable = false, length = 20) private String severity;
    @Column(nullable = false, length = 30) private String status;
    @Column(length = 320) private String assignedTo;
    @Column(nullable = false) private Instant createdAt;
    @Column(nullable = false) private Instant updatedAt;

    protected Incident() {}

    public Incident(UUID workspaceId, String title, String severity, String assignedTo) {
        this.id = UUID.randomUUID(); this.workspaceId = workspaceId; this.title = title.trim();
        this.severity = severity.trim().toUpperCase(Locale.ROOT); this.status = "OPEN";
        this.assignedTo = assignedTo == null || assignedTo.isBlank() ? null : assignedTo.trim();
        this.createdAt = Instant.now(); this.updatedAt = this.createdAt;
    }

    public void changeStatus(String status) { this.status = status.trim().toUpperCase(Locale.ROOT); this.updatedAt = Instant.now(); }
    public UUID getId() { return id; }
    public UUID getWorkspaceId() { return workspaceId; }
    public String getTitle() { return title; }
    public String getSeverity() { return severity; }
    public String getStatus() { return status; }
    public String getAssignedTo() { return assignedTo; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}

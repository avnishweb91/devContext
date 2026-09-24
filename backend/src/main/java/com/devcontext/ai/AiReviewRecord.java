package com.devcontext.ai;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "ai_review_records")
public class AiReviewRecord {
    @Id private UUID id;
    @Column(nullable = false) private UUID workspaceId;
    @Column(nullable = false, length = 300) private String title;
    @Column(nullable = false, length = 64) private String contextDigest;
    @Column(nullable = false, columnDefinition = "TEXT") private String summary;
    @Column(nullable = false, length = 320) private String requestedBy;
    @Column(nullable = false) private Instant createdAt;

    protected AiReviewRecord() {}

    public AiReviewRecord(UUID workspaceId, String title, String contextDigest, String summary, String requestedBy) {
        this.id = UUID.randomUUID();
        this.workspaceId = workspaceId;
        this.title = title;
        this.contextDigest = contextDigest;
        this.summary = summary;
        this.requestedBy = requestedBy;
        this.createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getWorkspaceId() { return workspaceId; }
    public String getTitle() { return title; }
    public String getContextDigest() { return contextDigest; }
    public String getSummary() { return summary; }
    public String getRequestedBy() { return requestedBy; }
    public Instant getCreatedAt() { return createdAt; }
}

package com.devcontext.verification;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "verification_runs")
public class VerificationRun {
    @Id private UUID id;
    @Column(nullable = false) private UUID workspaceId;
    @Column(nullable = false, length = 180) private String pullRequestRef;
    @Column(nullable = false, length = 30) private String status;
    @Column(nullable = false) private int reviewItems;
    @Column(nullable = false) private Instant createdAt;

    protected VerificationRun() {}

    public VerificationRun(UUID workspaceId, String pullRequestRef, String status, int reviewItems) {
        this.id = UUID.randomUUID();
        this.workspaceId = workspaceId;
        this.pullRequestRef = pullRequestRef;
        this.status = status;
        this.reviewItems = reviewItems;
        this.createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getWorkspaceId() { return workspaceId; }
    public String getPullRequestRef() { return pullRequestRef; }
    public String getStatus() { return status; }
    public int getReviewItems() { return reviewItems; }
    public Instant getCreatedAt() { return createdAt; }
}

package com.devcontext.integration.github;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "pull_request_records")
public class PullRequestRecord {
    @Id private UUID id;
    @Column(nullable = false) private UUID repositoryId;
    @Column(nullable = false) private int externalNumber;
    @Column(nullable = false, length = 500) private String title;
    @Column(nullable = false, length = 180) private String authorLogin;
    @Column(nullable = false, length = 30) private String state;
    @Column(nullable = false, length = 700) private String url;
    @Column(nullable = false, length = 30) private String verificationStatus;
    @Column(nullable = false) private Instant updatedAt;

    protected PullRequestRecord() {}

    public PullRequestRecord(UUID repositoryId, int externalNumber, String title, String authorLogin, String state, String url) {
        this.id = UUID.randomUUID();
        this.repositoryId = repositoryId;
        this.externalNumber = externalNumber;
        this.title = title;
        this.authorLogin = authorLogin;
        this.state = state;
        this.url = url;
        this.verificationStatus = "PENDING";
        this.updatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getRepositoryId() { return repositoryId; }
    public int getExternalNumber() { return externalNumber; }
    public String getTitle() { return title; }
    public String getAuthorLogin() { return authorLogin; }
    public String getState() { return state; }
    public String getUrl() { return url; }
    public String getVerificationStatus() { return verificationStatus; }
    public Instant getUpdatedAt() { return updatedAt; }
}


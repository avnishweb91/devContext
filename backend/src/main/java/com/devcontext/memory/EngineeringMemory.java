package com.devcontext.memory;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "engineering_memories")
public class EngineeringMemory {
    @Id private UUID id;
    @Column(nullable = false) private UUID workspaceId;
    @Column(nullable = false, length = 300) private String title;
    @Column(nullable = false, length = 180) private String source;
    @Column(nullable = false, columnDefinition = "TEXT") private String content;
    @Column(columnDefinition = "TEXT") private String sourceUrl;
    @Column(nullable = false) private Instant createdAt;

    protected EngineeringMemory() {}

    public EngineeringMemory(UUID workspaceId, String title, String source, String content, String sourceUrl) {
        this.id = UUID.randomUUID();
        this.workspaceId = workspaceId;
        this.title = title;
        this.source = source;
        this.content = content;
        this.sourceUrl = sourceUrl;
        this.createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getWorkspaceId() { return workspaceId; }
    public String getTitle() { return title; }
    public String getSource() { return source; }
    public String getContent() { return content; }
    public String getSourceUrl() { return sourceUrl; }
    public Instant getCreatedAt() { return createdAt; }
}

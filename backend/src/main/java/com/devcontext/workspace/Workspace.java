package com.devcontext.workspace;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Column;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "workspaces")
public class Workspace {
    @Id
    private UUID id;
    @Column(nullable = false, length = 180)
    private String name;
    @Column(nullable = false)
    private Instant createdAt;

    protected Workspace() {}

    public Workspace(String name) {
        this.id = UUID.randomUUID();
        this.name = name;
        this.createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public String getName() { return name; }
    public Instant getCreatedAt() { return createdAt; }
}


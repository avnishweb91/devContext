package com.devcontext.workspace;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "app_users")
public class AppUser {
    @Id
    private UUID id;
    @Column(nullable = false, unique = true, length = 320)
    private String email;
    @Column(nullable = false, length = 180)
    private String displayName;
    @Column(length = 40)
    private String identityProvider;
    @Column(nullable = false)
    private Instant createdAt;

    protected AppUser() {}

    public AppUser(String email, String displayName) {
        this.id = UUID.randomUUID();
        this.email = email.toLowerCase().trim();
        this.displayName = displayName.trim();
        this.createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public String getEmail() { return email; }
    public String getDisplayName() { return displayName; }
    public Instant getCreatedAt() { return createdAt; }
}


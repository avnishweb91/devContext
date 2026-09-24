package com.devcontext.workspace;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface WorkspaceInviteRepository extends JpaRepository<WorkspaceInvite, UUID> {
    Optional<WorkspaceInvite> findByTokenHash(String tokenHash);
}

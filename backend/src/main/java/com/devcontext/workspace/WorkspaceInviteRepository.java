package com.devcontext.workspace;

import org.springframework.data.jpa.repository.JpaRepository;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;

import java.util.Optional;
import java.util.UUID;

public interface WorkspaceInviteRepository extends JpaRepository<WorkspaceInvite, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<WorkspaceInvite> findByTokenHash(String tokenHash);
}

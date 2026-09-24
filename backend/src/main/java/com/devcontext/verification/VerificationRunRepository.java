package com.devcontext.verification;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface VerificationRunRepository extends JpaRepository<VerificationRun, UUID> {
    List<VerificationRun> findAllByWorkspaceIdOrderByCreatedAtDesc(UUID workspaceId);
}

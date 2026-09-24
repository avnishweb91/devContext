package com.devcontext.integration;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WorkspaceIntegrationRepository extends JpaRepository<WorkspaceIntegration, UUID> {
    List<WorkspaceIntegration> findAllByWorkspaceIdOrderByProvider(UUID workspaceId);
    Optional<WorkspaceIntegration> findByWorkspaceIdAndProvider(UUID workspaceId, String provider);
}

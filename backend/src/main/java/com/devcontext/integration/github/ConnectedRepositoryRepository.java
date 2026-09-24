package com.devcontext.integration.github;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ConnectedRepositoryRepository extends JpaRepository<ConnectedRepository, UUID> {
    Optional<ConnectedRepository> findByWorkspaceIdAndExternalId(UUID workspaceId, long externalId);
    List<ConnectedRepository> findAllByWorkspaceIdOrderByFullName(UUID workspaceId);
}


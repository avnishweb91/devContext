package com.devcontext.incident;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IncidentRepository extends JpaRepository<Incident, UUID> {
    List<Incident> findAllByWorkspaceIdOrderByUpdatedAtDesc(UUID workspaceId);
    Optional<Incident> findByIdAndWorkspaceId(UUID id, UUID workspaceId);
    long countByWorkspaceIdAndStatusIn(UUID workspaceId, List<String> statuses);
}

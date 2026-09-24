package com.devcontext.workspace;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;
import java.util.List;
import java.util.Optional;

public interface WorkspaceMembershipRepository extends JpaRepository<WorkspaceMembership, UUID> {
    boolean existsByWorkspaceIdAndUserId(UUID workspaceId, UUID userId);
    List<WorkspaceMembership> findAllByUserId(UUID userId);
    Optional<WorkspaceMembership> findByWorkspaceIdAndUserId(UUID workspaceId, UUID userId);
}

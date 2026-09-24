package com.devcontext.workspace;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;
import java.util.List;

public interface WorkspaceMembershipRepository extends JpaRepository<WorkspaceMembership, UUID> {
    boolean existsByWorkspaceIdAndUserId(UUID workspaceId, UUID userId);
    List<WorkspaceMembership> findAllByUserId(UUID userId);
}

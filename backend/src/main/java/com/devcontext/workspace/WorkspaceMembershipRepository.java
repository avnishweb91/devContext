package com.devcontext.workspace;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface WorkspaceMembershipRepository extends JpaRepository<WorkspaceMembership, UUID> {
    boolean existsByWorkspaceIdAndUserId(UUID workspaceId, UUID userId);
}


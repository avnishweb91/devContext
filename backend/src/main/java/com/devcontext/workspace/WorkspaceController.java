package com.devcontext.workspace;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/workspaces")
public class WorkspaceController {
    private final WorkspaceAccessService access;
    private final WorkspaceMembershipRepository memberships;
    private final WorkspaceRepository workspaces;

    public WorkspaceController(WorkspaceAccessService access, WorkspaceMembershipRepository memberships, WorkspaceRepository workspaces) {
        this.access = access;
        this.memberships = memberships;
        this.workspaces = workspaces;
    }

    @GetMapping
    public List<WorkspaceSummary> list(Authentication authentication) {
        AppUser user = access.currentUser(authentication);
        List<UUID> ids = memberships.findAllByUserId(user.getId()).stream().map(WorkspaceMembership::getWorkspaceId).toList();
        return workspaces.findAllById(ids).stream().map(workspace -> new WorkspaceSummary(workspace.getId(), workspace.getName())).toList();
    }

    public record WorkspaceSummary(UUID id, String name) {}
}


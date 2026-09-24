package com.devcontext.workspace;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OnboardingService {
    private final WorkspaceRepository workspaces;
    private final AppUserRepository users;
    private final WorkspaceMembershipRepository memberships;

    public OnboardingService(WorkspaceRepository workspaces, AppUserRepository users, WorkspaceMembershipRepository memberships) {
        this.workspaces = workspaces;
        this.users = users;
        this.memberships = memberships;
    }

    @Transactional
    public OnboardingController.OnboardingResponse createWorkspace(String companyName, String ownerName, String ownerEmail) {
        Workspace workspace = workspaces.save(new Workspace(companyName));
        AppUser owner = users.findByEmailIgnoreCase(ownerEmail)
                .orElseGet(() -> users.save(new AppUser(ownerEmail, ownerName)));
        WorkspaceMembership membership = memberships.save(new WorkspaceMembership(
                workspace.getId(), owner.getId(), WorkspaceMembership.Role.OWNER));
        return new OnboardingController.OnboardingResponse(workspace.getId(), workspace.getName(), owner.getId(), owner.getEmail(), membership.getRole().name());
    }
}


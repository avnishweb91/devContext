package com.devcontext.workspace;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WorkspaceAccessServiceTest {
    @Mock AppUserRepository users;
    @Mock WorkspaceMembershipRepository memberships;
    @Mock Authentication authentication;

    @Test
    void rejectsAuthenticatedUserFromAnotherWorkspace() {
        AppUser user = new AppUser("developer@acme.test", "Developer");
        UUID workspaceId = UUID.randomUUID();
        authenticatedAs(user.getEmail());
        when(users.findByEmailIgnoreCase(user.getEmail())).thenReturn(Optional.of(user));
        when(memberships.existsByWorkspaceIdAndUserId(workspaceId, user.getId())).thenReturn(false);

        WorkspaceAccessService service = new WorkspaceAccessService(users, memberships);

        assertThatThrownBy(() -> service.requireMember(workspaceId, authentication))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(error -> assertThat(((ResponseStatusException) error).getStatusCode().value()).isEqualTo(403));
    }

    @Test
    void allowsOwnerToUseAdminOnlyOperation() {
        AppUser user = new AppUser("owner@acme.test", "Owner");
        UUID workspaceId = UUID.randomUUID();
        authenticatedAs(user.getEmail());
        when(users.findByEmailIgnoreCase(user.getEmail())).thenReturn(Optional.of(user));
        when(memberships.findByWorkspaceIdAndUserId(workspaceId, user.getId()))
                .thenReturn(Optional.of(new WorkspaceMembership(workspaceId, user.getId(), WorkspaceMembership.Role.OWNER)));

        WorkspaceAccessService service = new WorkspaceAccessService(users, memberships);

        assertThat(service.requireAdmin(workspaceId, authentication)).isSameAs(user);
    }

    @Test
    void rejectsRegularMemberFromAdminOnlyOperation() {
        AppUser user = new AppUser("developer@acme.test", "Developer");
        UUID workspaceId = UUID.randomUUID();
        authenticatedAs(user.getEmail());
        when(users.findByEmailIgnoreCase(user.getEmail())).thenReturn(Optional.of(user));
        when(memberships.findByWorkspaceIdAndUserId(workspaceId, user.getId()))
                .thenReturn(Optional.of(new WorkspaceMembership(workspaceId, user.getId(), WorkspaceMembership.Role.DEVELOPER)));

        WorkspaceAccessService service = new WorkspaceAccessService(users, memberships);

        assertThatThrownBy(() -> service.requireAdmin(workspaceId, authentication))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(error -> assertThat(((ResponseStatusException) error).getStatusCode().value()).isEqualTo(403));
    }

    private void authenticatedAs(String email) {
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn(email);
    }
}

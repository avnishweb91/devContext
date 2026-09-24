package com.devcontext.workspace;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WorkspaceInviteControllerTest {
    @Mock WorkspaceAccessService access;
    @Mock WorkspaceInviteRepository invites;
    @Mock AppUserRepository users;
    @Mock WorkspaceMembershipRepository memberships;

    @Test
    void createsExpiringInviteWithOpaqueToken() {
        UUID workspaceId = UUID.randomUUID();
        WorkspaceInvite saved = new WorkspaceInvite(workspaceId, "dev@acme.test", WorkspaceMembership.Role.DEVELOPER,
                "hash", java.time.Instant.now().plusSeconds(3600));
        when(invites.save(any(WorkspaceInvite.class))).thenReturn(saved);
        WorkspaceInviteController controller = new WorkspaceInviteController(access, invites, users, memberships);

        var result = controller.create(workspaceId,
                new WorkspaceInviteController.CreateInviteRequest("dev@acme.test", "developer"),
                new UsernamePasswordAuthenticationToken("owner@acme.test", "n/a", java.util.List.of()));

        assertThat(result.token()).isNotBlank();
        assertThat(result.token()).doesNotContain("hash");
        assertThat(result.role()).isEqualTo("DEVELOPER");
        verify(access).requireAdmin(eq(workspaceId), any());
        verify(invites).save(any(WorkspaceInvite.class));
    }

    @Test
    void acceptsAnInviteOnlyOnce() {
        UUID workspaceId = UUID.randomUUID();
        WorkspaceInvite invite = new WorkspaceInvite(workspaceId, "dev@acme.test", WorkspaceMembership.Role.DEVELOPER,
                "hashed-token", java.time.Instant.now().plusSeconds(3600));
        AppUser user = new AppUser("dev@acme.test", "Developer");
        when(access.identityEmail(any())).thenReturn("dev@acme.test");
        when(invites.findByTokenHash(any())).thenReturn(Optional.of(invite));
        when(users.findByEmailIgnoreCase("dev@acme.test")).thenReturn(Optional.of(user));
        when(memberships.existsByWorkspaceIdAndUserId(workspaceId, user.getId())).thenReturn(false);
        WorkspaceInviteController controller = new WorkspaceInviteController(access, invites, users, memberships);

        controller.accept(new WorkspaceInviteController.AcceptInviteRequest("token"),
                new UsernamePasswordAuthenticationToken("dev@acme.test", "n/a"));

        assertThatThrownBy(() -> controller.accept(new WorkspaceInviteController.AcceptInviteRequest("token"),
                new UsernamePasswordAuthenticationToken("dev@acme.test", "n/a")))
                .hasMessageContaining("Invite is invalid or expired");
    }
}

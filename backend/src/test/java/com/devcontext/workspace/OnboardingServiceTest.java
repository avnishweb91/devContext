package com.devcontext.workspace;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OnboardingServiceTest {
    @Mock WorkspaceRepository workspaces;
    @Mock AppUserRepository users;
    @Mock WorkspaceMembershipRepository memberships;
    @InjectMocks OnboardingService service;

    @Test
    void createsOwnerMembershipForNewCompany() {
        UUID workspaceId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        when(workspaces.save(any(Workspace.class))).thenAnswer(invocation -> {
            Workspace workspace = invocation.getArgument(0);
            return new WorkspaceView(workspace, workspaceId);
        });
        when(users.findByEmailIgnoreCase("owner@acme.test")).thenReturn(Optional.empty());
        when(users.save(any(AppUser.class))).thenAnswer(invocation -> new UserView(invocation.getArgument(0), userId));
        when(memberships.save(any(WorkspaceMembership.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OnboardingController.OnboardingResponse response = service.createWorkspace("Acme", "Owner", "owner@acme.test");

        assertThat(response.companyName()).isEqualTo("Acme");
        assertThat(response.ownerEmail()).isEqualTo("owner@acme.test");
        assertThat(response.role()).isEqualTo("OWNER");
    }

    private static final class WorkspaceView extends Workspace {
        private final UUID id;
        WorkspaceView(Workspace source, UUID id) { super(source.getName()); this.id = id; }
        @Override public UUID getId() { return id; }
    }

    private static final class UserView extends AppUser {
        private final UUID id;
        UserView(AppUser source, UUID id) { super(source.getEmail(), source.getDisplayName()); this.id = id; }
        @Override public UUID getId() { return id; }
    }
}


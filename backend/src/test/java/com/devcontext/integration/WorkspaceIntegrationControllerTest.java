package com.devcontext.integration;

import com.devcontext.workspace.WorkspaceAccessService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WorkspaceIntegrationControllerTest {
    @Mock WorkspaceAccessService access;
    @Mock WorkspaceIntegrationRepository integrations;

    @Test
    void connectsSupportedProviderForWorkspace() {
        UUID workspaceId = UUID.randomUUID();
        WorkspaceIntegration saved = new WorkspaceIntegration(workspaceId, "JIRA", "CONNECTED", "jira-acme");
        when(integrations.save(any(WorkspaceIntegration.class))).thenReturn(saved);
        WorkspaceIntegrationController controller = new WorkspaceIntegrationController(access, integrations);

        var result = controller.connect(workspaceId,
                new WorkspaceIntegrationController.ConnectRequest("jira", "jira-acme"),
                new UsernamePasswordAuthenticationToken("dev@example.com", "n/a"));

        assertThat(result.provider()).isEqualTo("JIRA");
        assertThat(result.status()).isEqualTo("CONNECTED");
        verify(access).requireMember(eq(workspaceId), any());
        verify(integrations).save(any(WorkspaceIntegration.class));
    }
}

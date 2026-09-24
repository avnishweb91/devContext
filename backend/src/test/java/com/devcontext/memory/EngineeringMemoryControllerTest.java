package com.devcontext.memory;

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
class EngineeringMemoryControllerTest {
    @Mock WorkspaceAccessService access;
    @Mock EngineeringMemoryRepository memories;

    @Test
    void createsMemoryForTheAuthenticatedWorkspace() {
        UUID workspaceId = UUID.randomUUID();
        EngineeringMemory saved = new EngineeringMemory(workspaceId, "Retry policy", "decision", "Use bounded retries.", null);
        when(memories.save(any(EngineeringMemory.class))).thenReturn(saved);
        EngineeringMemoryController controller = new EngineeringMemoryController(access, memories);

        EngineeringMemory result = controller.create(workspaceId,
                new EngineeringMemoryController.CreateMemoryRequest("Retry policy", "decision", "Use bounded retries.", null),
                new UsernamePasswordAuthenticationToken("dev@example.com", "n/a"));

        assertThat(result.getWorkspaceId()).isEqualTo(workspaceId);
        assertThat(result.getContent()).contains("bounded retries");
        verify(access).requireMember(eq(workspaceId), any());
        verify(memories).save(any(EngineeringMemory.class));
    }
}

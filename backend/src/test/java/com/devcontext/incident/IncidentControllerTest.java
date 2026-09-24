package com.devcontext.incident;

import com.devcontext.workspace.WorkspaceAccessService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IncidentControllerTest {
    @Mock WorkspaceAccessService access;
    @Mock IncidentRepository incidents;

    @Test
    void createsAndUpdatesWorkspaceIncident() {
        UUID workspaceId = UUID.randomUUID();
        Incident saved = new Incident(workspaceId, "API latency", "HIGH", "oncall@acme.test");
        when(incidents.save(any(Incident.class))).thenReturn(saved);
        IncidentController controller = new IncidentController(access, incidents);

        Incident created = controller.create(workspaceId,
                new IncidentController.CreateIncidentRequest("API latency", "high", "oncall@acme.test"), auth());

        assertThat(created.getSeverity()).isEqualTo("HIGH");
        when(incidents.findByIdAndWorkspaceId(saved.getId(), workspaceId)).thenReturn(Optional.of(saved));
        Incident updated = controller.updateStatus(workspaceId, saved.getId(),
                new IncidentController.StatusRequest("acknowledged"), auth());
        assertThat(updated.getStatus()).isEqualTo("ACKNOWLEDGED");
    }

    @Test
    void refusesIncidentFromAnotherWorkspace() {
        UUID workspaceId = UUID.randomUUID();
        UUID incidentId = UUID.randomUUID();
        when(incidents.findByIdAndWorkspaceId(incidentId, workspaceId)).thenReturn(Optional.empty());
        IncidentController controller = new IncidentController(access, incidents);

        assertThatThrownBy(() -> controller.updateStatus(workspaceId, incidentId,
                new IncidentController.StatusRequest("resolved"), auth()))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Incident not found");
    }

    private UsernamePasswordAuthenticationToken auth() { return new UsernamePasswordAuthenticationToken("dev@example.com", "n/a"); }
}

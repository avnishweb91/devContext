package com.devcontext.incident;

import com.devcontext.workspace.WorkspaceAccessService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/workspaces/{workspaceId}/incidents")
public class IncidentController {
    private static final Set<String> SEVERITIES = Set.of("LOW", "MEDIUM", "HIGH", "CRITICAL");
    private static final Set<String> STATUSES = Set.of("OPEN", "ACKNOWLEDGED", "RESOLVED");
    private final WorkspaceAccessService access;
    private final IncidentRepository incidents;

    public IncidentController(WorkspaceAccessService access, IncidentRepository incidents) { this.access = access; this.incidents = incidents; }

    @GetMapping
    public List<Incident> list(@PathVariable UUID workspaceId, Authentication authentication) {
        access.requireMember(workspaceId, authentication);
        return incidents.findAllByWorkspaceIdOrderByUpdatedAtDesc(workspaceId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Incident create(@PathVariable UUID workspaceId, @Valid @RequestBody CreateIncidentRequest request, Authentication authentication) {
        access.requireMember(workspaceId, authentication);
        String severity = normalize(request.severity(), SEVERITIES, "severity");
        return incidents.save(new Incident(workspaceId, request.title(), severity, request.assignedTo()));
    }

    @PostMapping("/{incidentId}/status")
    public Incident updateStatus(@PathVariable UUID workspaceId, @PathVariable UUID incidentId,
                                 @Valid @RequestBody StatusRequest request, Authentication authentication) {
        access.requireMember(workspaceId, authentication);
        Incident incident = incidents.findByIdAndWorkspaceId(incidentId, workspaceId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Incident not found"));
        incident.changeStatus(normalize(request.status(), STATUSES, "status"));
        return incidents.save(incident);
    }

    private String normalize(String value, Set<String> allowed, String field) {
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        if (!allowed.contains(normalized)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid incident " + field);
        return normalized;
    }

    public record CreateIncidentRequest(@NotBlank @Size(max = 300) String title, @NotBlank @Size(max = 20) String severity,
                                        @Size(max = 320) String assignedTo) {}
    public record StatusRequest(@NotBlank @Size(max = 30) String status) {}
}

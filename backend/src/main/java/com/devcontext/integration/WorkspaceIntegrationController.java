package com.devcontext.integration;

import com.devcontext.workspace.WorkspaceAccessService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/workspaces/{workspaceId}/integrations")
public class WorkspaceIntegrationController {
    private static final Set<String> SUPPORTED_PROVIDERS = Set.of("GITHUB", "JIRA", "SLACK");
    private final WorkspaceAccessService access;
    private final WorkspaceIntegrationRepository integrations;

    public WorkspaceIntegrationController(WorkspaceAccessService access, WorkspaceIntegrationRepository integrations) {
        this.access = access;
        this.integrations = integrations;
    }

    @GetMapping
    public List<IntegrationSummary> list(@PathVariable UUID workspaceId, Authentication authentication) {
        access.requireMember(workspaceId, authentication);
        return integrations.findAllByWorkspaceIdOrderByProvider(workspaceId).stream()
                .map(integration -> new IntegrationSummary(integration.getProvider(), integration.getStatus(), integration.getExternalAccountId(), integration.getLastSyncedAt(), integration.getLastSyncError()))
                .toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public IntegrationSummary connect(@PathVariable UUID workspaceId, @Valid @RequestBody ConnectRequest request,
                                      Authentication authentication) {
        access.requireAdmin(workspaceId, authentication);
        String provider = request.provider().trim().toUpperCase(Locale.ROOT);
        if (!SUPPORTED_PROVIDERS.contains(provider)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported integration provider");
        }
        WorkspaceIntegration integration = integrations.findByWorkspaceIdAndProvider(workspaceId, provider)
                .orElseGet(() -> new WorkspaceIntegration(workspaceId, provider, "CONNECTED", request.externalAccountId()));
        integration.connect("CONNECTED", request.externalAccountId());
        WorkspaceIntegration saved = integrations.save(integration);
        return new IntegrationSummary(saved.getProvider(), saved.getStatus(), saved.getExternalAccountId(), saved.getLastSyncedAt(), saved.getLastSyncError());
    }

    public record ConnectRequest(@NotBlank @Size(max = 40) String provider, @Size(max = 180) String externalAccountId) {}
    public record IntegrationSummary(String provider, String status, String externalAccountId, java.time.Instant lastSyncedAt, String lastSyncError) {}
}

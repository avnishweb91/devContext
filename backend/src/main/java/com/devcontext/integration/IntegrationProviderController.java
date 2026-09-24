package com.devcontext.integration;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Set;
import com.devcontext.workspace.WorkspaceAccessService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/integrations/providers")
public class IntegrationProviderController {
    private final ObjectProvider<ClientRegistrationRepository> registrations;
    private final WorkspaceAccessService access;

    public IntegrationProviderController(ObjectProvider<ClientRegistrationRepository> registrations, WorkspaceAccessService access) {
        this.registrations = registrations;
        this.access = access;
    }

    @GetMapping("/{provider}/connect")
    public void connect(@PathVariable String provider, @RequestParam UUID workspaceId, Authentication authentication, HttpServletRequest request,
                        HttpServletResponse response) throws IOException {
        access.requireAdmin(workspaceId, authentication);
        String email = access.identityEmail(authentication);
        String id = provider.toLowerCase(java.util.Locale.ROOT);
        if (!Set.of("jira", "slack").contains(id) || registrations.getIfAvailable() == null
                || registrations.getIfAvailable().findByRegistrationId(id) == null) {
            response.sendError(404, "Integration provider is not configured");
            return;
        }
        request.getSession().setAttribute("devcontext.oauth.link.email", email);
        response.sendRedirect("/oauth2/authorization/" + id);
    }

    @GetMapping
    public List<ProviderSummary> providers() {
        ClientRegistrationRepository repository = registrations.getIfAvailable();
        if (repository == null) return List.of();
        return List.of("github", "jira", "slack").stream()
                .map(repository::findByRegistrationId)
                .filter(java.util.Objects::nonNull)
                .map(this::summary)
                .toList();
    }

    private ProviderSummary summary(ClientRegistration registration) {
        String authorizationPath = "github".equals(registration.getRegistrationId())
                ? "/oauth2/authorization/github"
                : "/api/integrations/providers/" + registration.getRegistrationId() + "/connect";
        return new ProviderSummary(registration.getRegistrationId(), registration.getClientName(),
                authorizationPath);
    }

    public record ProviderSummary(String id, String name, String authorizationPath) {}
}

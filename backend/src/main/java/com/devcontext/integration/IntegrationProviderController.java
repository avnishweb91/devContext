package com.devcontext.integration;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/integrations/providers")
public class IntegrationProviderController {
    private final ObjectProvider<ClientRegistrationRepository> registrations;

    public IntegrationProviderController(ObjectProvider<ClientRegistrationRepository> registrations) {
        this.registrations = registrations;
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
        return new ProviderSummary(registration.getRegistrationId(), registration.getClientName(),
                "/oauth2/authorization/" + registration.getRegistrationId());
    }

    public record ProviderSummary(String id, String name, String authorizationPath) {}
}

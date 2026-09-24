package com.devcontext.integration;

import com.devcontext.memory.EngineeringMemoryRepository;
import com.devcontext.workspace.WorkspaceAccessService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProviderSyncServiceTest {
    @Mock WorkspaceAccessService access;
    @Mock WorkspaceIntegrationRepository integrations;
    @Mock EngineeringMemoryRepository memories;
    @Mock ObjectProvider<OAuth2AuthorizedClientService> authorizedClients;
    @Mock RestClient.Builder restClientBuilder;

    @Test
    void rejectsSyncWhenOAuthIsNotEnabled() {
        when(restClientBuilder.build()).thenReturn(mock(RestClient.class));
        when(authorizedClients.getIfAvailable()).thenReturn(null);
        ProviderSyncService service = service();

        assertThatThrownBy(() -> service.sync(UUID.randomUUID(), "jira", authentication()))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("OAuth security is not enabled");
    }

    @Test
    void rejectsUnsupportedProviderBeforeAccessingOAuth() {
        when(restClientBuilder.build()).thenReturn(mock(RestClient.class));
        ProviderSyncService service = service();

        assertThatThrownBy(() -> service.sync(UUID.randomUUID(), "github", authentication()))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Only Jira and Slack");
    }

    private ProviderSyncService service() {
        return new ProviderSyncService(access, integrations, memories, authorizedClients,
                new ObjectMapper(), restClientBuilder);
    }

    private Authentication authentication() {
        return mock(Authentication.class);
    }
}

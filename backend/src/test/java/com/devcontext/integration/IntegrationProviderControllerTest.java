package com.devcontext.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import com.devcontext.workspace.WorkspaceAccessService;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class IntegrationProviderControllerTest {
    @Test
    void listsOnlyConfiguredProviders() {
        ClientRegistration github = ClientRegistration.withRegistrationId("github")
                .clientId("id").clientSecret("secret").authorizationUri("https://github.com/login/oauth/authorize")
                .tokenUri("https://github.com/login/oauth/access_token").authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
                .scope("read:user").build();
        ClientRegistrationRepository repository = id -> "github".equals(id) ? github : null;
        ObjectProvider<ClientRegistrationRepository> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(repository);

        List<IntegrationProviderController.ProviderSummary> result = new IntegrationProviderController(provider, mock(WorkspaceAccessService.class)).providers();

        assertThat(result).singleElement().satisfies(item -> {
            assertThat(item.id()).isEqualTo("github");
            assertThat(item.authorizationPath()).isEqualTo("/oauth2/authorization/github");
        });
    }
}

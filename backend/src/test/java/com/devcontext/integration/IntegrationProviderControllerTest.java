package com.devcontext.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import com.devcontext.workspace.WorkspaceAccessService;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

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

    @Test
    void startsLinkedProviderFlowForAuthenticatedIdentity() throws Exception {
        ClientRegistration jira = ClientRegistration.withRegistrationId("jira")
                .clientId("id").clientSecret("secret").authorizationUri("https://jira.example/authorize")
                .tokenUri("https://jira.example/token").authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}").scope("read:jira-work").build();
        ClientRegistrationRepository repository = id -> "jira".equals(id) ? jira : null;
        ObjectProvider<ClientRegistrationRepository> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(repository);
        WorkspaceAccessService access = mock(WorkspaceAccessService.class);
        when(access.identityEmail(any())).thenReturn("owner@acme.test");
        IntegrationProviderController controller = new IntegrationProviderController(provider, access);
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        controller.connect("jira", new UsernamePasswordAuthenticationToken("owner@acme.test", "n/a"), request, response);

        assertThat(request.getSession().getAttribute("devcontext.oauth.link.email")).isEqualTo("owner@acme.test");
        assertThat(response.getRedirectedUrl()).isEqualTo("/oauth2/authorization/jira");
        verify(access).identityEmail(any());
    }
}

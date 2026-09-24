package com.devcontext.security;

import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.core.context.SecurityContextHolder;
import com.devcontext.workspace.AppUser;
import com.devcontext.workspace.AppUserRepository;
import com.devcontext.workspace.WorkspaceMembership;
import com.devcontext.workspace.WorkspaceMembershipRepository;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.eq;

@ExtendWith(MockitoExtension.class)
class OAuth2LinkSuccessHandlerTest {
    @Mock ObjectProvider<OAuth2AuthorizedClientService> clients;
    @Mock ObjectProvider<ClientRegistrationRepository> repositories;
    @Mock OAuth2AuthorizedClientService clientService;
    @Mock AppUserRepository users;
    @Mock WorkspaceMembershipRepository memberships;

    @AfterEach
    void clearSecurityContext() { SecurityContextHolder.clearContext(); }

    @Test
    void restoresApplicationIdentityAndRekeysLinkedToken() throws Exception {
        ClientRegistration jira = ClientRegistration.withRegistrationId("jira")
                .clientId("id").clientSecret("secret").authorizationUri("https://jira.example/authorize")
                .tokenUri("https://jira.example/token").authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}").build();
        ClientRegistrationRepository repository = id -> jira;
        OAuth2AccessToken accessToken = new OAuth2AccessToken(OAuth2AccessToken.TokenType.BEARER, "access-token",
                Instant.now().minusSeconds(10), Instant.now().plusSeconds(3600));
        OAuth2AuthorizedClient providerClient = new OAuth2AuthorizedClient(jira, "jira-subject", accessToken,
                new OAuth2RefreshToken("refresh-token", Instant.now().minusSeconds(10)));
        OAuth2User user = mock(OAuth2User.class);
        when(user.getName()).thenReturn("jira-subject");
        OAuth2AuthenticationToken authentication = new OAuth2AuthenticationToken(user,
                List.of(new SimpleGrantedAuthority("ROLE_USER")), "jira");
        when(clients.getIfAvailable()).thenReturn(clientService);
        when(repositories.getIfAvailable()).thenReturn(repository);
        when(clientService.loadAuthorizedClient("jira", "jira-subject")).thenReturn(providerClient);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.getSession().setAttribute("devcontext.oauth.link.email", "owner@acme.test");
        MockHttpServletResponse response = new MockHttpServletResponse();
        new OAuth2LinkSuccessHandler(clients, repositories, users, memberships, "https://app.example")
                .onAuthenticationSuccess(request, response, authentication);

        assertThat(response.getRedirectedUrl()).isEqualTo("https://app.example/integrations");
        assertThat(SecurityContextHolder.getContext().getAuthentication().getName()).isEqualTo("owner@acme.test");
        assertThat(request.getSession().getAttribute("devcontext.oauth.link.email")).isNull();
        verify(clientService).saveAuthorizedClient(any(OAuth2AuthorizedClient.class), any());
    }

    @Test
    void clearsStaleLinkMarkerWhenNormalGitHubLoginCompletes() throws Exception {
        OAuth2User user = mock(OAuth2User.class);
        OAuth2AuthenticationToken authentication = new OAuth2AuthenticationToken(user, List.of(), "github");
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.getSession().setAttribute("devcontext.oauth.link.email", "old@acme.test");
        MockHttpServletResponse response = new MockHttpServletResponse();

        new OAuth2LinkSuccessHandler(clients, repositories, users, memberships, "https://app.example")
                .onAuthenticationSuccess(request, response, authentication);

        assertThat(request.getSession().getAttribute("devcontext.oauth.link.email")).isNull();
        assertThat(response.getRedirectedUrl()).isEqualTo("https://app.example/onboarding");
    }

    @Test
    void sendsExistingGithubMemberToDashboard() throws Exception {
        OAuth2User user = mock(OAuth2User.class);
        when(user.getAttribute("email")).thenReturn("owner@acme.test");
        OAuth2AuthenticationToken authentication = new OAuth2AuthenticationToken(user, List.of(), "github");
        AppUser existing = new AppUser("owner@acme.test", "Owner");
        when(users.findByEmailIgnoreCase("owner@acme.test")).thenReturn(java.util.Optional.of(existing));
        when(memberships.findAllByUserId(eq(existing.getId()))).thenReturn(List.of(
                new WorkspaceMembership(java.util.UUID.randomUUID(), existing.getId(), WorkspaceMembership.Role.OWNER)));
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        new OAuth2LinkSuccessHandler(clients, repositories, users, memberships, "https://app.example")
                .onAuthenticationSuccess(request, response, authentication);

        assertThat(response.getRedirectedUrl()).isEqualTo("https://app.example/");
    }
}

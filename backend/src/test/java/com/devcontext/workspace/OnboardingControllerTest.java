package com.devcontext.workspace;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2UserAuthority;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OnboardingControllerTest {
    @Mock OnboardingService service;
    @Mock WorkspaceAccessService access;

    @Test
    void rejectsOwnerEmailDifferentFromAuthenticatedIdentity() {
        Authentication authentication = new UsernamePasswordAuthenticationToken("github-id", "n/a", java.util.List.of());
        when(access.identityEmail(authentication)).thenReturn("signed-in@acme.test");
        OnboardingController controller = new OnboardingController(service, access);

        assertThatThrownBy(() -> controller.createWorkspace(
                new OnboardingController.CreateWorkspaceRequest("Acme", "Owner", "other@acme.test"), authentication))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Owner email must match");
    }
}

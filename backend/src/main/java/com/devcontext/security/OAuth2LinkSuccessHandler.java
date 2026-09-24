package com.devcontext.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;

@Component
public class OAuth2LinkSuccessHandler implements AuthenticationSuccessHandler {
    private static final String LINK_EMAIL = "devcontext.oauth.link.email";
    private final ObjectProvider<OAuth2AuthorizedClientService> authorizedClients;
    private final ObjectProvider<ClientRegistrationRepository> registrations;
    private final String frontendOrigin;

    public OAuth2LinkSuccessHandler(ObjectProvider<OAuth2AuthorizedClientService> authorizedClients, ObjectProvider<ClientRegistrationRepository> registrations,
                                    @Value("${devcontext.frontend-origin:http://localhost:3000}") String frontendOrigin) {
        this.authorizedClients = authorizedClients;
        this.registrations = registrations;
        this.frontendOrigin = frontendOrigin;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        String registrationId = authentication instanceof OAuth2AuthenticationToken oauth
                ? oauth.getAuthorizedClientRegistrationId() : "github";
        String linkedEmail = (String) request.getSession().getAttribute(LINK_EMAIL);
        if (linkedEmail != null && !"github".equals(registrationId)) {
            OAuth2AuthorizedClientService clientService = authorizedClients.getIfAvailable();
            ClientRegistrationRepository registrationRepository = registrations.getIfAvailable();
            OAuth2AuthorizedClient providerClient = clientService == null ? null : clientService.loadAuthorizedClient(registrationId, authentication.getName());
            if (providerClient != null && registrationRepository != null) {
                var registration = registrationRepository.findByRegistrationId(registrationId);
                clientService.saveAuthorizedClient(new OAuth2AuthorizedClient(registration, linkedEmail,
                        providerClient.getAccessToken(), providerClient.getRefreshToken()),
                        new UsernamePasswordAuthenticationToken(linkedEmail, "LINKED_PROVIDER",
                                List.of(new SimpleGrantedAuthority("ROLE_USER"))));
            }
            Authentication restored = new UsernamePasswordAuthenticationToken(linkedEmail, "LINKED_PROVIDER",
                    List.of(new SimpleGrantedAuthority("ROLE_USER")));
            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(restored);
            SecurityContextHolder.setContext(context);
            request.getSession().setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);
            request.getSession().removeAttribute(LINK_EMAIL);
            response.sendRedirect(frontendOrigin + "/integrations");
            return;
        }
        response.sendRedirect(frontendOrigin + "/onboarding");
    }
}

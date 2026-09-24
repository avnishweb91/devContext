package com.devcontext.security;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.HashMap;
import java.util.Map;

@Service
public class GitHubOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {
    private final DefaultOAuth2UserService delegate = new DefaultOAuth2UserService();
    private final RestClient client;

    public GitHubOAuth2UserService(RestClient.Builder builder) {
        this.client = builder.build();
    }

    @Override
    public OAuth2User loadUser(OAuth2UserRequest request) throws OAuth2AuthenticationException {
        OAuth2User user = delegate.loadUser(request);
        if (!"github".equals(request.getClientRegistration().getRegistrationId())
                || hasEmail(user.getAttributes())) return user;
        try {
            JsonNode emails = client.get().uri("https://api.github.com/user/emails")
                    .headers(headers -> headers.setBearerAuth(request.getAccessToken().getTokenValue()))
                    .retrieve().body(JsonNode.class);
            String email = selectEmail(emails);
            if (email == null) return user;
            Map<String, Object> attributes = new HashMap<>(user.getAttributes());
            attributes.put("email", email);
            String nameAttribute = request.getClientRegistration().getProviderDetails().getUserInfoEndpoint().getUserNameAttributeName();
            return new DefaultOAuth2User(user.getAuthorities(), attributes, nameAttribute);
        } catch (RuntimeException ignored) {
            // Keep the original profile; onboarding will return a clear identity error if no email is available.
            return user;
        }
    }

    private boolean hasEmail(Map<String, Object> attributes) {
        Object email = attributes.get("email");
        return email instanceof String value && !value.isBlank();
    }

    private String selectEmail(JsonNode emails) {
        if (emails == null || !emails.isArray()) return null;
        String fallback = null;
        for (JsonNode item : emails) {
            if (!item.path("verified").asBoolean(false)) continue;
            String email = item.path("email").asText("");
            if (fallback == null && !email.isBlank()) fallback = email;
            if (item.path("primary").asBoolean(false) && !email.isBlank()) return email;
        }
        return fallback;
    }
}

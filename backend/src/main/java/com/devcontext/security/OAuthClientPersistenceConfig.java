package com.devcontext.security;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.client.JdbcOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;

@Configuration
@ConditionalOnProperty(name = "devcontext.security.enabled", havingValue = "true")
public class OAuthClientPersistenceConfig {
    @Bean
    OAuth2AuthorizedClientService authorizedClientService(JdbcTemplate jdbcTemplate,
                                                          ClientRegistrationRepository registrations) {
        return new JdbcOAuth2AuthorizedClientService(jdbcTemplate, registrations);
    }
}

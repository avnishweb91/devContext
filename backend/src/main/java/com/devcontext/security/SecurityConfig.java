package com.devcontext.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {
    private final GitHubOAuth2UserService githubUserService;
    private final OAuth2LinkSuccessHandler oauth2LinkSuccessHandler;

    public SecurityConfig(GitHubOAuth2UserService githubUserService, OAuth2LinkSuccessHandler oauth2LinkSuccessHandler) {
        this.githubUserService = githubUserService;
        this.oauth2LinkSuccessHandler = oauth2LinkSuccessHandler;
    }

    @Value("${devcontext.security.enabled:false}")
    private boolean securityEnabled;

    @Value("${devcontext.frontend-origin:http://localhost:3000}")
    private String frontendOrigin;

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()));

        if (!securityEnabled) {
            http.authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
            return http.build();
        }

        http.authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health", "/actuator/info", "/oauth2/**", "/login/**", "/api/webhooks/**").permitAll()
                        .anyRequest().authenticated())
                .oauth2Login(login -> login
                        .userInfoEndpoint(userInfo -> userInfo.userService(githubUserService))
                        .successHandler(oauth2LinkSuccessHandler))
                .logout(logout -> logout.logoutSuccessUrl(frontendOrigin))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED));
        return http.build();
    }
}

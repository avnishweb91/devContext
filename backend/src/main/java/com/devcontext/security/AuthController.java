package com.devcontext.security;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    @GetMapping("/me")
    public CurrentUser currentUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return new CurrentUser(false, null, null, List.of());
        }
        return new CurrentUser(true, authentication.getName(), authentication.getPrincipal().toString(),
                authentication.getAuthorities().stream().map(Object::toString).toList());
    }

    public record CurrentUser(boolean authenticated, String subject, String principal, List<String> authorities) {}
}


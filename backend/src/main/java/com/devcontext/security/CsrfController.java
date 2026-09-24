package com.devcontext.security;

import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class CsrfController {
    @GetMapping("/csrf")
    public CsrfResponse token(CsrfToken token) {
        return new CsrfResponse(token == null ? "" : token.getToken());
    }

    public record CsrfResponse(String token) {}
}

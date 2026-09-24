package com.devcontext.workspace;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.core.Authentication;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@RestController
@RequestMapping("/api/onboarding")
public class OnboardingController {
    private final OnboardingService service;
    private final WorkspaceAccessService access;

    public OnboardingController(OnboardingService service, WorkspaceAccessService access) {
        this.service = service;
        this.access = access;
    }

    @PostMapping("/workspaces")
    @ResponseStatus(HttpStatus.CREATED)
    public OnboardingResponse createWorkspace(@Valid @RequestBody CreateWorkspaceRequest request, Authentication authentication) {
        String ownerEmail = request.ownerEmail();
        if (authentication != null && authentication.isAuthenticated()
                && !"anonymousUser".equals(authentication.getName())) {
            String authenticatedEmail = access.identityEmail(authentication);
            if (!authenticatedEmail.equalsIgnoreCase(ownerEmail)) {
                throw new ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN,
                        "Owner email must match the signed-in identity");
            }
            ownerEmail = authenticatedEmail;
        }
        return service.createWorkspace(request.companyName(), request.ownerName(), ownerEmail);
    }

    public record CreateWorkspaceRequest(
            @NotBlank @Size(max = 180) String companyName,
            @NotBlank @Size(max = 180) String ownerName,
            @NotBlank @Email @Size(max = 320) String ownerEmail) {}

    public record OnboardingResponse(UUID workspaceId, String companyName, UUID ownerId, String ownerEmail, String role) {}
}

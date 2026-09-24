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

import java.util.UUID;

@RestController
@RequestMapping("/api/onboarding")
public class OnboardingController {
    private final OnboardingService service;

    public OnboardingController(OnboardingService service) { this.service = service; }

    @PostMapping("/workspaces")
    @ResponseStatus(HttpStatus.CREATED)
    public OnboardingResponse createWorkspace(@Valid @RequestBody CreateWorkspaceRequest request) {
        return service.createWorkspace(request.companyName(), request.ownerName(), request.ownerEmail());
    }

    public record CreateWorkspaceRequest(
            @NotBlank @Size(max = 180) String companyName,
            @NotBlank @Size(max = 180) String ownerName,
            @NotBlank @Email @Size(max = 320) String ownerEmail) {}

    public record OnboardingResponse(UUID workspaceId, String companyName, UUID ownerId, String ownerEmail, String role) {}
}


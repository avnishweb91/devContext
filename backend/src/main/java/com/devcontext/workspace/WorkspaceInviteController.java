package com.devcontext.workspace;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Locale;
import java.util.UUID;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.FORBIDDEN;

@RestController
@RequestMapping("/api")
public class WorkspaceInviteController {
    private final WorkspaceAccessService access;
    private final WorkspaceInviteRepository invites;
    private final AppUserRepository users;
    private final WorkspaceMembershipRepository memberships;
    private final SecureRandom random = new SecureRandom();

    public WorkspaceInviteController(WorkspaceAccessService access, WorkspaceInviteRepository invites,
                                     AppUserRepository users, WorkspaceMembershipRepository memberships) {
        this.access = access;
        this.invites = invites;
        this.users = users;
        this.memberships = memberships;
    }

    @PostMapping("/workspaces/{workspaceId}/invites")
    @ResponseStatus(HttpStatus.CREATED)
    public InviteResponse create(@PathVariable UUID workspaceId, @Valid @RequestBody CreateInviteRequest request,
                                 Authentication authentication) {
        access.requireAdmin(workspaceId, authentication);
        WorkspaceMembership.Role role = parseRole(request.role());
        byte[] tokenBytes = new byte[32];
        random.nextBytes(tokenBytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
        WorkspaceInvite invite = invites.save(new WorkspaceInvite(workspaceId, request.email(), role, hash(token),
                Instant.now().plus(7, ChronoUnit.DAYS)));
        return new InviteResponse(invite.getId(), invite.getEmail(), invite.getRole().name(), token, invite.getExpiresAt());
    }

    @PostMapping("/invites/accept")
    @Transactional
    public AcceptResponse accept(@Valid @RequestBody AcceptInviteRequest request, Authentication authentication) {
        String email = access.identityEmail(authentication);
        WorkspaceInvite invite = invites.findByTokenHash(hash(request.token()))
                .filter(candidate -> candidate.activeAt(Instant.now()))
                .orElseThrow(() -> new ResponseStatusException(BAD_REQUEST, "Invite is invalid or expired"));
        if (!invite.getEmail().equalsIgnoreCase(email)) {
            throw new ResponseStatusException(FORBIDDEN, "Invite email must match the signed-in identity");
        }
        AppUser user = users.findByEmailIgnoreCase(email).orElseGet(() -> users.save(new AppUser(email, displayName(authentication))));
        if (!memberships.existsByWorkspaceIdAndUserId(invite.getWorkspaceId(), user.getId())) {
            memberships.save(new WorkspaceMembership(invite.getWorkspaceId(), user.getId(), invite.getRole()));
        }
        invite.accept();
        invites.save(invite);
        return new AcceptResponse(invite.getWorkspaceId(), invite.getRole().name());
    }

    private WorkspaceMembership.Role parseRole(String value) {
        try {
            WorkspaceMembership.Role role = WorkspaceMembership.Role.valueOf(value.trim().toUpperCase(Locale.ROOT));
            if (role == WorkspaceMembership.Role.OWNER) throw new IllegalArgumentException();
            return role;
        } catch (Exception exception) {
            throw new ResponseStatusException(BAD_REQUEST, "Invite role is invalid");
        }
    }

    private String displayName(Authentication authentication) {
        if (authentication.getPrincipal() instanceof OAuth2User oauthUser) {
            String name = oauthUser.getAttribute("name");
            if (name != null && !name.isBlank()) return name;
        }
        return authentication.getName();
    }

    private String hash(String token) {
        try { return HexFormatHolder.hex(MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8))); }
        catch (Exception exception) { throw new IllegalStateException("Could not hash invite token", exception); }
    }

    private static final class HexFormatHolder {
        static String hex(byte[] bytes) { return java.util.HexFormat.of().formatHex(bytes); }
    }

    public record CreateInviteRequest(@NotBlank @Email @Size(max = 320) String email,
                                      @NotBlank @Size(max = 40) String role) {}
    public record AcceptInviteRequest(@NotBlank @Size(max = 200) String token) {}
    public record InviteResponse(UUID inviteId, String email, String role, String token, Instant expiresAt) {}
    public record AcceptResponse(UUID workspaceId, String role) {}
}

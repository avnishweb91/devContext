package com.devcontext.workspace;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@Service
public class WorkspaceAccessService {
    private final AppUserRepository users;
    private final WorkspaceMembershipRepository memberships;

    public WorkspaceAccessService(AppUserRepository users, WorkspaceMembershipRepository memberships) {
        this.users = users;
        this.memberships = memberships;
    }

    public AppUser requireMember(UUID workspaceId, Authentication authentication) {
        AppUser user = currentUser(authentication);
        if (!memberships.existsByWorkspaceIdAndUserId(workspaceId, user.getId())) {
            throw new ResponseStatusException(FORBIDDEN, "User is not a member of this workspace");
        }
        return user;
    }

    public AppUser currentUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(UNAUTHORIZED, "Authentication required");
        }
        String email = authentication.getPrincipal() instanceof OAuth2User oauthUser
                ? oauthUser.getAttribute("email")
                : authentication.getName();
        if (email == null || email.isBlank()) {
            throw new ResponseStatusException(UNAUTHORIZED, "Authenticated identity has no email");
        }
        AppUser user = users.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResponseStatusException(FORBIDDEN, "User is not onboarded to this workspace"));
        return user;
    }
}

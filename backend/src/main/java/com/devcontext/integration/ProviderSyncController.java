package com.devcontext.integration;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/workspaces/{workspaceId}/integrations")
public class ProviderSyncController {
    private final ProviderSyncService syncService;

    public ProviderSyncController(ProviderSyncService syncService) {
        this.syncService = syncService;
    }

    @PostMapping("/{provider}/sync")
    public ProviderSyncService.SyncResult sync(@PathVariable UUID workspaceId, @PathVariable String provider,
                                               Authentication authentication) {
        return syncService.sync(workspaceId, provider, authentication);
    }
}

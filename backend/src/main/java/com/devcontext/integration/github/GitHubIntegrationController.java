package com.devcontext.integration.github;

import com.devcontext.workspace.WorkspaceAccessService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/integrations/github")
public class GitHubIntegrationController {
    private final OAuth2AuthorizedClientService authorizedClients;
    private final GitHubApiClient github;
    private final WorkspaceAccessService access;
    private final ConnectedRepositoryRepository repositories;
    private final PullRequestRecordRepository pullRequests;
    private final GitHubSyncJobRepository jobs;
    private final GitHubSyncService syncService;

    public GitHubIntegrationController(OAuth2AuthorizedClientService authorizedClients, GitHubApiClient github,
                                       WorkspaceAccessService access, ConnectedRepositoryRepository repositories,
                                       PullRequestRecordRepository pullRequests, GitHubSyncJobRepository jobs,
                                       GitHubSyncService syncService) {
        this.authorizedClients = authorizedClients;
        this.github = github;
        this.access = access;
        this.repositories = repositories;
        this.pullRequests = pullRequests;
        this.jobs = jobs;
        this.syncService = syncService;
    }

    @GetMapping("/repositories")
    public List<RepositorySummary> repositories(Authentication authentication) {
        OAuth2AuthorizedClient client = authorizedClient(authentication);
        return github.repositories(client).stream()
                .map(repo -> new RepositorySummary(repo.externalId(), repo.name(), repo.fullName(), repo.privateRepository(), repo.url()))
                .toList();
    }

    @PostMapping("/workspaces/{workspaceId}/sync")
    public SyncResult sync(@PathVariable UUID workspaceId, Authentication authentication) {
        access.requireMember(workspaceId, authentication);
        authorizedClient(authentication);
        GitHubSyncJob job = syncService.queue(workspaceId, authentication.getName());
        return new SyncResult(job.getId(), 0, 0, job.getStatus(), job.getErrorMessage());
    }

    @GetMapping("/workspaces/{workspaceId}/sync-jobs/{jobId}")
    public SyncResult syncStatus(@PathVariable UUID workspaceId, @PathVariable UUID jobId, Authentication authentication) {
        access.requireMember(workspaceId, authentication);
        GitHubSyncJob job = jobs.findById(jobId).filter(candidate -> candidate.getWorkspaceId().equals(workspaceId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Sync job not found"));
        return new SyncResult(job.getId(), job.getRepositories(), job.getPullRequests(), job.getStatus(), job.getErrorMessage());
    }

    @PostMapping("/workspaces/{workspaceId}/sync-jobs/{jobId}/retry")
    public SyncResult retry(@PathVariable UUID workspaceId, @PathVariable UUID jobId, Authentication authentication) {
        access.requireMember(workspaceId, authentication);
        authorizedClient(authentication);
        GitHubSyncJob job = syncService.retry(workspaceId, jobId, authentication.getName());
        return new SyncResult(job.getId(), 0, 0, job.getStatus(), job.getErrorMessage());
    }

    private OAuth2AuthorizedClient authorizedClient(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sign in with GitHub first");
        }
        OAuth2AuthorizedClient client = authorizedClients.loadAuthorizedClient("github", authentication.getName());
        if (client == null || client.getAccessToken() == null) {
            throw new ResponseStatusException(HttpStatus.PRECONDITION_REQUIRED, "GitHub is not connected");
        }
        return client;
    }

    public record RepositorySummary(long id, String name, String fullName, boolean privateRepository, String url) {}
    public record SyncResult(UUID jobId, int repositories, int pullRequests, String status, String errorMessage) {}
}

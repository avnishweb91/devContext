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

    public GitHubIntegrationController(OAuth2AuthorizedClientService authorizedClients, GitHubApiClient github,
                                       WorkspaceAccessService access, ConnectedRepositoryRepository repositories,
                                       PullRequestRecordRepository pullRequests) {
        this.authorizedClients = authorizedClients;
        this.github = github;
        this.access = access;
        this.repositories = repositories;
        this.pullRequests = pullRequests;
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
        OAuth2AuthorizedClient client = authorizedClient(authentication);
        int repositoryCount = 0;
        int pullRequestCount = 0;
        for (GitHubApiClient.GitHubRepositoryData remote : github.repositories(client)) {
            ConnectedRepository repository = repositories.findByWorkspaceIdAndExternalId(workspaceId, remote.externalId())
                    .orElseGet(() -> new ConnectedRepository(workspaceId, remote.externalId(), remote.name(), remote.fullName(), remote.privateRepository(), remote.url()));
            ConnectedRepository connected = repositories.save(repository);
            repositoryCount++;
            for (GitHubApiClient.GitHubPullRequestData remotePr : github.pullRequests(client, remote.fullName())) {
                PullRequestRecord record = pullRequests.findByRepositoryIdAndExternalNumber(connected.getId(), remotePr.number())
                        .orElseGet(() -> new PullRequestRecord(connected.getId(), remotePr.number(), remotePr.title(), remotePr.authorLogin(), remotePr.state(), remotePr.url()));
                pullRequests.save(record);
                pullRequestCount++;
            }
        }
        return new SyncResult(repositoryCount, pullRequestCount, "completed");
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
    public record SyncResult(int repositories, int pullRequests, String status) {}
}

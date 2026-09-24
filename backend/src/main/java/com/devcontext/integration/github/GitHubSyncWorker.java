package com.devcontext.integration.github;

import org.springframework.scheduling.annotation.Async;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class GitHubSyncWorker {
    private final GitHubSyncJobRepository jobs;
    private final OAuth2AuthorizedClientService authorizedClients;
    private final GitHubApiClient github;
    private final ConnectedRepositoryRepository repositories;
    private final PullRequestRecordRepository pullRequests;

    public GitHubSyncWorker(GitHubSyncJobRepository jobs, OAuth2AuthorizedClientService authorizedClients,
                            GitHubApiClient github, ConnectedRepositoryRepository repositories,
                            PullRequestRecordRepository pullRequests) {
        this.jobs = jobs;
        this.authorizedClients = authorizedClients;
        this.github = github;
        this.repositories = repositories;
        this.pullRequests = pullRequests;
    }

    @Async("integrationTaskExecutor")
    @Transactional
    public void synchronize(UUID jobId, UUID workspaceId, String principalName) {
        GitHubSyncJob job = jobs.findById(jobId).orElse(null);
        if (job == null) return;
        job.start();
        jobs.save(job);
        try {
            OAuth2AuthorizedClient client = authorizedClients.loadAuthorizedClient("github", principalName);
            if (client == null || client.getAccessToken() == null) throw new IllegalStateException("GitHub is not connected");
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
            job.complete(repositoryCount, pullRequestCount);
        } catch (Exception exception) {
            job.fail(exception.getMessage());
        }
        jobs.save(job);
    }
}

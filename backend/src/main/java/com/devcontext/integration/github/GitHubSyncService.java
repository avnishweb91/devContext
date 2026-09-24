package com.devcontext.integration.github;

import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Service
public class GitHubSyncService {
    private final GitHubSyncJobRepository jobs;
    private final GitHubSyncWorker worker;

    public GitHubSyncService(GitHubSyncJobRepository jobs, GitHubSyncWorker worker) {
        this.jobs = jobs;
        this.worker = worker;
    }

    public GitHubSyncJob queue(UUID workspaceId, String principalName) {
        GitHubSyncJob job = jobs.save(new GitHubSyncJob(workspaceId, principalName));
        worker.synchronize(job.getId(), workspaceId, principalName);
        return job;
    }

    public GitHubSyncJob retry(UUID workspaceId, UUID jobId, String principalName) {
        GitHubSyncJob previous = jobs.findById(jobId)
                .filter(job -> job.getWorkspaceId().equals(workspaceId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Sync job not found"));
        if (!"FAILED".equals(previous.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Only failed sync jobs can be retried");
        }
        return queue(workspaceId, principalName);
    }
}

package com.devcontext.integration.github;

import org.springframework.stereotype.Service;

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
}

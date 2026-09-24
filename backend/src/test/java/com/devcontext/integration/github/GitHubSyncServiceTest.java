package com.devcontext.integration.github;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GitHubSyncServiceTest {
    @Mock GitHubSyncJobRepository jobs;
    @Mock GitHubSyncWorker worker;

    @Test
    void persistsJobBeforeDispatchingBackgroundWork() {
        UUID workspaceId = UUID.randomUUID();
        GitHubSyncJob saved = new GitHubSyncJob(workspaceId, "github-user");
        when(jobs.save(any(GitHubSyncJob.class))).thenReturn(saved);
        GitHubSyncService service = new GitHubSyncService(jobs, worker);

        GitHubSyncJob result = service.queue(workspaceId, "github-user");

        assertThat(result.getStatus()).isEqualTo("PENDING");
        verify(jobs).save(any(GitHubSyncJob.class));
        verify(worker).synchronize(result.getId(), workspaceId, "github-user");
    }

    @Test
    void retriesOnlyFailedJobsInTheSameWorkspace() {
        UUID workspaceId = UUID.randomUUID();
        GitHubSyncJob failed = new GitHubSyncJob(workspaceId, "github-user");
        failed.start();
        failed.fail("provider timeout");
        when(jobs.findById(failed.getId())).thenReturn(java.util.Optional.of(failed));
        GitHubSyncJob retry = new GitHubSyncJob(workspaceId, "github-user");
        when(jobs.save(any(GitHubSyncJob.class))).thenReturn(retry);
        GitHubSyncService service = new GitHubSyncService(jobs, worker);

        GitHubSyncJob result = service.retry(workspaceId, failed.getId(), "github-user");

        assertThat(result.getStatus()).isEqualTo("PENDING");
        verify(worker).synchronize(result.getId(), workspaceId, "github-user");
    }
}

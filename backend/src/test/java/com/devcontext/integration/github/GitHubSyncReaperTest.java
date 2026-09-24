package com.devcontext.integration.github;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GitHubSyncReaperTest {
    @Mock GitHubSyncJobRepository jobs;
    @Mock GitHubSyncJob pending;
    @Mock GitHubSyncJob running;

    @Test
    void marksStalePendingAndRunningJobsFailed() {
        when(jobs.findByStatusAndCreatedAtBefore(eq("PENDING"), any())).thenReturn(List.of(pending));
        when(jobs.findByStatusAndStartedAtBefore(eq("RUNNING"), any())).thenReturn(List.of(running));
        GitHubSyncReaper reaper = new GitHubSyncReaper(jobs, 30);

        reaper.failStaleJobs();

        verify(pending).fail("GitHub synchronization timed out before starting");
        verify(running).fail("GitHub synchronization timed out");
    }
}

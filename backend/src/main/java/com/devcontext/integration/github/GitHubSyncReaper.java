package com.devcontext.integration.github;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;

@Component
@Profile("prod")
public class GitHubSyncReaper {
    private final GitHubSyncJobRepository jobs;
    private final Duration timeout;

    public GitHubSyncReaper(GitHubSyncJobRepository jobs,
                            @Value("${devcontext.integrations.github.reaper.timeout-minutes:30}") long timeoutMinutes) {
        this.jobs = jobs;
        this.timeout = Duration.ofMinutes(Math.max(1, timeoutMinutes));
    }

    @Scheduled(fixedDelayString = "${devcontext.integrations.github.reaper.delay-ms:300000}")
    @Transactional
    public void failStaleJobs() {
        Instant now = Instant.now();
        jobs.findByStatusAndCreatedAtBefore("PENDING", now.minus(timeout)).forEach(job -> job.fail("GitHub synchronization timed out before starting"));
        jobs.findByStatusAndStartedAtBefore("RUNNING", now.minus(timeout)).forEach(job -> job.fail("GitHub synchronization timed out"));
    }
}

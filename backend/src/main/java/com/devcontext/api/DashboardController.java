package com.devcontext.api;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api")
public class DashboardController {

    @GetMapping("/dashboard")
    public Dashboard dashboard() {
        return new Dashboard(
                new Metrics(24, 87, 7, 2),
                List.of(
                        new PullRequest("#1842", "Add rate-limit handling to payments API", "payments-service", "Riya S.", "VERIFIED", "All evidence checks passed"),
                        new PullRequest("#1839", "Refactor customer event pipeline", "event-stream", "Arjun K.", "ATTENTION", "2 checks missing"),
                        new PullRequest("#1832", "Update auth token refresh flow", "identity-service", "Neha P.", "CONFLICT", "Decision conflict")
                ),
                List.of(
                        new Memory("Decision: use async retries for payment webhooks", "#payments-platform", "18m ago"),
                        new Memory("Incident pattern detected in event-stream", "INC-221", "2h ago"),
                        new Memory("Documentation drift found in auth-service", "README vs API behavior", "Yesterday")
                ),
                new Risk(36, 78, 64, 91),
                Instant.now()
        );
    }

    @PostMapping("/verification/run")
    public VerificationResult runVerification() {
        return new VerificationResult("completed", 2, "Verification complete: two items need human review.", Instant.now());
    }

    public record Dashboard(Metrics metrics, List<PullRequest> pullRequests, List<Memory> memories, Risk risk, Instant generatedAt) {}
    public record Metrics(int openPullRequests, int contextCoverage, int unverifiedAiChanges, int activeIncidents) {}
    public record PullRequest(String id, String title, String repository, String author, String status, String summary) {}
    public record Memory(String title, String source, String age) {}
    public record Risk(int score, int testsAdded, int docsUpdated, int rollbackReady) {}
    public record VerificationResult(String status, int reviewItems, String message, Instant completedAt) {}
}


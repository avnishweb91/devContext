package com.devcontext.verification;

import com.devcontext.integration.github.ConnectedRepository;
import com.devcontext.integration.github.ConnectedRepositoryRepository;
import com.devcontext.integration.github.PullRequestRecord;
import com.devcontext.integration.github.PullRequestRecordRepository;
import com.devcontext.workspace.WorkspaceAccessService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/workspaces/{workspaceId}")
public class PullRequestVerificationController {
    private final WorkspaceAccessService access;
    private final ConnectedRepositoryRepository repositories;
    private final PullRequestRecordRepository pullRequests;
    private final VerificationRunRepository runs;

    public PullRequestVerificationController(WorkspaceAccessService access,
                                             ConnectedRepositoryRepository repositories,
                                             PullRequestRecordRepository pullRequests,
                                             VerificationRunRepository runs) {
        this.access = access;
        this.repositories = repositories;
        this.pullRequests = pullRequests;
        this.runs = runs;
    }

    @GetMapping("/pull-requests")
    public List<PullRequestSummary> pullRequests(@PathVariable UUID workspaceId, Authentication authentication) {
        access.requireMember(workspaceId, authentication);
        return repositories.findAllByWorkspaceIdOrderByFullName(workspaceId).stream()
                .flatMap(repository -> pullRequests.findAllByRepositoryIdOrderByUpdatedAtDesc(repository.getId()).stream()
                        .map(pr -> summary(repository, pr)))
                .toList();
    }

    @GetMapping("/verification-runs")
    public List<VerificationRun> verificationRuns(@PathVariable UUID workspaceId, Authentication authentication) {
        access.requireMember(workspaceId, authentication);
        return runs.findAllByWorkspaceIdOrderByCreatedAtDesc(workspaceId);
    }

    @PostMapping("/pull-requests/{pullRequestId}/verify")
    @Transactional
    public VerificationResponse verify(@PathVariable UUID workspaceId, @PathVariable UUID pullRequestId,
                                       Authentication authentication) {
        access.requireMember(workspaceId, authentication);
        PullRequestRecord pullRequest = pullRequests.findById(pullRequestId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pull request not found"));
        ConnectedRepository repository = repositories.findById(pullRequest.getRepositoryId())
                .filter(candidate -> candidate.getWorkspaceId().equals(workspaceId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pull request not found"));
        VerificationRun run = runs.save(new VerificationRun(workspaceId,
                repository.getFullName() + "#" + pullRequest.getExternalNumber(), "REVIEW_REQUIRED", 1));
        pullRequest.markVerification("REVIEW_REQUIRED");
        pullRequests.save(pullRequest);
        return new VerificationResponse(run.getId(), run.getStatus(), run.getReviewItems(),
                "Verification recorded; human evidence review is required before release.");
    }

    private PullRequestSummary summary(ConnectedRepository repository, PullRequestRecord pr) {
        return new PullRequestSummary(pr.getId(), repository.getFullName(), pr.getExternalNumber(), pr.getTitle(),
                pr.getAuthorLogin(), pr.getState(), pr.getUrl(), pr.getVerificationStatus());
    }

    public record PullRequestSummary(UUID id, String repository, int number, String title, String author,
                                     String state, String url, String verificationStatus) {}
    public record VerificationResponse(UUID runId, String status, int reviewItems, String message) {}
}

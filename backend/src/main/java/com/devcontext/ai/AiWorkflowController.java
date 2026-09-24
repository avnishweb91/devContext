package com.devcontext.ai;

import com.devcontext.workspace.WorkspaceAccessService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;

@RestController
@RequestMapping("/api/workspaces/{workspaceId}/ai")
public class AiWorkflowController {
    private final WorkspaceAccessService access;
    private final AiWorkflowService ai;
    private final AiReviewRecordRepository reviews;

    public AiWorkflowController(WorkspaceAccessService access, AiWorkflowService ai, AiReviewRecordRepository reviews) {
        this.access = access;
        this.ai = ai;
        this.reviews = reviews;
    }

    @PostMapping("/review-summary")
    public ReviewSummary reviewSummary(@PathVariable UUID workspaceId, @Valid @RequestBody ReviewRequest request,
                                       Authentication authentication) {
        access.requireMember(workspaceId, authentication);
        String summary = ai.summarize(request.title(), request.context());
        AiReviewRecord review = reviews.save(new AiReviewRecord(workspaceId, request.title(), digest(request.context()), summary,
                authentication == null ? "unknown" : authentication.getName()));
        return new ReviewSummary(review.getId(), review.getSummary(), review.getCreatedAt());
    }

    @GetMapping("/review-summaries")
    public List<AiReviewRecord> reviewSummaries(@PathVariable UUID workspaceId, Authentication authentication) {
        access.requireMember(workspaceId, authentication);
        return reviews.findAllByWorkspaceIdOrderByCreatedAtDesc(workspaceId);
    }

    private String digest(String context) {
        try {
            byte[] bytes = MessageDigest.getInstance("SHA-256").digest(context.getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder(64);
            for (byte value : bytes) result.append(String.format("%02x", value));
            return result.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    public record ReviewRequest(@NotBlank @Size(max = 300) String title, @NotBlank @Size(max = 12000) String context) {}
    public record ReviewSummary(UUID reviewId, String summary, java.time.Instant createdAt) {}
}

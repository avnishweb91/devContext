package com.devcontext.ai;

import com.devcontext.workspace.WorkspaceAccessService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/workspaces/{workspaceId}/ai")
public class AiWorkflowController {
    private final WorkspaceAccessService access;
    private final AiWorkflowService ai;

    public AiWorkflowController(WorkspaceAccessService access, AiWorkflowService ai) {
        this.access = access;
        this.ai = ai;
    }

    @PostMapping("/review-summary")
    public ReviewSummary reviewSummary(@PathVariable UUID workspaceId, @Valid @RequestBody ReviewRequest request,
                                       Authentication authentication) {
        access.requireMember(workspaceId, authentication);
        return new ReviewSummary(ai.summarize(request.title(), request.context()));
    }

    public record ReviewRequest(@NotBlank @Size(max = 300) String title, @NotBlank @Size(max = 12000) String context) {}
    public record ReviewSummary(String summary) {}
}

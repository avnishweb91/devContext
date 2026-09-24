package com.devcontext.ai;

import com.devcontext.workspace.WorkspaceAccessService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiWorkflowControllerTest {
    @Mock WorkspaceAccessService access;
    @Mock AiWorkflowService ai;
    @Mock AiReviewRecordRepository reviews;

    @Test
    void createsWorkspaceScopedReviewSummary() {
        UUID workspaceId = UUID.randomUUID();
        when(ai.summarize("Rate limit change", "Tests cover the retry path.")).thenReturn("Risk is low; verify rollback evidence.");
        when(reviews.save(any(AiReviewRecord.class))).thenAnswer(invocation -> invocation.getArgument(0));
        AiWorkflowController controller = new AiWorkflowController(access, ai, reviews);

        var result = controller.reviewSummary(workspaceId,
                new AiWorkflowController.ReviewRequest("Rate limit change", "Tests cover the retry path."),
                new UsernamePasswordAuthenticationToken("dev@example.com", "n/a"));

        assertThat(result.summary()).contains("Risk is low");
        verify(access).requireMember(eq(workspaceId), any());
        verify(ai).summarize("Rate limit change", "Tests cover the retry path.");
        verify(reviews).save(any(AiReviewRecord.class));
    }
}

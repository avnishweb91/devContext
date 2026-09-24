package com.devcontext.verification;

import com.devcontext.integration.github.ConnectedRepository;
import com.devcontext.integration.github.ConnectedRepositoryRepository;
import com.devcontext.integration.github.PullRequestRecord;
import com.devcontext.integration.github.PullRequestRecordRepository;
import com.devcontext.workspace.WorkspaceAccessService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PullRequestVerificationControllerTest {
    @Mock WorkspaceAccessService access;
    @Mock ConnectedRepositoryRepository repositories;
    @Mock PullRequestRecordRepository pullRequests;
    @Mock VerificationRunRepository runs;

    @Test
    void verifiesOnlyPullRequestsBelongingToTheRequestedWorkspace() {
        UUID workspaceId = UUID.randomUUID();
        UUID repositoryId = UUID.randomUUID();
        ConnectedRepository repository = new ConnectedRepository(workspaceId, 42L, "api", "acme/api", false, "https://github.com/acme/api");
        PullRequestRecord pullRequest = new PullRequestRecord(repositoryId, 7, "Improve retries", "dev", "open", "https://github.com/acme/api/pull/7");
        UUID pullRequestId = pullRequest.getId();
        when(pullRequests.findById(pullRequestId)).thenReturn(Optional.of(pullRequest));
        when(repositories.findById(repositoryId)).thenReturn(Optional.of(repository));
        when(runs.save(any(VerificationRun.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PullRequestVerificationController controller = new PullRequestVerificationController(access, repositories, pullRequests, runs);
        var result = controller.verify(workspaceId, pullRequestId,
                new UsernamePasswordAuthenticationToken("dev@example.com", "n/a"));

        assertThat(result.status()).isEqualTo("REVIEW_REQUIRED");
        assertThat(result.reviewItems()).isEqualTo(1);
        assertThat(pullRequest.getVerificationStatus()).isEqualTo("REVIEW_REQUIRED");
        verify(access).requireMember(eq(workspaceId), any());
        verify(pullRequests).save(pullRequest);
        ArgumentCaptor<VerificationRun> run = ArgumentCaptor.forClass(VerificationRun.class);
        verify(runs).save(run.capture());
        assertThat(run.getValue().getPullRequestRef()).isEqualTo("acme/api#7");
    }

}

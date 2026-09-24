package com.devcontext.api;

import com.devcontext.integration.github.ConnectedRepository;
import com.devcontext.integration.github.ConnectedRepositoryRepository;
import com.devcontext.integration.github.PullRequestRecord;
import com.devcontext.integration.github.PullRequestRecordRepository;
import com.devcontext.memory.EngineeringMemoryRepository;
import com.devcontext.workspace.WorkspaceAccessService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WorkspaceDashboardControllerTest {
    @Mock WorkspaceAccessService access;
    @Mock ConnectedRepositoryRepository repositories;
    @Mock PullRequestRecordRepository pullRequests;
    @Mock EngineeringMemoryRepository memories;

    @Test
    void buildsMetricsFromWorkspaceData() {
        UUID workspaceId = UUID.randomUUID();
        ConnectedRepository repository = new ConnectedRepository(workspaceId, 42, "api", "acme/api", false, "https://github.com/acme/api");
        PullRequestRecord pr = new PullRequestRecord(repository.getId(), 7, "Fix retries", "dev", "open", "https://github.com/acme/api/pull/7");
        when(repositories.findAllByWorkspaceIdOrderByFullName(workspaceId)).thenReturn(List.of(repository));
        when(pullRequests.findAllByRepositoryIdOrderByUpdatedAtDesc(repository.getId())).thenReturn(List.of(pr));
        when(memories.findAllByWorkspaceIdOrderByCreatedAtDesc(workspaceId)).thenReturn(List.of());

        var result = new WorkspaceDashboardController(access, repositories, pullRequests, memories).dashboard(workspaceId,
                new UsernamePasswordAuthenticationToken("dev@example.com", "n/a", List.of()));

        assertThat(result.metrics().openPullRequests()).isEqualTo(1);
        assertThat(result.metrics().unverifiedAiChanges()).isEqualTo(1);
        assertThat(result.pullRequests()).singleElement().satisfies(item -> assertThat(item.title()).isEqualTo("Fix retries"));
        verify(access).requireMember(eq(workspaceId), any());
    }
}

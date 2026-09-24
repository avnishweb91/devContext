package com.devcontext.api;

import com.devcontext.integration.github.ConnectedRepository;
import com.devcontext.integration.github.ConnectedRepositoryRepository;
import com.devcontext.integration.github.PullRequestRecord;
import com.devcontext.integration.github.PullRequestRecordRepository;
import com.devcontext.incident.IncidentRepository;
import com.devcontext.memory.EngineeringMemory;
import com.devcontext.memory.EngineeringMemoryRepository;
import com.devcontext.workspace.WorkspaceAccessService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/workspaces/{workspaceId}/dashboard")
public class WorkspaceDashboardController {
    private final WorkspaceAccessService access;
    private final ConnectedRepositoryRepository repositories;
    private final PullRequestRecordRepository pullRequests;
    private final EngineeringMemoryRepository memories;
    private final IncidentRepository incidents;

    public WorkspaceDashboardController(WorkspaceAccessService access, ConnectedRepositoryRepository repositories,
                                        PullRequestRecordRepository pullRequests, EngineeringMemoryRepository memories,
                                        IncidentRepository incidents) {
        this.access = access;
        this.repositories = repositories;
        this.pullRequests = pullRequests;
        this.memories = memories;
        this.incidents = incidents;
    }

    @GetMapping
    public Dashboard dashboard(@PathVariable UUID workspaceId, Authentication authentication) {
        access.requireMember(workspaceId, authentication);
        List<ConnectedRepository> connected = repositories.findAllByWorkspaceIdOrderByFullName(workspaceId);
        List<PullRequestView> prViews = connected.stream()
                .flatMap(repository -> pullRequests.findAllByRepositoryIdOrderByUpdatedAtDesc(repository.getId()).stream()
                        .map(pr -> view(repository, pr)))
                .limit(20)
                .toList();
        List<EngineeringMemory> workspaceMemories = memories.findAllByWorkspaceIdOrderByCreatedAtDesc(workspaceId);
        int open = (int) prViews.stream().filter(pr -> "open".equalsIgnoreCase(pr.state())).count();
        int pending = (int) prViews.stream().filter(pr -> "PENDING".equals(pr.verificationStatus())).count();
        int coverage = connected.isEmpty() ? 0 : Math.min(100, (int) Math.round((workspaceMemories.size() * 100.0) / Math.max(1, connected.size())));
        int activeIncidents = (int) incidents.countByWorkspaceIdAndStatusIn(workspaceId, List.of("OPEN", "ACKNOWLEDGED"));
        return new Dashboard(new Metrics(open, coverage, pending, activeIncidents), prViews,
                workspaceMemories.stream().limit(10).map(this::memory).toList(), Instant.now());
    }

    private PullRequestView view(ConnectedRepository repository, PullRequestRecord pr) {
        return new PullRequestView(pr.getId(), repository.getFullName(), pr.getExternalNumber(), pr.getTitle(),
                pr.getAuthorLogin(), pr.getState(), pr.getUrl(), pr.getVerificationStatus());
    }

    private MemoryView memory(EngineeringMemory memory) {
        return new MemoryView(memory.getId(), memory.getTitle(), memory.getSource(), memory.getCreatedAt());
    }

    public record Dashboard(Metrics metrics, List<PullRequestView> pullRequests, List<MemoryView> memories, Instant generatedAt) {}
    public record Metrics(int openPullRequests, int contextCoverage, int unverifiedAiChanges, int activeIncidents) {}
    public record PullRequestView(UUID id, String repository, int number, String title, String author, String state,
                                  String url, String verificationStatus) {}
    public record MemoryView(UUID id, String title, String source, Instant createdAt) {}
}

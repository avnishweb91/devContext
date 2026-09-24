package com.devcontext.integration.github;

import com.devcontext.workspace.Workspace;
import com.devcontext.workspace.WorkspaceRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class GitHubPersistenceTest {
    @Autowired WorkspaceRepository workspaces;
    @Autowired ConnectedRepositoryRepository repositories;
    @Autowired PullRequestRecordRepository pullRequests;

    @Test
    void persistsRepositoryAndPullRequestWithinWorkspace() {
        Workspace workspace = workspaces.save(new Workspace("Acme Engineering"));
        ConnectedRepository repository = repositories.save(new ConnectedRepository(
                workspace.getId(), 12345L, "platform", "acme/platform", true, "https://github.com/acme/platform"));
        pullRequests.save(new PullRequestRecord(repository.getId(), 42, "Improve retry policy", "dev-user", "open", "https://github.com/acme/platform/pull/42"));

        assertThat(repositories.findAllByWorkspaceIdOrderByFullName(workspace.getId())).hasSize(1);
        assertThat(pullRequests.findAllByRepositoryIdOrderByUpdatedAtDesc(repository.getId())).singleElement()
                .extracting(PullRequestRecord::getTitle).isEqualTo("Improve retry policy");
    }
}


package com.devcontext.integration.github;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PullRequestRecordRepository extends JpaRepository<PullRequestRecord, UUID> {
    Optional<PullRequestRecord> findByRepositoryIdAndExternalNumber(UUID repositoryId, int externalNumber);
    List<PullRequestRecord> findAllByRepositoryIdOrderByUpdatedAtDesc(UUID repositoryId);
}

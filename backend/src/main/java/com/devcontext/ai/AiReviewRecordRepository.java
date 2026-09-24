package com.devcontext.ai;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AiReviewRecordRepository extends JpaRepository<AiReviewRecord, UUID> {
    List<AiReviewRecord> findAllByWorkspaceIdOrderByCreatedAtDesc(UUID workspaceId);
}

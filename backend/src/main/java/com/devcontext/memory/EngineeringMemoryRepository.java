package com.devcontext.memory;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface EngineeringMemoryRepository extends JpaRepository<EngineeringMemory, UUID> {
    List<EngineeringMemory> findAllByWorkspaceIdOrderByCreatedAtDesc(UUID workspaceId);
}

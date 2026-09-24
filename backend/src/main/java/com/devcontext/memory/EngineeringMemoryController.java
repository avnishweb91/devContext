package com.devcontext.memory;

import com.devcontext.workspace.WorkspaceAccessService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/workspaces/{workspaceId}/memories")
public class EngineeringMemoryController {
    private final WorkspaceAccessService access;
    private final EngineeringMemoryRepository memories;

    public EngineeringMemoryController(WorkspaceAccessService access, EngineeringMemoryRepository memories) {
        this.access = access;
        this.memories = memories;
    }

    @GetMapping
    public List<EngineeringMemory> list(@PathVariable UUID workspaceId, Authentication authentication) {
        access.requireMember(workspaceId, authentication);
        return memories.findAllByWorkspaceIdOrderByCreatedAtDesc(workspaceId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EngineeringMemory create(@PathVariable UUID workspaceId, @Valid @RequestBody CreateMemoryRequest request,
                                    Authentication authentication) {
        access.requireMember(workspaceId, authentication);
        return memories.save(new EngineeringMemory(workspaceId, request.title(), request.source(), request.content(), request.sourceUrl()));
    }

    public record CreateMemoryRequest(
            @NotBlank @Size(max = 300) String title,
            @NotBlank @Size(max = 180) String source,
            @NotBlank String content,
            @Size(max = 2000) String sourceUrl) {}
}

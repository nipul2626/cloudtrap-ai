package com.cloudtrap.api.workspace.dto;

import com.cloudtrap.api.workspace.entity.Workspace;

import java.time.Instant;
import java.util.UUID;

public record WorkspaceResponse(
        UUID id,
        String name,
        String slug,
        UUID createdBy,
        Instant createdAt,
        Instant updatedAt
) {

    public static WorkspaceResponse from(Workspace workspace) {
        return new WorkspaceResponse(
                workspace.getId(),
                workspace.getName(),
                workspace.getSlug(),
                workspace.getCreatedBy(),
                workspace.getCreatedAt(),
                workspace.getUpdatedAt()
        );
    }
}
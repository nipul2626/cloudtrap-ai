package com.cloudtrap.api.workspace.dto;

import org.springframework.data.domain.Page;

import java.util.List;

public record WorkspacePageResponse(
        List<WorkspaceResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last
) {

    public static WorkspacePageResponse from(
            Page<WorkspaceResponse> result
    ) {
        return new WorkspacePageResponse(
                result.getContent(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages(),
                result.isFirst(),
                result.isLast()
        );
    }
}
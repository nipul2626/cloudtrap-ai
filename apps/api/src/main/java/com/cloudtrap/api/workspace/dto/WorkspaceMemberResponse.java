
package com.cloudtrap.api.workspace.dto;

import com.cloudtrap.api.identity.entity.User;
import com.cloudtrap.api.workspace.entity.WorkspaceMembership;

import java.time.Instant;
import java.util.UUID;

public record WorkspaceMemberResponse(
        UUID userId,
        String email,
        String displayName,
        String role,
        Instant joinedAt
) {
    public static WorkspaceMemberResponse from(
            WorkspaceMembership membership,
            User user
    ) {
        return new WorkspaceMemberResponse(
                user.getId(),
                user.getEmail(),
                user.getDisplayName(),
                membership.getRole(),
                membership.getCreatedAt()
        );
    }
}

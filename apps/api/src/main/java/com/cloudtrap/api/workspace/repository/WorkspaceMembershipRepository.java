package com.cloudtrap.api.workspace.repository;

import com.cloudtrap.api.workspace.entity.WorkspaceMembership;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * Data-access operations for workspace memberships.
 *
 * Authorization decisions and membership-management rules belong
 * in the service layer. Use pagination for potentially large
 * membership collections.
 */
public interface WorkspaceMembershipRepository
        extends JpaRepository<WorkspaceMembership, UUID> {

    /**
     * Finds a specific user's membership in a workspace.
     */
    Optional<WorkspaceMembership> findByWorkspaceIdAndUserId(
            UUID workspaceId,
            UUID userId
    );

    /**
     * Checks whether a user belongs to a workspace.
     */
    boolean existsByWorkspaceIdAndUserId(
            UUID workspaceId,
            UUID userId
    );

    /**
     * Retrieves workspace members in pages.
     */
    Page<WorkspaceMembership> findAllByWorkspaceId(
            UUID workspaceId,
            Pageable pageable
    );

    /**
     * Retrieves a user's workspace memberships in pages.
     */
    Page<WorkspaceMembership> findAllByUserId(
            UUID userId,
            Pageable pageable
    );

    /**
     * Counts the memberships belonging to a workspace.
     */
    long countByWorkspaceId(UUID workspaceId);
}
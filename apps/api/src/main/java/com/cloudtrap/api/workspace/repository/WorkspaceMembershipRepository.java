
package com.cloudtrap.api.workspace.repository;

import com.cloudtrap.api.workspace.entity.WorkspaceMembership;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WorkspaceMembershipRepository
        extends JpaRepository<WorkspaceMembership, UUID> {

    Optional<WorkspaceMembership> findByWorkspaceIdAndUserId(
            UUID workspaceId,
            UUID userId
    );

    boolean existsByWorkspaceIdAndUserId(
            UUID workspaceId,
            UUID userId
    );

    Page<WorkspaceMembership> findAllByWorkspaceId(
            UUID workspaceId,
            Pageable pageable
    );

    Page<WorkspaceMembership> findAllByUserId(
            UUID userId,
            Pageable pageable
    );

    long countByWorkspaceId(UUID workspaceId);

    List<WorkspaceMembership> findAllByWorkspaceIdOrderByCreatedAtAscIdAsc(
            UUID workspaceId
    );

    long countByWorkspaceIdAndRole(UUID workspaceId, String role);

    void deleteAllByWorkspaceId(UUID workspaceId);

    @Query("""
            select m
            from WorkspaceMembership m
            where m.workspaceId = :workspaceId
              and m.userId = :userId
            """)
    Optional<WorkspaceMembership> findMembership(
            @Param("workspaceId") UUID workspaceId,
            @Param("userId") UUID userId
    );
}

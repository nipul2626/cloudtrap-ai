package com.cloudtrap.api.workspace.repository;

import com.cloudtrap.api.workspace.entity.Workspace;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface WorkspaceRepository
        extends JpaRepository<Workspace, UUID> {

    Optional<Workspace> findBySlug(String slug);

    boolean existsBySlug(String slug);

    /**
     * Return only workspaces for which the user has a membership.
     * EXISTS avoids duplicate workspace rows if the data model evolves.
     */
    @Query(
            value = """
                    select w
                    from Workspace w
                    where exists (
                        select m.id
                        from WorkspaceMembership m
                        where m.workspaceId = w.id
                          and m.userId = :userId
                    )
                    """,
            countQuery = """
                    select count(w)
                    from Workspace w
                    where exists (
                        select m.id
                        from WorkspaceMembership m
                        where m.workspaceId = w.id
                          and m.userId = :userId
                    )
                    """
    )
    Page<Workspace> findAllVisibleToUser(
            @Param("userId") UUID userId,
            Pageable pageable
    );
}
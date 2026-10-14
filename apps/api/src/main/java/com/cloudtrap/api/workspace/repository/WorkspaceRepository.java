package com.cloudtrap.api.workspace.repository;

import com.cloudtrap.api.workspace.entity.Workspace;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * Persistence operations for workspaces.
 *
 * Workspace authorization and business rules belong in the service layer.
 */
public interface WorkspaceRepository
        extends JpaRepository<Workspace, UUID> {

    /**
     * Finds a workspace by its unique slug.
     */
    Optional<Workspace> findBySlug(String slug);

    /**
     * Checks whether a workspace slug is already registered.
     */
    boolean existsBySlug(String slug);
}
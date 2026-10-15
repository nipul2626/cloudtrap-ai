
package com.cloudtrap.api.cloudaccount.repository;

import com.cloudtrap.api.cloudaccount.entity.CloudAccount;
import com.cloudtrap.api.cloudaccount.entity.CloudAccountStatus;
import com.cloudtrap.api.cloudaccount.entity.CloudProvider;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * Data-access operations for connected cloud accounts.
 *
 * Workspace authorization, provider-specific validation, and
 * account lifecycle rules belong in the service layer.
 */
public interface CloudAccountRepository
        extends JpaRepository<CloudAccount, UUID> {

    /**
     * Finds a cloud account by its identifier within a workspace.
     */
    Optional<CloudAccount> findByIdAndWorkspaceId(
            UUID id,
            UUID workspaceId
    );

    /**
     * Lists cloud accounts belonging to a workspace using pagination.
     */
    Page<CloudAccount> findAllByWorkspaceId(
            UUID workspaceId,
            Pageable pageable
    );

    Page<CloudAccount> findAllByWorkspaceIdAndProviderAndStatus(
            UUID workspaceId,
            CloudProvider provider,
            CloudAccountStatus status,
            Pageable pageable
    );

    /**
     * Lists accounts for a particular provider within a workspace.
     */
    Page<CloudAccount> findAllByWorkspaceIdAndProvider(
            UUID workspaceId,
            CloudProvider provider,
            Pageable pageable
    );

    /**
     * Checks whether an account with the same composite identity exists.
     */
    boolean existsByWorkspaceIdAndProviderAndAccountIdentifier(
            UUID workspaceId,
            CloudProvider provider,
            String accountIdentifier
    );

    /**
     * Lists accounts with a particular status within a workspace.
     */
    Page<CloudAccount> findAllByWorkspaceIdAndStatus(
            UUID workspaceId,
            CloudAccountStatus status,
            Pageable pageable
    );
}

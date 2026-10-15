
package com.cloudtrap.api.cloudaccount.dto;

import com.cloudtrap.api.cloudaccount.entity.CloudAccount;
import com.cloudtrap.api.cloudaccount.entity.CloudAccountStatus;
import com.cloudtrap.api.cloudaccount.entity.CloudProvider;

import java.time.Instant;
import java.util.UUID;

public record CloudAccountResponse(
        UUID id,
        UUID workspaceId,
        CloudProvider provider,
        String accountIdentifier,
        String displayName,
        CloudAccountStatus status,
        Instant createdAt,
        Instant updatedAt
) {
    public static CloudAccountResponse from(CloudAccount account) {
        return new CloudAccountResponse(
                account.getId(),
                account.getWorkspaceId(),
                account.getProvider(),
                account.getAccountIdentifier(),
                account.getDisplayName(),
                account.getStatus(),
                account.getCreatedAt(),
                account.getUpdatedAt()
        );
    }
}

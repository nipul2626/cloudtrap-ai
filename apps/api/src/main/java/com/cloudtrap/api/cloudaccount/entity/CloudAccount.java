
package com.cloudtrap.api.cloudaccount.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "cloud_accounts")
public class CloudAccount {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "workspace_id", nullable = false, updatable = false)
    private UUID workspaceId;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider", nullable = false, length = 30)
    private CloudProvider provider;

    @Column(name = "account_identifier", nullable = false, length = 255)
    private String accountIdentifier;

    @Column(name = "display_name", nullable = false, length = 120)
    private String displayName;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private CloudAccountStatus status = CloudAccountStatus.ACTIVE;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected CloudAccount() {
    }

    public CloudAccount(
            UUID workspaceId,
            CloudProvider provider,
            String accountIdentifier,
            String displayName
    ) {
        this.workspaceId = Objects.requireNonNull(
                workspaceId, "workspaceId must not be null"
        );
        this.provider = Objects.requireNonNull(
                provider, "provider must not be null"
        );
        this.accountIdentifier = requireText(
                accountIdentifier, "accountIdentifier", 255
        );
        this.displayName = requireText(
                displayName, "displayName", 120
        );
        this.status = CloudAccountStatus.ACTIVE;
    }

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();

        if (id == null) {
            id = UUID.randomUUID();
        }

        if (createdAt == null) {
            createdAt = now;
        }

        if (updatedAt == null) {
            updatedAt = now;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    private static String requireText(
            String value,
            String fieldName,
            int maxLength
    ) {
        Objects.requireNonNull(value, fieldName + " must not be null");

        String normalized = value.trim();

        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(
                    fieldName + " must not be blank"
            );
        }

        if (normalized.length() > maxLength) {
            throw new IllegalArgumentException(
                    fieldName + " must not exceed "
                            + maxLength + " characters"
            );
        }

        return normalized;
    }

    public UUID getId() {
        return id;
    }

    public UUID getWorkspaceId() {
        return workspaceId;
    }

    public CloudProvider getProvider() {
        return provider;
    }

    public String getAccountIdentifier() {
        return accountIdentifier;
    }

    public String getDisplayName() {
        return displayName;
    }

    public CloudAccountStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void rename(String displayName) {
        this.displayName = requireText(
                displayName, "displayName", 120
        );
    }

    public void disable() {
        this.status = CloudAccountStatus.DISABLED;
    }

    public void enable() {
        this.status = CloudAccountStatus.ACTIVE;
    }
}

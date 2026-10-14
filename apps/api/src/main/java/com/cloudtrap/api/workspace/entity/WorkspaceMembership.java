package com.cloudtrap.api.workspace.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "workspace_memberships")
public class WorkspaceMembership {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "workspace_id", nullable = false, updatable = false)
    private UUID workspaceId;

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Column(name = "role", nullable = false, length = 30)
    private String role;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected WorkspaceMembership() {
    }

    public WorkspaceMembership(
            UUID workspaceId,
            UUID userId,
            String role
    ) {
        this.workspaceId = Objects.requireNonNull(
                workspaceId, "workspaceId must not be null"
        );
        this.userId = Objects.requireNonNull(
                userId, "userId must not be null"
        );
        this.role = validateRole(role);
    }

    @PrePersist
    protected void onCreate() {
        if (id == null) {
            id = UUID.randomUUID();
        }

        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    private static String validateRole(String role) {
        Objects.requireNonNull(role, "role must not be null");

        String normalized = role.trim().toUpperCase(java.util.Locale.ROOT);

        if (!normalized.equals("OWNER")
                && !normalized.equals("ADMIN")
                && !normalized.equals("ANALYST")
                && !normalized.equals("VIEWER")) {
            throw new IllegalArgumentException(
                    "Unsupported workspace role: " + role
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

    public UUID getUserId() {
        return userId;
    }

    public String getRole() {
        return role;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void changeRole(String role) {
        this.role = validateRole(role);
    }
}
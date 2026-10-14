
-- V1: Identity, workspaces, memberships, and cloud accounts.
-- PostgreSQL is the source of truth for the application schema.

CREATE TABLE users (
                       id UUID PRIMARY KEY,
                       email VARCHAR(320) NOT NULL,
                       password_hash VARCHAR(255) NOT NULL,
                       display_name VARCHAR(120) NOT NULL,
                       is_active BOOLEAN NOT NULL DEFAULT TRUE,
                       created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
                       updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
                       CONSTRAINT uq_users_email UNIQUE (email)
);

CREATE TABLE workspaces (
                            id UUID PRIMARY KEY,
                            name VARCHAR(120) NOT NULL,
                            slug VARCHAR(100) NOT NULL,
                            created_by UUID NOT NULL,
                            created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
                            updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
                            CONSTRAINT uq_workspaces_slug UNIQUE (slug),
                            CONSTRAINT fk_workspaces_created_by
                                FOREIGN KEY (created_by) REFERENCES users(id)
);

CREATE TABLE workspace_memberships (
                                       id UUID PRIMARY KEY,
                                       workspace_id UUID NOT NULL,
                                       user_id UUID NOT NULL,
                                       role VARCHAR(30) NOT NULL,
                                       created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                       CONSTRAINT fk_memberships_workspace
                                           FOREIGN KEY (workspace_id) REFERENCES workspaces(id),
                                       CONSTRAINT fk_memberships_user
                                           FOREIGN KEY (user_id) REFERENCES users(id),
                                       CONSTRAINT uq_workspace_membership
                                           UNIQUE (workspace_id, user_id),
                                       CONSTRAINT chk_membership_role
                                           CHECK (role IN ('OWNER', 'ADMIN', 'ANALYST', 'VIEWER'))
);

CREATE TABLE cloud_accounts (
                                id UUID PRIMARY KEY,
                                workspace_id UUID NOT NULL,
                                provider VARCHAR(30) NOT NULL,
                                account_identifier VARCHAR(255) NOT NULL,
                                display_name VARCHAR(120) NOT NULL,
                                status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
                                created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                CONSTRAINT fk_cloud_accounts_workspace
                                    FOREIGN KEY (workspace_id) REFERENCES workspaces(id),
                                CONSTRAINT uq_cloud_account
                                    UNIQUE (workspace_id, provider, account_identifier),
                                CONSTRAINT chk_cloud_account_provider
                                    CHECK (provider IN ('AWS', 'AZURE', 'GCP', 'DEMO')),
                                CONSTRAINT chk_cloud_account_status
                                    CHECK (status IN ('ACTIVE', 'DISABLED'))
);

CREATE INDEX idx_memberships_user_id
    ON workspace_memberships(user_id);

CREATE INDEX idx_cloud_accounts_workspace_id
    ON cloud_accounts(workspace_id);

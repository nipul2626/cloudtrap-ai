
package com.cloudtrap.api.scanning;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("postgres-test")
@TestPropertySource(
        locations = "classpath:application-postgres-test.properties"
)
@Transactional
class AccountScopedAssetPersistenceIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void shouldAllowSameExternalIdInDifferentAwsAccounts() {
        UUID userId = createUser();
        UUID workspaceId = createWorkspace(userId);
        UUID firstAccountId = createAwsAccount(workspaceId, "111111111111");
        UUID secondAccountId = createAwsAccount(workspaceId, "222222222222");

        String externalId = "arn:aws:s3:::shared-test-bucket";

        insertCloudAsset(
                workspaceId,
                firstAccountId,
                externalId,
                "first-account-bucket"
        );

        insertCloudAsset(
                workspaceId,
                secondAccountId,
                externalId,
                "second-account-bucket"
        );

        Integer count = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM assets
                WHERE workspace_id = ?
                  AND provider = 'AWS'
                  AND external_id = ?
                  AND cloud_account_id IN (?, ?)
                """,
                Integer.class,
                workspaceId,
                externalId,
                firstAccountId,
                secondAccountId
        );

        assertThat(count).isEqualTo(2);
    }

    @Test
    void shouldUpdateExistingCloudAssetForSameAccount() {
        UUID userId = createUser();
        UUID workspaceId = createWorkspace(userId);
        UUID accountId = createAwsAccount(workspaceId, "333333333333");

        String externalId = "arn:aws:s3:::upsert-test-bucket";

        insertCloudAsset(
                workspaceId,
                accountId,
                externalId,
                "original-name"
        );

        insertCloudAsset(
                workspaceId,
                accountId,
                externalId,
                "updated-name"
        );

        Integer count = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM assets
                WHERE workspace_id = ?
                  AND cloud_account_id = ?
                  AND provider = 'AWS'
                  AND external_id = ?
                """,
                Integer.class,
                workspaceId,
                accountId,
                externalId
        );

        String name = jdbcTemplate.queryForObject(
                """
                SELECT name
                FROM assets
                WHERE workspace_id = ?
                  AND cloud_account_id = ?
                  AND provider = 'AWS'
                  AND external_id = ?
                """,
                String.class,
                workspaceId,
                accountId,
                externalId
        );

        assertThat(count).isEqualTo(1);
        assertThat(name).isEqualTo("updated-name");
    }

    @Test
    void shouldKeepDemoAssetIdentityUnscoped() {
        UUID userId = createUser();
        UUID workspaceId = createWorkspace(userId);

        String externalId = "demo:test-asset";

        insertDemoAsset(workspaceId, externalId, "demo-original");
        insertDemoAsset(workspaceId, externalId, "demo-updated");

        Integer count = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM assets
                WHERE workspace_id = ?
                  AND cloud_account_id IS NULL
                  AND provider = 'DEMO'
                  AND external_id = ?
                """,
                Integer.class,
                workspaceId,
                externalId
        );

        String name = jdbcTemplate.queryForObject(
                """
                SELECT name
                FROM assets
                WHERE workspace_id = ?
                  AND cloud_account_id IS NULL
                  AND provider = 'DEMO'
                  AND external_id = ?
                """,
                String.class,
                workspaceId,
                externalId
        );

        assertThat(count).isEqualTo(1);
        assertThat(name).isEqualTo("demo-updated");
    }

    private UUID createUser() {
        UUID userId = UUID.randomUUID();

        jdbcTemplate.update(
                """
                INSERT INTO users (
                    id, email, password_hash, display_name
                )
                VALUES (?, ?, ?, ?)
                """,
                userId,
                "asset-test-" + userId + "@example.invalid",
                "integration-test-not-a-real-password-hash",
                "Asset Integration Test"
        );

        return userId;
    }

    private UUID createWorkspace(UUID userId) {
        UUID workspaceId = UUID.randomUUID();

        jdbcTemplate.update(
                """
                INSERT INTO workspaces (
                    id, name, slug, created_by
                )
                VALUES (?, ?, ?, ?)
                """,
                workspaceId,
                "Asset Test Workspace",
                "asset-test-" + workspaceId,
                userId
        );

        return workspaceId;
    }

    private UUID createAwsAccount(UUID workspaceId, String accountIdentifier) {
        UUID accountId = UUID.randomUUID();

        jdbcTemplate.update(
                """
                INSERT INTO cloud_accounts (
                    id, workspace_id, provider, account_identifier,
                    display_name, status
                )
                VALUES (?, ?, 'AWS', ?, ?, 'ACTIVE')
                """,
                accountId,
                workspaceId,
                accountIdentifier,
                "AWS Test Account " + accountIdentifier
        );

        return accountId;
    }

    private void insertCloudAsset(
            UUID workspaceId,
            UUID accountId,
            String externalId,
            String name
    ) {
        jdbcTemplate.update(
                """
                INSERT INTO assets (
                    id, workspace_id, cloud_account_id, provider,
                    external_id, asset_type, name, exposure,
                    criticality, normalized_attributes
                )
                VALUES (
                    ?, ?, ?, 'AWS', ?, 'S3_BUCKET', ?, 'PRIVATE',
                    3, '{}'::jsonb
                )
                ON CONFLICT (
                    workspace_id, cloud_account_id, provider, external_id
                ) WHERE cloud_account_id IS NOT NULL
                DO UPDATE SET
                    name = EXCLUDED.name,
                    last_seen_at = CURRENT_TIMESTAMP,
                    updated_at = CURRENT_TIMESTAMP
                """,
                UUID.randomUUID(),
                workspaceId,
                accountId,
                externalId,
                name
        );
    }

    private void insertDemoAsset(
            UUID workspaceId,
            String externalId,
            String name
    ) {
        jdbcTemplate.update(
                """
                INSERT INTO assets (
                    id, workspace_id, cloud_account_id, provider,
                    external_id, asset_type, name, exposure,
                    criticality, normalized_attributes
                )
                VALUES (
                    ?, ?, NULL, 'DEMO', ?, 'DEMO_RESOURCE', ?,
                    'UNKNOWN', 3, '{}'::jsonb
                )
                ON CONFLICT (workspace_id, provider, external_id)
                WHERE cloud_account_id IS NULL
                DO UPDATE SET
                    name = EXCLUDED.name,
                    last_seen_at = CURRENT_TIMESTAMP,
                    updated_at = CURRENT_TIMESTAMP
                """,
                UUID.randomUUID(),
                workspaceId,
                externalId,
                name
        );
    }
}


package com.cloudtrap.api.scanning.worker;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("postgres-test")
@TestPropertySource(
        locations = "classpath:application-postgres-test.properties"
)
@Transactional
class ScanRetryIntegrationTest {

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void shouldPersistRetryAndTerminalFailureStates() {
        UUID userId = UUID.randomUUID();
        UUID workspaceId = UUID.randomUUID();

        jdbc.update("""
            INSERT INTO users (
                id, email, password_hash, display_name
            ) VALUES (?, ?, ?, ?)
            """,
            userId,
            "retry-test-" + userId + "@example.invalid",
            "test-hash",
            "Retry Test User"
        );

        jdbc.update("""
            INSERT INTO workspaces (id, name, slug, created_by)
            VALUES (?, ?, ?, ?)
            """,
            workspaceId,
            "Retry Test Workspace",
            "retry-test-" + workspaceId,
            userId
        );

        UUID retryScanId = insertScan(
                workspaceId, userId, 1, 3, "RUNNING"
        );

        UUID failedScanId = insertScan(
                workspaceId, userId, 3, 3, "RUNNING"
        );

        // Same retry transition SQL used by ScanWorker.retryOrFail().
        jdbc.update("""
            UPDATE scans
            SET status = CASE
                    WHEN attempt_count >= max_attempts THEN 'FAILED'
                    ELSE 'QUEUED'
                END,
                completed_at = CASE
                    WHEN attempt_count >= max_attempts
                        THEN CURRENT_TIMESTAMP
                    ELSE NULL
                END,
                error_code = CASE
                    WHEN attempt_count >= max_attempts
                        THEN 'SCAN_EXECUTION_FAILED'
                    ELSE NULL
                END,
                safe_error_message = CASE
                    WHEN attempt_count >= max_attempts
                        THEN 'The scan could not be completed. Retry the scan.'
                    ELSE NULL
                END,
                lease_owner = NULL,
                lease_expires_at = NULL,
                updated_at = CURRENT_TIMESTAMP
            WHERE id = ?
              AND status = 'RUNNING'
            """,
            retryScanId
        );

        jdbc.update("""
            UPDATE scans
            SET status = CASE
                    WHEN attempt_count >= max_attempts THEN 'FAILED'
                    ELSE 'QUEUED'
                END,
                completed_at = CASE
                    WHEN attempt_count >= max_attempts
                        THEN CURRENT_TIMESTAMP
                    ELSE NULL
                END,
                error_code = CASE
                    WHEN attempt_count >= max_attempts
                        THEN 'SCAN_EXECUTION_FAILED'
                    ELSE NULL
                END,
                safe_error_message = CASE
                    WHEN attempt_count >= max_attempts
                        THEN 'The scan could not be completed. Retry the scan.'
                    ELSE NULL
                END,
                lease_owner = NULL,
                lease_expires_at = NULL,
                updated_at = CURRENT_TIMESTAMP
            WHERE id = ?
              AND status = 'RUNNING'
            """,
            failedScanId
        );

        assertThat(readStatus(retryScanId)).isEqualTo("QUEUED");
        assertThat(readStatus(failedScanId)).isEqualTo("FAILED");
        assertThat(readErrorCode(failedScanId))
                .isEqualTo("SCAN_EXECUTION_FAILED");
    }

    private UUID insertScan(
            UUID workspaceId,
            UUID userId,
            int attempts,
            int maxAttempts,
            String status
    ) {
        UUID scanId = UUID.randomUUID();

        jdbc.update("""
            INSERT INTO scans (
                id, workspace_id, requested_by, source, dataset_id,
                status, rule_set_version, risk_formula_version,
                request_fingerprint, attempt_count, max_attempts,
                lease_owner, lease_expires_at
            ) VALUES (
                ?, ?, ?, 'DEMO', 'retry-test',
                ?, 'test-rules-v1', 'cloudtrap-risk-v1',
                ?, ?, ?, 'retry-test-worker',
                CASE WHEN ? = 'RUNNING'
                     THEN CURRENT_TIMESTAMP + INTERVAL '60 seconds'
                     ELSE NULL
                END
            )
            """,
            scanId,
            workspaceId,
            userId,
            status,
            UUID.randomUUID().toString().replace("-", "")
                    + UUID.randomUUID().toString().replace("-", ""),
            attempts,
            maxAttempts,
            status
        );

        return scanId;
    }

    private String readStatus(UUID scanId) {
        return jdbc.queryForObject(
                "SELECT status FROM scans WHERE id = ?",
                String.class,
                scanId
        );
    }

    private String readErrorCode(UUID scanId) {
        return jdbc.queryForObject(
                "SELECT error_code FROM scans WHERE id = ?",
                String.class,
                scanId
        );
    }
}

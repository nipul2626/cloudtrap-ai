package com.cloudtrap.api.scanning.persistence;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Repository
public class ScanRepository {
    private final JdbcTemplate jdbc;

    public ScanRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @Transactional
    public Map<String, Object> claimNext(String workerId, int leaseSeconds) {
        List<Map<String, Object>> claimed = jdbc.queryForList("""
            WITH candidate AS (
                SELECT id FROM scans
                WHERE status = 'QUEUED'
                   OR (status = 'RUNNING' AND lease_expires_at < CURRENT_TIMESTAMP)
                ORDER BY queued_at, created_at
                FOR UPDATE SKIP LOCKED
                LIMIT 1
            )
            UPDATE scans s
               SET status = 'RUNNING',
                   attempt_count = s.attempt_count + 1,
                   lease_owner = ?,
                   lease_expires_at = CURRENT_TIMESTAMP + (? * INTERVAL '1 second'),
                   started_at = COALESCE(s.started_at, CURRENT_TIMESTAMP),
                   completed_at = NULL,
                   error_code = NULL,
                   safe_error_message = NULL,
                   updated_at = CURRENT_TIMESTAMP
              FROM candidate c
             WHERE s.id = c.id
               AND s.attempt_count < s.max_attempts
            RETURNING s.id, s.workspace_id, s.dataset_id, s.attempt_count, s.max_attempts,
                      s.lease_owner, s.lease_expires_at, s.rule_set_version
            """, workerId, leaseSeconds);
        return claimed.isEmpty() ? null : claimed.getFirst();
    }

    public boolean ownsLease(UUID scanId, String workerId) {
        return Boolean.TRUE.equals(jdbc.queryForObject("""
            SELECT EXISTS(SELECT 1 FROM scans WHERE id = ? AND status = 'RUNNING'
              AND lease_owner = ? AND lease_expires_at > CURRENT_TIMESTAMP)
            """, Boolean.class, scanId, workerId));
    }

    public Map<String, Object> find(UUID scanId, UUID workspaceId) {
        List<Map<String, Object>> rows = jdbc.queryForList("""
            SELECT id, workspace_id, dataset_id, source, status, rule_set_version,
                   risk_formula_version, queued_at, started_at, completed_at,
                   assets_discovered, assets_scanned, findings_created, error_code,
                   safe_error_message, attempt_count, max_attempts
              FROM scans WHERE id = ? AND workspace_id = ?
            """, scanId, workspaceId);
        return rows.isEmpty() ? null : rows.getFirst();
    }

    public void complete(UUID scanId, String workerId, int discovered, int scanned, int findings) {
        int updated = jdbc.update("""
            UPDATE scans SET status='COMPLETED', completed_at=CURRENT_TIMESTAMP,
              lease_owner=NULL, lease_expires_at=NULL, assets_discovered=?,
              assets_scanned=?, findings_created=?, updated_at=CURRENT_TIMESTAMP
            WHERE id=? AND status='RUNNING' AND lease_owner=?
              AND lease_expires_at > CURRENT_TIMESTAMP
            """, discovered, scanned, findings, scanId, workerId);
        if (updated != 1) throw new IllegalStateException("Scan lease was lost before completion");
    }

    public void failOrRetry(UUID scanId, String workerId, String code, String safeMessage) {
        jdbc.update("""
            UPDATE scans SET
              status = CASE WHEN attempt_count >= max_attempts THEN 'FAILED' ELSE 'QUEUED' END,
              completed_at = CASE WHEN attempt_count >= max_attempts THEN CURRENT_TIMESTAMP ELSE NULL END,
              error_code = CASE WHEN attempt_count >= max_attempts THEN ? ELSE NULL END,
              safe_error_message = CASE WHEN attempt_count >= max_attempts THEN ? ELSE NULL END,
              lease_owner=NULL, lease_expires_at=NULL, updated_at=CURRENT_TIMESTAMP
            WHERE id=? AND status='RUNNING' AND lease_owner=?
            """, code, safeMessage, scanId, workerId);
    }
}

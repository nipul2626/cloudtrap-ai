
package com.cloudtrap.api.scanning.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class ScanController {

    private static final String DEMO_DATASET_ID = "default-demo";
    private static final int MAX_PAGE_SIZE = 100;

    private final JdbcTemplate jdbc;

    public ScanController(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public record CreateScanRequest(
            @NotNull UUID workspaceId,
            @NotBlank
            @Size(max = 120)
            @Pattern(regexp = "[a-zA-Z0-9][a-zA-Z0-9._-]*")
            String datasetId
    ) {}

    public record FindingStatusRequest(
            @NotBlank
            @Pattern(regexp = "ACKNOWLEDGED|RESOLVED|ACCEPTED_RISK|OPEN")
            String status
    ) {}

    @PostMapping("/scans")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public Map<String, Object> create(
            @Valid @RequestBody CreateScanRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String key,
            Authentication authentication
    ) {
        UUID userId = currentUserId(authentication);
        requireWriter(request.workspaceId(), userId);

        if (!DEMO_DATASET_ID.equals(request.datasetId())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Unsupported dataset"
            );
        }

        validateIdempotencyKey(key);

        String fingerprint = sha256(
                request.workspaceId() + "|" + request.datasetId()
        );

        if (key != null) {
            List<Map<String, Object>> existing = jdbc.queryForList(
                    """
                    SELECT id, request_fingerprint
                    FROM scans
                    WHERE workspace_id = ? AND idempotency_key = ?
                    """,
                    request.workspaceId(),
                    key
            );

            if (!existing.isEmpty()) {
                verifyFingerprint(existing.getFirst(), fingerprint);
                return scanResponse(
                        asUuid(existing.getFirst().get("id")),
                        request.workspaceId()
                );
            }
        }

        String ruleSetVersion = jdbc.queryForList(
                        """
                        SELECT rule_id, version
                        FROM security_rules
                        WHERE enabled = TRUE
                        ORDER BY rule_id, version
                        """
                )
                .stream()
                .map(row -> row.get("rule_id") + "@" + row.get("version"))
                .reduce((left, right) -> left + "," + right)
                .orElse("empty");

        UUID scanId = UUID.randomUUID();

        try {
            jdbc.update(
                    """
                    INSERT INTO scans (
                        id, workspace_id, requested_by, source, dataset_id,
                        status, rule_set_version, risk_formula_version,
                        idempotency_key, request_fingerprint, max_attempts
                    )
                    VALUES (
                        ?, ?, ?, 'DEMO', ?, 'QUEUED', ?,
                        'cloudtrap-risk-v1', ?, ?, 3
                    )
                    """,
                    scanId,
                    request.workspaceId(),
                    userId,
                    request.datasetId(),
                    ruleSetVersion,
                    key,
                    fingerprint
            );
        } catch (DuplicateKeyException exception) {
            // A concurrent request may have inserted the same idempotency key.
            if (key == null) {
                throw exception;
            }

            List<Map<String, Object>> existing = jdbc.queryForList(
                    """
                    SELECT id, request_fingerprint
                    FROM scans
                    WHERE workspace_id = ? AND idempotency_key = ?
                    """,
                    request.workspaceId(),
                    key
            );

            if (existing.isEmpty()) {
                throw exception;
            }

            verifyFingerprint(existing.getFirst(), fingerprint);
            scanId = asUuid(existing.getFirst().get("id"));
        }

        return scanResponse(scanId, request.workspaceId());
    }

    @GetMapping("/scans/{scanId}")
    public Map<String, Object> getScan(
            @PathVariable UUID scanId,
            @RequestParam UUID workspaceId,
            Authentication authentication
    ) {
        requireMembership(workspaceId, currentUserId(authentication));
        return scanResponse(scanId, workspaceId);
    }

    @GetMapping("/scans/{scanId}/findings")
    public Map<String, Object> getFindings(
            @PathVariable UUID scanId,
            @RequestParam UUID workspaceId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            Authentication authentication
    ) {
        requireMembership(workspaceId, currentUserId(authentication));
        validatePagination(page, size);

        Map<String, Object> scan = scanResponse(scanId, workspaceId);
        int offset = Math.multiplyExact(page, size);

        List<Map<String, Object>> items = jdbc.queryForList(
                """
                SELECT
                    f.id,
                    f.asset_id,
                    f.rule_id,
                    f.rule_version,
                    f.source_type,
                    f.title,
                    f.description,
                    f.category,
                    f.severity,
                    f.status,
                    f.evidence,
                    f.first_detected_at,
                    f.last_detected_at,
                    (
                        SELECT ra.score
                        FROM risk_assessments ra
                        WHERE ra.workspace_id = f.workspace_id
                          AND ra.finding_id = f.id
                        ORDER BY ra.calculated_at DESC, ra.id DESC
                        LIMIT 1
                    ) AS risk_score
                FROM findings f
                WHERE f.workspace_id = ? AND f.scan_id = ?
                ORDER BY
                    CASE f.severity
                        WHEN 'CRITICAL' THEN 0
                        WHEN 'HIGH' THEN 1
                        WHEN 'MEDIUM' THEN 2
                        WHEN 'LOW' THEN 3
                        ELSE 4
                    END,
                    f.created_at DESC,
                    f.id
                LIMIT ? OFFSET ?
                """,
                workspaceId, scanId, size, offset
        );

        Integer count = jdbc.queryForObject(
                """
                SELECT COUNT(*)
                FROM findings
                WHERE workspace_id = ? AND scan_id = ?
                """,
                Integer.class,
                workspaceId,
                scanId
        );

        return Map.of(
                "scan", scan,
                "items", items,
                "page", page,
                "size", size,
                "total", count == null ? 0 : count
        );
    }

    @PatchMapping("/findings/{findingId}/status")
    public Map<String, Object> updateFindingStatus(
            @PathVariable UUID findingId,
            @RequestParam UUID workspaceId,
            @Valid @RequestBody FindingStatusRequest request,
            Authentication authentication
    ) {
        requireWriter(workspaceId, currentUserId(authentication));

        int updated = jdbc.update(
                """
                UPDATE findings
                SET status = ?,
                    resolved_at = CASE
                        WHEN ? = 'RESOLVED' THEN CURRENT_TIMESTAMP
                        ELSE NULL
                    END,
                    updated_at = CURRENT_TIMESTAMP
                WHERE id = ? AND workspace_id = ?
                """,
                request.status(),
                request.status(),
                findingId,
                workspaceId
        );

        if (updated != 1) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Finding not found"
            );
        }

        return jdbc.queryForList(
                """
                SELECT id, workspace_id, scan_id, asset_id, rule_id,
                       rule_version, title, severity, status,
                       resolved_at, updated_at
                FROM findings
                WHERE id = ? AND workspace_id = ?
                """,
                findingId,
                workspaceId
        ).getFirst();
    }

    @GetMapping("/assets")
    public Map<String, Object> listAssets(
            @RequestParam UUID workspaceId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            Authentication authentication
    ) {
        requireMembership(workspaceId, currentUserId(authentication));
        validatePagination(page, size);

        int offset = Math.multiplyExact(page, size);

        List<Map<String, Object>> items = jdbc.queryForList(
                """
                SELECT id, provider, external_id, asset_type, name,
                       region, exposure, criticality,
                       first_seen_at, last_seen_at
                FROM assets
                WHERE workspace_id = ?
                ORDER BY last_seen_at DESC, id
                LIMIT ? OFFSET ?
                """,
                workspaceId, size, offset
        );

        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM assets WHERE workspace_id = ?",
                Integer.class,
                workspaceId
        );

        return Map.of(
                "items", items,
                "page", page,
                "size", size,
                "total", count == null ? 0 : count
        );
    }

    @GetMapping("/assets/{assetId}")
    public Map<String, Object> getAsset(
            @PathVariable UUID assetId,
            @RequestParam UUID workspaceId,
            Authentication authentication
    ) {
        requireMembership(workspaceId, currentUserId(authentication));

        List<Map<String, Object>> rows = jdbc.queryForList(
                """
                SELECT
                    a.id,
                    a.provider,
                    a.external_id,
                    a.asset_type,
                    a.name,
                    a.region,
                    a.exposure,
                    a.criticality,
                    a.normalized_attributes,
                    a.first_seen_at,
                    a.last_seen_at,
                    (
                        SELECT COUNT(*)
                        FROM findings f
                        WHERE f.workspace_id = a.workspace_id
                          AND f.asset_id = a.id
                          AND f.status = 'OPEN'
                    ) AS open_findings
                FROM assets a
                WHERE a.id = ? AND a.workspace_id = ?
                """,
                assetId,
                workspaceId
        );

        if (rows.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Asset not found"
            );
        }

        return rows.getFirst();
    }

    private Map<String, Object> scanResponse(UUID scanId, UUID workspaceId) {
        List<Map<String, Object>> rows = jdbc.queryForList(
                """
                SELECT
                    id, workspace_id, dataset_id, source, status,
                    rule_set_version, risk_formula_version,
                    queued_at, started_at, completed_at,
                    assets_discovered, assets_scanned, findings_created,
                    error_code, safe_error_message, attempt_count, max_attempts
                FROM scans
                WHERE id = ? AND workspace_id = ?
                """,
                scanId,
                workspaceId
        );

        if (rows.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Scan not found"
            );
        }

        return rows.getFirst();
    }

    private UUID currentUserId(Authentication authentication) {
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication.getName() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }

        List<UUID> ids = jdbc.query(
                """
                SELECT id
                FROM users
                WHERE lower(email) = lower(?)
                """,
                (resultSet, rowNumber) ->
                        resultSet.getObject("id", UUID.class),
                authentication.getName()
        );

        if (ids.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }

        return ids.getFirst();
    }

    private void requireWriter(UUID workspaceId, UUID userId) {
        List<String> roles = jdbc.query(
                """
                SELECT m.role
                FROM workspaces w
                JOIN workspace_memberships m ON m.workspace_id = w.id
                WHERE w.id = ? AND m.user_id = ?
                """,
                (resultSet, rowNumber) -> resultSet.getString("role"),
                workspaceId,
                userId
        );

        if (roles.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Workspace not found"
            );
        }

        if (!Set.of("OWNER", "ADMIN", "ANALYST").contains(roles.getFirst())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Workspace role cannot perform this action"
            );
        }
    }

    private void requireMembership(UUID workspaceId, UUID userId) {
        Boolean member = jdbc.queryForObject(
                """
                SELECT EXISTS (
                    SELECT 1
                    FROM workspaces w
                    JOIN workspace_memberships m ON m.workspace_id = w.id
                    WHERE w.id = ? AND m.user_id = ?
                )
                """,
                Boolean.class,
                workspaceId,
                userId
        );

        if (!Boolean.TRUE.equals(member)) {
            // Do not disclose whether another workspace exists.
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Workspace not found"
            );
        }
    }

    private static void validateIdempotencyKey(String key) {
        if (key != null && (
                key.isBlank()
                || key.length() > 128
                || !key.matches("[A-Za-z0-9._:-]+")
        )) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid Idempotency-Key"
            );
        }
    }

    private static void verifyFingerprint(
            Map<String, Object> existing,
            String expectedFingerprint
    ) {
        Object stored = existing.get("request_fingerprint");

        if (stored == null
                || !expectedFingerprint.equals(stored.toString().trim())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Idempotency key was used for a different request"
            );
        }
    }

    private static void validatePagination(int page, int size) {
        if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid pagination"
            );
        }

        try {
            Math.multiplyExact(page, size);
        } catch (ArithmeticException exception) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Pagination offset is too large"
            );
        }
    }

    private static UUID asUuid(Object value) {
        if (value instanceof UUID uuid) {
            return uuid;
        }

        return UUID.fromString(value.toString());
    }

    private static String sha256(String value) {
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256")
                            .digest(value.getBytes(StandardCharsets.UTF_8))
            );
        } catch (java.security.NoSuchAlgorithmException exception) {
            throw new IllegalStateException(
                    "SHA-256 is unavailable",
                    exception
            );
        }
    }
}

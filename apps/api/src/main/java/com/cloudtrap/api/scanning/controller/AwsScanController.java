
package com.cloudtrap.api.scanning.controller;

import tools.jackson.core.JacksonException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api/v1/scans/aws")
public class AwsScanController {

    private static final Pattern AWS_ACCOUNT_ID = Pattern.compile("[0-9]{12}");
    private static final Pattern REGION = Pattern.compile("[a-z0-9-]{1,64}");
    private static final Pattern RESOURCE_TYPE =
            Pattern.compile("[a-zA-Z][a-zA-Z0-9._:/-]{0,119}");
    private static final Pattern IDEMPOTENCY_KEY =
            Pattern.compile("[A-Za-z0-9._:-]{1,128}");

    private static final Set<String> WRITER_ROLES =
            Set.of("OWNER", "ADMIN", "ANALYST");

    private static final Map<String, Set<String>> COLLECTORS = Map.of(
            "aws-s3", Set.of("s3", "s3:bucket"),
            "aws-ec2-security-groups", Set.of(
                    "ec2", "ec2:security-group", "ec2:security-group-rule")
    );

    private final JdbcTemplate jdbc;
    private final ObjectMapper mapper;

    public AwsScanController(JdbcTemplate jdbc, ObjectMapper mapper) {
        this.jdbc = jdbc;
        this.mapper = mapper;
    }

    public record CreateAwsScanRequest(
            @NotNull UUID workspaceId,
            @NotNull UUID cloudAccountId,
            @NotNull String collectorId,
            @Size(max = 100) List<String> regions,
            @NotNull @Size(min = 1, max = 100)
            List<String> resourceTypes,
            @Min(1) @Max(100000) int maximumAssets,
            @Min(1) @Max(1800) int timeoutSeconds
    ) {
        public CreateAwsScanRequest {
            regions = regions == null ? List.of() : List.copyOf(regions);
            resourceTypes = resourceTypes == null
                    ? List.of()
                    : List.copyOf(resourceTypes);
        }
    }

    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    public Map<String, Object> create(
            @Valid @RequestBody CreateAwsScanRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false)
            String idempotencyKey,
            Authentication authentication
    ) {
        UUID userId = currentUserId(authentication);
        requireWriter(request.workspaceId(), userId);
        validateIdempotencyKey(idempotencyKey);
        validateCollectorAndScope(request);

        String accountIdentifier = findActiveAwsAccount(
                request.workspaceId(), request.cloudAccountId());

        String fingerprint = sha256(
                request.workspaceId() + "|AWS|"
                        + request.cloudAccountId() + "|"
                        + request.collectorId() + "|"
                        + String.join(",", request.regions()) + "|"
                        + String.join(",", request.resourceTypes()) + "|"
                        + request.maximumAssets() + "|"
                        + request.timeoutSeconds()
        );

        if (idempotencyKey != null) {
            List<Map<String, Object>> existing = jdbc.queryForList(
                    """
                    SELECT id, request_fingerprint
                    FROM scans
                    WHERE workspace_id = ? AND idempotency_key = ?
                    """,
                    request.workspaceId(), idempotencyKey
            );

            if (!existing.isEmpty()) {
                verifyFingerprint(existing.getFirst(), fingerprint);
                return scanResponse(
                        asUuid(existing.getFirst().get("id")),
                        request.workspaceId());
            }
        }

        String ruleSetVersion = jdbc.queryForList(
                        """
                        SELECT rule_id, version
                        FROM security_rules
                        WHERE enabled = TRUE
                        ORDER BY rule_id, version
                        """
                ).stream()
                .map(row -> row.get("rule_id") + "@" + row.get("version"))
                .reduce((left, right) -> left + "," + right)
                .orElse("empty");

        UUID scanId = UUID.randomUUID();

        try {
            jdbc.update(
                    """
                    INSERT INTO scans (
                        id, workspace_id, cloud_account_id, requested_by,
                        source, dataset_id, status, rule_set_version,
                        risk_formula_version, idempotency_key,
                        request_fingerprint, max_attempts,
                        collector_id, collector_version,
                        collection_regions, collection_resource_types,
                        maximum_assets, collection_timeout_seconds
                    )
                    VALUES (
                        ?, ?, ?, ?, 'AWS', ?, 'QUEUED', ?,
                        'cloudtrap-risk-v1', ?, ?, 3,
                        ?, '1.0.0', ?::jsonb, ?::jsonb, ?, ?
                    )
                    """,
                    scanId,
                    request.workspaceId(),
                    request.cloudAccountId(),
                    userId,
                    request.collectorId(),
                    ruleSetVersion,
                    idempotencyKey,
                    fingerprint,
                    request.collectorId(),
                    writeJson(request.regions()),
                    writeJson(request.resourceTypes()),
                    request.maximumAssets(),
                    request.timeoutSeconds()
            );
        } catch (DuplicateKeyException exception) {
            if (idempotencyKey == null) {
                throw exception;
            }

            List<Map<String, Object>> existing = jdbc.queryForList(
                    """
                    SELECT id, request_fingerprint
                    FROM scans
                    WHERE workspace_id = ? AND idempotency_key = ?
                    """,
                    request.workspaceId(), idempotencyKey
            );

            if (existing.isEmpty()) {
                throw exception;
            }

            verifyFingerprint(existing.getFirst(), fingerprint);
            scanId = asUuid(existing.getFirst().get("id"));
        }

        return scanResponse(scanId, request.workspaceId());
    }

    private void validateCollectorAndScope(CreateAwsScanRequest request) {
        Set<String> supportedTypes = COLLECTORS.get(request.collectorId());

        if (supportedTypes == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Unsupported AWS collector");
        }

        if (request.resourceTypes().stream().anyMatch(
                type -> type == null
                        || !RESOURCE_TYPE.matcher(type).matches()
                        || !supportedTypes.contains(type))) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Resource types are not supported by this collector");
        }

        if (request.regions().stream().anyMatch(
                region -> region == null
                        || !REGION.matcher(region).matches())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Invalid AWS region");
        }

        if (request.maximumAssets() < 1
                || request.maximumAssets() > 100000
                || request.timeoutSeconds() < 1
                || request.timeoutSeconds() > 1800) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Invalid collection limits");
        }
    }

    private String findActiveAwsAccount(UUID workspaceId, UUID cloudAccountId) {
        List<String> accountIds = jdbc.query(
                """
                SELECT account_identifier
                FROM cloud_accounts
                WHERE id = ?
                  AND workspace_id = ?
                  AND provider = 'AWS'
                  AND status = 'ACTIVE'
                """,
                (rs, row) -> rs.getString("account_identifier"),
                cloudAccountId,
                workspaceId
        );

        if (accountIds.isEmpty()) {
            // Avoid disclosing whether another workspace owns this account.
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "AWS cloud account not found");
        }

        String accountId = accountIds.getFirst();
        if (!AWS_ACCOUNT_ID.matcher(accountId).matches()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Configured AWS account identifier is invalid");
        }

        return accountId;
    }

    private UUID currentUserId(Authentication authentication) {
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication.getName() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }

        List<UUID> ids = jdbc.query(
                """
                SELECT id FROM users
                WHERE lower(email) = lower(?)
                """,
                (rs, row) -> rs.getObject("id", UUID.class),
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
                FROM workspace_memberships m
                JOIN workspaces w ON w.id = m.workspace_id
                WHERE m.workspace_id = ? AND m.user_id = ?
                """,
                (rs, row) -> rs.getString("role"),
                workspaceId, userId
        );

        if (roles.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Workspace not found");
        }

        if (!WRITER_ROLES.contains(roles.getFirst())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Workspace role cannot create scans");
        }
    }

    private Map<String, Object> scanResponse(UUID scanId, UUID workspaceId) {
        List<Map<String, Object>> rows = jdbc.queryForList(
                """
                SELECT id, workspace_id, cloud_account_id, source,
                       dataset_id, status, collector_id, collector_version,
                       collection_regions, collection_resource_types,
                       maximum_assets, collection_timeout_seconds,
                       collection_coverage, collection_errors,
                       queued_at, started_at, completed_at,
                       assets_discovered, assets_scanned, findings_created,
                       error_code, safe_error_message, attempt_count, max_attempts
                FROM scans
                WHERE id = ? AND workspace_id = ?
                """,
                scanId, workspaceId
        );

        if (rows.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Scan not found");
        }
        return rows.getFirst();
    }

    private static void validateIdempotencyKey(String key) {
        if (key != null && !IDEMPOTENCY_KEY.matcher(key).matches()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Invalid Idempotency-Key");
        }
    }

    private static void verifyFingerprint(
            Map<String, Object> existing, String expected) {
        Object stored = existing.get("request_fingerprint");
        if (stored == null || !expected.equals(stored.toString().trim())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Idempotency key was used for a different request");
        }
    }

    private String writeJson(Object value) {
        try {
            return mapper.writeValueAsString(value);
        } catch (JacksonException exception) {
            throw new IllegalStateException(
                    "Could not serialize validated scan scope");
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
                            .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (java.security.NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable");
        }
    }
}

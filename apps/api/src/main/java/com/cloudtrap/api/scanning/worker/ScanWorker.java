
package com.cloudtrap.api.scanning.worker;

import com.cloudtrap.api.scanning.domain.NormalizedAsset;
import com.cloudtrap.api.scanning.domain.RuleFinding;
import com.cloudtrap.api.scanning.risk.DeterministicRiskScorer;
import com.cloudtrap.api.scanning.rules.DeterministicRuleEngine;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class ScanWorker {

    private static final String DEMO_DATASET_ID = "default-demo";
    private static final int MAX_DATASET_ASSETS = 5_000;
    private static final int MAX_ATTEMPTS = 3;
    private static final int LEASE_SECONDS = 120;

    private final JdbcTemplate jdbc;
    private final ObjectMapper mapper;
    private final DeterministicRuleEngine rules;
    private final DeterministicRiskScorer scorer;
    private final Resource demoDataset;
    private final TransactionTemplate transactionTemplate;
    private final String workerId = UUID.randomUUID().toString();

    public ScanWorker(
            JdbcTemplate jdbc,
            ObjectMapper mapper,
            DeterministicRuleEngine rules,
            DeterministicRiskScorer scorer,
            TransactionTemplate transactionTemplate,
            @Value("classpath:datasets/default-demo.json") Resource demoDataset
    ) {
        this.jdbc = jdbc;
        this.mapper = mapper;
        this.rules = rules;
        this.scorer = scorer;
        this.transactionTemplate = transactionTemplate;
        this.demoDataset = demoDataset;
    }

    @Scheduled(fixedDelayString = "${cloudtrap.scanning.poll-delay-ms:1000}")
    public void poll() {
        markExhaustedLeasesFailed();

        Map<String, Object> job = claimNext();
        if (job == null) {
            return;
        }

        UUID scanId = asUuid(job.get("id"));
        UUID workspaceId = asUuid(job.get("workspace_id"));
        String datasetId = job.get("dataset_id").toString();

        try {
            transactionTemplate.executeWithoutResult(transactionStatus -> {
                try {
                    process(scanId, workspaceId, datasetId);
                } catch (Exception exception) {
                    throw new ScanProcessingException(exception);
                }
            });
        } catch (Exception exception) {
            // Persist only safe, stable error details. Never expose exception text.
            retryOrFail(
                    scanId,
                    "SCAN_EXECUTION_FAILED",
                    "The scan could not be completed. Retry the scan."
            );
        }
    }

    private void markExhaustedLeasesFailed() {
        jdbc.update(
                """
                UPDATE scans
                SET status = 'FAILED',
                    completed_at = CURRENT_TIMESTAMP,
                    error_code = 'SCAN_RETRIES_EXHAUSTED',
                    safe_error_message =
                        'The scan worker stopped before completing this scan.',
                    lease_owner = NULL,
                    lease_expires_at = NULL,
                    updated_at = CURRENT_TIMESTAMP
                WHERE status = 'RUNNING'
                  AND lease_expires_at < CURRENT_TIMESTAMP
                  AND attempt_count >= max_attempts
                """
        );
    }

    /**
     * Claim and lease a job in one PostgreSQL statement. SKIP LOCKED allows
     * multiple worker instances to claim different jobs concurrently.
     */
    private Map<String, Object> claimNext() {
        List<Map<String, Object>> jobs = jdbc.queryForList(
                """
                WITH candidate AS (
                    SELECT id
                    FROM scans
                    WHERE (
                        status = 'QUEUED'
                        AND attempt_count < max_attempts
                    ) OR (
                        status = 'RUNNING'
                        AND lease_expires_at < CURRENT_TIMESTAMP
                        AND attempt_count < max_attempts
                    )
                    ORDER BY queued_at, created_at, id
                    LIMIT 1
                    FOR UPDATE SKIP LOCKED
                )
                UPDATE scans s
                SET status = 'RUNNING',
                    attempt_count = s.attempt_count + 1,
                    lease_owner = ?,
                    lease_expires_at =
                        CURRENT_TIMESTAMP + (? * INTERVAL '1 second'),
                    started_at = COALESCE(s.started_at, CURRENT_TIMESTAMP),
                    completed_at = NULL,
                    error_code = NULL,
                    safe_error_message = NULL,
                    updated_at = CURRENT_TIMESTAMP
                FROM candidate c
                WHERE s.id = c.id
                RETURNING s.id, s.workspace_id, s.dataset_id
                """,
                workerId,
                LEASE_SECONDS
        );

        return jobs.isEmpty() ? null : jobs.getFirst();
    }

    /**
     * This method runs inside the TransactionTemplate transaction.
     * Any exception rolls back the inventory, findings, risk, remediation,
     * and completion updates together.
     */
    private void process(
            UUID scanId,
            UUID workspaceId,
            String datasetId
    ) throws Exception {
        if (!DEMO_DATASET_ID.equals(datasetId)) {
            throw new IllegalArgumentException("Unsupported dataset");
        }

        JsonNode root;
        try (var input = demoDataset.getInputStream()) {
            root = mapper.readTree(input);
        }

        if (!datasetId.equals(root.path("datasetId").asString())) {
            throw new IllegalStateException("Dataset ID mismatch");
        }

        JsonNode assets = root.path("assets");

        if (!assets.isArray()
                || assets.isEmpty()
                || assets.size() > MAX_DATASET_ASSETS) {
            throw new IllegalStateException("Invalid bounded dataset");
        }

        int findingsCreated = 0;

        for (JsonNode node : assets) {
            requireLease(scanId);

            String externalId = requiredText(node, "externalId");
            String assetType = requiredText(node, "assetType");
            String name = requiredText(node, "name");
            String exposure = node.path("exposure").asString("UNKNOWN");
            int criticality = node.path("criticality").asInt(3);

            if (criticality < 1 || criticality > 5) {
                throw new IllegalStateException("Invalid asset criticality");
            }

            NormalizedAsset.Exposure normalizedExposure;
            try {
                normalizedExposure =
                        NormalizedAsset.Exposure.valueOf(exposure);
            } catch (IllegalArgumentException exception) {
                throw new IllegalStateException("Invalid asset exposure");
            }

            JsonNode attributesNode = node.path("attributes");
            if (!attributesNode.isObject()) {
                throw new IllegalStateException("Asset attributes must be an object");
            }

            Map<String, Object> attributes =
                    mapper.convertValue(attributesNode, Map.class);

            String attributesJson = mapper.writeValueAsString(attributes);

            UUID proposedAssetId = UUID.nameUUIDFromBytes(
                    (workspaceId + ":DEMO:" + externalId)
                            .getBytes(StandardCharsets.UTF_8)
            );

            jdbc.update(
                    """
                    INSERT INTO assets (
                        id, workspace_id, provider, external_id,
                        asset_type, name, region, exposure, criticality,
                        normalized_attributes, last_seen_at, updated_at
                    )
                    VALUES (
                        ?, ?, 'DEMO', ?, ?, ?, ?, ?, ?,
                        ?::jsonb, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
                    )
                    ON CONFLICT (workspace_id, provider, external_id)
                    DO UPDATE SET
                        asset_type = EXCLUDED.asset_type,
                        name = EXCLUDED.name,
                        region = EXCLUDED.region,
                        exposure = EXCLUDED.exposure,
                        criticality = EXCLUDED.criticality,
                        normalized_attributes = EXCLUDED.normalized_attributes,
                        last_seen_at = CURRENT_TIMESTAMP,
                        updated_at = CURRENT_TIMESTAMP
                    """,
                    proposedAssetId,
                    workspaceId,
                    externalId,
                    assetType,
                    name,
                    node.path("region").asString(null),
                    normalizedExposure.name(),
                    criticality,
                    attributesJson
            );

            UUID assetId = jdbc.queryForObject(
                    """
                    SELECT id
                    FROM assets
                    WHERE workspace_id = ?
                      AND provider = 'DEMO'
                      AND external_id = ?
                    """,
                    UUID.class,
                    workspaceId,
                    externalId
            );

            if (assetId == null) {
                throw new IllegalStateException("Asset persistence failed");
            }

            jdbc.update(
                    """
                    INSERT INTO scan_assets (scan_id, asset_id, workspace_id)
                    VALUES (?, ?, ?)
                    ON CONFLICT (scan_id, asset_id) DO NOTHING
                    """,
                    scanId,
                    assetId,
                    workspaceId
            );

            NormalizedAsset asset = new NormalizedAsset(
                    assetId,
                    workspaceId,
                    "DEMO",
                    externalId,
                    assetType,
                    name,
                    node.path("region").asString(null),
                    normalizedExposure,
                    criticality,
                    attributes
            );

            for (RuleFinding finding : rules.evaluate(asset)) {
                requireLease(scanId);

                String evidenceJson =
                        mapper.writeValueAsString(finding.evidence());

                String deduplicationKey = sha256(
                        workspaceId + "|" + assetId + "|"
                                + finding.ruleId() + "|"
                                + finding.ruleVersion()
                );

                UUID findingId = UUID.nameUUIDFromBytes(
                        (scanId + "|" + assetId + "|"
                                + finding.ruleId() + "|"
                                + finding.ruleVersion())
                                .getBytes(StandardCharsets.UTF_8)
                );

                int inserted = jdbc.update(
                        """
                        INSERT INTO findings (
                            id, workspace_id, scan_id, asset_id,
                            rule_id, rule_version, source_type,
                            title, description, category, severity,
                            status, deduplication_key, evidence
                        )
                        VALUES (
                            ?, ?, ?, ?, ?, ?, 'DETERMINISTIC_RULE',
                            ?, ?, ?, ?, 'OPEN', ?, ?::jsonb
                        )
                        ON CONFLICT (
                            scan_id, asset_id, rule_id, rule_version
                        ) DO NOTHING
                        """,
                        findingId,
                        workspaceId,
                        scanId,
                        assetId,
                        finding.ruleId(),
                        finding.ruleVersion(),
                        finding.title(),
                        finding.description(),
                        finding.category(),
                        finding.severity().name(),
                        deduplicationKey,
                        evidenceJson
                );

                if (inserted == 0) {
                    continue;
                }

                var risk = scorer.score(finding, asset);
                String factorsJson =
                        mapper.writeValueAsString(risk.factors());

                jdbc.update(
                        """
                        INSERT INTO risk_assessments (
                            id, workspace_id, finding_id, score,
                            severity, factors, formula_version
                        )
                        VALUES (?, ?, ?, ?, ?, ?::jsonb, ?)
                        """,
                        UUID.randomUUID(),
                        workspaceId,
                        findingId,
                        risk.score(),
                        finding.severity().name(),
                        factorsJson,
                        risk.formulaVersion()
                );

                String priority = finding.severity().name().equals("INFO")
                        ? "LOW"
                        : finding.severity().name();

                jdbc.update(
                        """
                        INSERT INTO remediations (
                            id, workspace_id, finding_id, summary,
                            steps, priority, source_type,
                            requires_human_review
                        )
                        VALUES (?, ?, ?, ?, ?::jsonb, ?, 'RULE', TRUE)
                        """,
                        UUID.randomUUID(),
                        workspaceId,
                        findingId,
                        finding.remediationSummary(),
                        mapper.writeValueAsString(finding.remediationSteps()),
                        priority
                );

                findingsCreated++;
            }
        }

        int updated = jdbc.update(
                """
                UPDATE scans
                SET status = 'COMPLETED',
                    completed_at = CURRENT_TIMESTAMP,
                    lease_owner = NULL,
                    lease_expires_at = NULL,
                    assets_discovered = ?,
                    assets_scanned = ?,
                    findings_created = ?,
                    updated_at = CURRENT_TIMESTAMP
                WHERE id = ?
                  AND status = 'RUNNING'
                  AND lease_owner = ?
                  AND lease_expires_at > CURRENT_TIMESTAMP
                """,
                assets.size(),
                assets.size(),
                findingsCreated,
                scanId,
                workerId
        );

        if (updated != 1) {
            throw new IllegalStateException(
                    "Scan lease was lost before completion"
            );
        }
    }

    private void requireLease(UUID scanId) {
        Boolean ownsLease = jdbc.queryForObject(
                """
                SELECT EXISTS (
                    SELECT 1
                    FROM scans
                    WHERE id = ?
                      AND status = 'RUNNING'
                      AND lease_owner = ?
                      AND lease_expires_at > CURRENT_TIMESTAMP
                )
                """,
                Boolean.class,
                scanId,
                workerId
        );

        if (!Boolean.TRUE.equals(ownsLease)) {
            throw new IllegalStateException("Scan lease was lost");
        }
    }

    private void retryOrFail(
            UUID scanId,
            String errorCode,
            String safeMessage
    ) {
        jdbc.update(
                """
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
                        WHEN attempt_count >= max_attempts THEN ?
                        ELSE NULL
                    END,
                    safe_error_message = CASE
                        WHEN attempt_count >= max_attempts THEN ?
                        ELSE NULL
                    END,
                    lease_owner = NULL,
                    lease_expires_at = NULL,
                    updated_at = CURRENT_TIMESTAMP
                WHERE id = ?
                  AND status = 'RUNNING'
                  AND lease_owner = ?
                """,
                errorCode,
                safeMessage,
                scanId,
                workerId
        );
    }

    private static String requiredText(JsonNode node, String field) {
        String value = node.path(field).asString(null);

        if (value == null || value.isBlank() || value.length() > 512) {
            throw new IllegalStateException(
                    "Dataset contains an invalid " + field
            );
        }

        return value.trim();
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

    private static final class ScanProcessingException
            extends RuntimeException {

        private ScanProcessingException(Throwable cause) {
            super(cause);
        }
    }
}


package com.cloudtrap.api.scanning.worker;

import com.cloudtrap.api.scanning.domain.NormalizedAsset;
import com.cloudtrap.api.scanning.domain.RuleFinding;
import com.cloudtrap.api.scanning.provider.CloudDiscoveryService;
import com.cloudtrap.api.scanning.provider.CollectionResult;
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
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Component
public class ScanWorker {

    private static final String DEMO_DATASET_ID = "default-demo";
    private static final int MAX_DATASET_ASSETS = 5_000;
    private static final int LEASE_SECONDS = 120;

    private final JdbcTemplate jdbc;
    private final ObjectMapper mapper;
    private final DeterministicRuleEngine rules;
    private final DeterministicRiskScorer scorer;
    private final Resource demoDataset;
    private final TransactionTemplate transactionTemplate;
    private final AwsScanExecutionService awsExecutionService;
    private final String workerId = UUID.randomUUID().toString();

    public ScanWorker(
            JdbcTemplate jdbc,
            ObjectMapper mapper,
            DeterministicRuleEngine rules,
            DeterministicRiskScorer scorer,
            TransactionTemplate transactionTemplate,
            AwsScanExecutionService awsExecutionService,
            @Value("classpath:datasets/default-demo.json")
            Resource demoDataset
    ) {
        this.jdbc = Objects.requireNonNull(jdbc);
        this.mapper = Objects.requireNonNull(mapper);
        this.rules = Objects.requireNonNull(rules);
        this.scorer = Objects.requireNonNull(scorer);
        this.transactionTemplate = Objects.requireNonNull(transactionTemplate);
        this.awsExecutionService = Objects.requireNonNull(awsExecutionService);
        this.demoDataset = Objects.requireNonNull(demoDataset);
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
        String datasetId = Objects.toString(job.get("dataset_id"), "");
        String source = Objects.toString(job.get("source"), "DEMO");

        try {
            if ("AWS".equals(source)) {
                executeAwsScan(scanId, workspaceId);
            } else if ("DEMO".equals(source)) {
                transactionTemplate.executeWithoutResult(status -> {
                    try {
                        processDemo(scanId, workspaceId, datasetId);
                    } catch (Exception exception) {
                        throw new ScanProcessingException(exception);
                    }
                });
            } else {
                throw new IllegalArgumentException("Unsupported scan source");
            }
        } catch (Exception exception) {
            retryOrFail(
                    scanId,
                    "SCAN_EXECUTION_FAILED",
                    "The scan could not be completed. Retry the scan."
            );
        }
    }

    private void executeAwsScan(UUID scanId, UUID workspaceId)
            throws Exception {

        Integer timeoutSeconds = jdbc.queryForObject(
                """
                SELECT collection_timeout_seconds
                FROM scans
                WHERE id = ?
                  AND workspace_id = ?
                  AND source = 'AWS'
                  AND status = 'RUNNING'
                  AND lease_owner = ?
                """,
                Integer.class,
                scanId,
                workspaceId,
                workerId
        );

        if (timeoutSeconds == null
                || timeoutSeconds < 1
                || timeoutSeconds > 1800) {
            throw new IllegalStateException("Invalid AWS scan timeout");
        }

        int leaseDuration = Math.min(timeoutSeconds + 60, 1860);

        int extended = jdbc.update(
                """
                UPDATE scans
                SET lease_expires_at =
                        CURRENT_TIMESTAMP + (? * INTERVAL '1 second'),
                    updated_at = CURRENT_TIMESTAMP
                WHERE id = ?
                  AND workspace_id = ?
                  AND source = 'AWS'
                  AND status = 'RUNNING'
                  AND lease_owner = ?
                  AND lease_expires_at > CURRENT_TIMESTAMP
                """,
                leaseDuration,
                scanId,
                workspaceId,
                workerId
        );

        if (extended != 1) {
            throw new IllegalStateException("Scan lease was lost");
        }

        CloudDiscoveryService.DiscoveryOutcome outcome =
                awsExecutionService.execute(scanId, workspaceId);

        CollectionResult result = outcome.collectionResult();

        UUID cloudAccountId = jdbc.queryForObject(
                """
                SELECT cloud_account_id
                FROM scans
                WHERE id = ?
                  AND workspace_id = ?
                  AND source = 'AWS'
                  AND status = 'RUNNING'
                  AND lease_owner = ?
                """,
                UUID.class,
                scanId,
                workspaceId,
                workerId
        );

        if (cloudAccountId == null) {
            throw new IllegalStateException(
                    "AWS scan has no associated cloud account");
        }

        transactionTemplate.executeWithoutResult(status -> {
            try {
                requireLease(scanId);
                persistCollectionMetadata(scanId, result);

                if (result.coverage() == CollectionResult.Coverage.FAILED) {
                    failCollection(scanId);
                    return;
                }

                int findingsCreated = persistAssetsAndAnalyze(
                        scanId,
                        workspaceId,
                        cloudAccountId,
                        result.assets()
                );

                finishScan(
                        scanId,
                        result.assets().size(),
                        findingsCreated
                );
            } catch (Exception exception) {
                throw new ScanProcessingException(exception);
            }
        });
    }

    private void processDemo(
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

        JsonNode nodes = root.path("assets");

        if (!nodes.isArray()
                || nodes.isEmpty()
                || nodes.size() > MAX_DATASET_ASSETS) {
            throw new IllegalStateException("Invalid bounded dataset");
        }

        List<NormalizedAsset> assets = new ArrayList<>();

        for (JsonNode node : nodes) {
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
                throw new IllegalStateException(
                        "Asset attributes must be an object");
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> attributes =
                    mapper.convertValue(attributesNode, Map.class);

            UUID assetId = UUID.nameUUIDFromBytes(
                    (workspaceId + ":DEMO:" + externalId)
                            .getBytes(StandardCharsets.UTF_8)
            );

            assets.add(new NormalizedAsset(
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
            ));
        }

        int findingsCreated = persistAssetsAndAnalyze(
                scanId,
                workspaceId,
                null,
                assets
        );

        finishScan(scanId, assets.size(), findingsCreated);
    }

    private int persistAssetsAndAnalyze(
            UUID scanId,
            UUID workspaceId,
            UUID cloudAccountId,
            List<NormalizedAsset> assets
    ) throws Exception {

        int findingsCreated = 0;

        for (NormalizedAsset proposed : assets) {
            requireLease(scanId);

            if (!workspaceId.equals(proposed.workspaceId())) {
                throw new IllegalStateException(
                        "Asset workspace does not match scan workspace");
            }

            String provider = proposed.provider();

            if ("DEMO".equals(provider)) {
                if (cloudAccountId != null) {
                    throw new IllegalStateException(
                            "DEMO assets cannot be assigned to a cloud account");
                }
            } else if (!"AWS".equals(provider) || cloudAccountId == null) {
                throw new IllegalStateException(
                        "Cloud asset provider/account mismatch");
            }

            String attributesJson =
                    mapper.writeValueAsString(proposed.attributes());

            UUID proposedAssetId = UUID.nameUUIDFromBytes(
                    (workspaceId + ":"
                            + (cloudAccountId == null
                                ? "DEMO"
                                : cloudAccountId.toString())
                            + ":" + provider + ":" + proposed.externalId())
                            .getBytes(StandardCharsets.UTF_8)
            );

            if (cloudAccountId == null) {
                jdbc.update(
                        """
                        INSERT INTO assets (
                            id, workspace_id, cloud_account_id, provider,
                            external_id, asset_type, name, region, exposure,
                            criticality, normalized_attributes,
                            last_seen_at, updated_at
                        )
                        VALUES (
                            ?, ?, NULL, 'DEMO', ?, ?, ?, ?, ?, ?,
                            ?::jsonb, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
                        )
                        ON CONFLICT (workspace_id, provider, external_id)
                        WHERE cloud_account_id IS NULL
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
                        proposed.externalId(),
                        proposed.assetType(),
                        proposed.name(),
                        proposed.region(),
                        proposed.exposure().name(),
                        proposed.criticality(),
                        attributesJson
                );
            } else {
                jdbc.update(
                        """
                        INSERT INTO assets (
                            id, workspace_id, cloud_account_id, provider,
                            external_id, asset_type, name, region, exposure,
                            criticality, normalized_attributes,
                            last_seen_at, updated_at
                        )
                        VALUES (
                            ?, ?, ?, ?, ?, ?, ?, ?, ?, ?,
                            ?::jsonb, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
                        )
                        ON CONFLICT (
                            workspace_id, cloud_account_id, provider, external_id
                        ) WHERE cloud_account_id IS NOT NULL
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
                        cloudAccountId,
                        provider,
                        proposed.externalId(),
                        proposed.assetType(),
                        proposed.name(),
                        proposed.region(),
                        proposed.exposure().name(),
                        proposed.criticality(),
                        attributesJson
                );
            }

            UUID assetId = jdbc.queryForObject(
                    """
                    SELECT id
                    FROM assets
                    WHERE workspace_id = ?
                      AND cloud_account_id IS NOT DISTINCT FROM ?
                      AND provider = ?
                      AND external_id = ?
                    """,
                    UUID.class,
                    workspaceId,
                    cloudAccountId,
                    provider,
                    proposed.externalId()
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
                    provider,
                    proposed.externalId(),
                    proposed.assetType(),
                    proposed.name(),
                    proposed.region(),
                    proposed.exposure(),
                    proposed.criticality(),
                    proposed.attributes()
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
                        mapper.writeValueAsString(risk.factors()),
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
                        mapper.writeValueAsString(
                                finding.remediationSteps()),
                        priority
                );

                findingsCreated++;
            }
        }

        return findingsCreated;
    }

    private void persistCollectionMetadata(
            UUID scanId,
            CollectionResult result
    ) throws Exception {

        List<Map<String, Object>> errors = result.errors().stream()
                .map(error -> Map.<String, Object>of(
                        "collectorId", error.collectorId(),
                        "code", error.code().name(),
                        "retryable", error.retryable(),
                        "safeMessage", error.safeMessage()
                ))
                .toList();

        int updated = jdbc.update(
                """
                UPDATE scans
                SET collection_coverage = ?,
                    collection_errors = ?::jsonb,
                    updated_at = CURRENT_TIMESTAMP
                WHERE id = ?
                  AND status = 'RUNNING'
                  AND lease_owner = ?
                  AND lease_expires_at > CURRENT_TIMESTAMP
                """,
                result.coverage().name(),
                mapper.writeValueAsString(errors),
                scanId,
                workerId
        );

        if (updated != 1) {
            throw new IllegalStateException("Scan lease was lost");
        }
    }

    private void failCollection(UUID scanId) {
        int updated = jdbc.update(
                """
                UPDATE scans
                SET status = 'FAILED',
                    completed_at = CURRENT_TIMESTAMP,
                    error_code = 'CLOUD_COLLECTION_FAILED',
                    safe_error_message =
                        'Cloud inventory collection failed. Review collection errors before retrying.',
                    lease_owner = NULL,
                    lease_expires_at = NULL,
                    assets_discovered = 0,
                    assets_scanned = 0,
                    findings_created = 0,
                    updated_at = CURRENT_TIMESTAMP
                WHERE id = ?
                  AND status = 'RUNNING'
                  AND lease_owner = ?
                  AND lease_expires_at > CURRENT_TIMESTAMP
                """,
                scanId,
                workerId
        );

        if (updated != 1) {
            throw new IllegalStateException("Scan lease was lost");
        }
    }

    private void finishScan(
            UUID scanId,
            int assetsDiscovered,
            int findingsCreated
    ) {
        requireLease(scanId);

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
                assetsDiscovered,
                assetsDiscovered,
                findingsCreated,
                scanId,
                workerId
        );

        if (updated != 1) {
            throw new IllegalStateException(
                    "Scan lease was lost before completion");
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
                RETURNING s.id, s.workspace_id, s.dataset_id, s.source
                """,
                workerId,
                LEASE_SECONDS
        );

        return jobs.isEmpty() ? null : jobs.getFirst();
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
                    "Dataset contains an invalid " + field);
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
                    "SHA-256 is unavailable", exception);
        }
    }

    private static final class ScanProcessingException
            extends RuntimeException {
        private ScanProcessingException(Throwable cause) {
            super(cause);
        }
    }
}

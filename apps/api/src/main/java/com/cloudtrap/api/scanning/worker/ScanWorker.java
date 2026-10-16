package com.cloudtrap.api.scanning.worker;

import com.cloudtrap.api.scanning.domain.NormalizedAsset;
import com.cloudtrap.api.scanning.domain.RuleFinding;
import com.cloudtrap.api.scanning.risk.DeterministicRiskScorer;
import com.cloudtrap.api.scanning.rules.DeterministicRuleEngine;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;

@Component
public class ScanWorker {
    private final JdbcTemplate jdbc;
    private final ObjectMapper mapper;
    private final DeterministicRuleEngine rules;
    private final DeterministicRiskScorer scorer;
    private final Resource demoDataset;
    private final TransactionTemplate transactionTemplate;
    private final String workerId = UUID.randomUUID().toString();

    public ScanWorker(JdbcTemplate jdbc, ObjectMapper mapper, DeterministicRuleEngine rules,
                      DeterministicRiskScorer scorer, TransactionTemplate transactionTemplate,
                      @Value("classpath:datasets/default-demo.json") Resource demoDataset) {
        this.jdbc = jdbc; this.mapper = mapper; this.rules = rules; this.scorer = scorer;
        this.demoDataset = demoDataset; this.transactionTemplate = transactionTemplate;
    }

    @Scheduled(fixedDelayString = "${cloudtrap.scanning.poll-delay-ms:1000}")
    public void poll() {
        jdbc.update("""
          UPDATE scans SET status='FAILED', completed_at=CURRENT_TIMESTAMP,
            error_code='SCAN_RETRIES_EXHAUSTED',
            safe_error_message='The scan worker stopped before completing this scan.',
            lease_owner=NULL,lease_expires_at=NULL,updated_at=CURRENT_TIMESTAMP
          WHERE status='RUNNING' AND lease_expires_at < CURRENT_TIMESTAMP
            AND attempt_count >= max_attempts
          """);
        Map<String,Object> job = claim();
        if (job == null) return;
        UUID id = (UUID) job.get("id");
        try {
            transactionTemplate.executeWithoutResult(status -> {\n                try { process(id, (UUID) job.get("workspace_id"), (String) job.get("dataset_id")); }\n                catch (Exception ex) { throw new ScanProcessingException(ex); }\n            });
        } catch (Exception ex) {
            // Keep provider details and exception messages out of persisted API responses.
            retry(id, "SCAN_EXECUTION_FAILED", "The scan could not be completed. Retry the scan.");
        }
    }

    @Transactional
    protected Map<String,Object> claim() {
        List<Map<String,Object>> rows = jdbc.queryForList("""
          WITH candidate AS (
            SELECT id FROM scans
            WHERE (status='QUEUED' AND attempt_count < max_attempts)
               OR (status='RUNNING' AND lease_expires_at < CURRENT_TIMESTAMP AND attempt_count < max_attempts)
            ORDER BY queued_at, created_at
            LIMIT 1 FOR UPDATE SKIP LOCKED
          )
          UPDATE scans s SET status='RUNNING', attempt_count=s.attempt_count+1,
            lease_owner=?, lease_expires_at=CURRENT_TIMESTAMP + (? * INTERVAL '1 second'),
            started_at=COALESCE(s.started_at,CURRENT_TIMESTAMP), completed_at=NULL,
            error_code=NULL,safe_error_message=NULL,updated_at=CURRENT_TIMESTAMP
          FROM candidate c WHERE s.id=c.id
          RETURNING s.id,s.workspace_id,s.dataset_id
          """, workerId, 120);
        return rows.isEmpty() ? null : rows.getFirst();
    }

    @Transactional
    protected void process(UUID scanId, UUID workspaceId, String datasetId) throws Exception {
        if (!"default-demo".equals(datasetId)) throw new IllegalArgumentException("Unknown dataset");
        JsonNode root;
        try (var in = demoDataset.getInputStream()) { root = mapper.readTree(in); }
        if (!datasetId.equals(root.path("datasetId").asText())) throw new IllegalStateException("Dataset mismatch");
        JsonNode assets = root.path("assets");
        if (!assets.isArray() || assets.isEmpty() || assets.size() > 5000) throw new IllegalStateException("Invalid bounded dataset");
        int findingCount = 0;
        for (JsonNode node : assets) {
            if (!ownsLease(scanId)) throw new IllegalStateException("Worker lease lost");
            UUID assetId = UUID.nameUUIDFromBytes((workspaceId + ":DEMO:" + node.path("externalId").asText()).getBytes(StandardCharsets.UTF_8));
            String attrs = mapper.writeValueAsString(mapper.convertValue(node.path("attributes"), Map.class));
            String exposure = node.path("exposure").asText("UNKNOWN");
            int criticality = node.path("criticality").asInt(3);
            jdbc.update("""
              INSERT INTO assets(id,workspace_id,provider,external_id,asset_type,name,region,exposure,criticality,normalized_attributes,last_seen_at,updated_at)
              VALUES(?,?,'DEMO',?,?,?,?,?,?,?::jsonb,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP)
              ON CONFLICT(workspace_id,provider,external_id) DO UPDATE SET
                asset_type=EXCLUDED.asset_type,name=EXCLUDED.name,region=EXCLUDED.region,
                exposure=EXCLUDED.exposure,criticality=EXCLUDED.criticality,
                normalized_attributes=EXCLUDED.normalized_attributes,last_seen_at=CURRENT_TIMESTAMP,updated_at=CURRENT_TIMESTAMP
              """, assetId, workspaceId, node.path("externalId").asText(), node.path("assetType").asText(),
                    node.path("name").asText(), node.path("region").asText(null), exposure, criticality, attrs);
            UUID actualAssetId = jdbc.queryForObject("SELECT id FROM assets WHERE workspace_id=? AND provider='DEMO' AND external_id=?",
                    UUID.class, workspaceId, node.path("externalId").asText());
            jdbc.update("INSERT INTO scan_assets(scan_id,asset_id,workspace_id) VALUES(?,?,?) ON CONFLICT DO NOTHING",
                    scanId, actualAssetId, workspaceId);
            Map<String,Object> attributes = mapper.convertValue(node.path("attributes"), Map.class);
            NormalizedAsset asset = new NormalizedAsset(actualAssetId, workspaceId, "DEMO",
                    node.path("externalId").asText(), node.path("assetType").asText(), node.path("name").asText(),
                    node.path("region").asText(null), NormalizedAsset.Exposure.valueOf(exposure), criticality, attributes);
            for (RuleFinding finding : rules.evaluate(asset)) {
                String evidence = mapper.writeValueAsString(finding.evidence());
                String dedupe = sha256(workspaceId + "|" + actualAssetId + "|" + finding.ruleId() + "|" + finding.ruleVersion());
                UUID findingId = UUID.nameUUIDFromBytes((scanId + "|" + actualAssetId + "|" + finding.ruleId() + "|" + finding.ruleVersion()).getBytes(StandardCharsets.UTF_8));
                int inserted = jdbc.update("""
                  INSERT INTO findings(id,workspace_id,scan_id,asset_id,rule_id,rule_version,source_type,title,description,category,severity,status,deduplication_key,evidence)
                  VALUES(?,?,?, ?,?,?,'DETERMINISTIC_RULE',?,?,?,?,'OPEN',?,?::jsonb)
                  ON CONFLICT(scan_id,asset_id,rule_id,rule_version) DO NOTHING
                  """, findingId, workspaceId, scanId, actualAssetId, finding.ruleId(), finding.ruleVersion(),
                        finding.title(), finding.description(), finding.category(), finding.severity().name(), dedupe, evidence);
                if (inserted == 1) {
                    var risk = scorer.score(finding, asset);
                    jdbc.update("INSERT INTO risk_assessments(id,workspace_id,finding_id,score,severity,factors,formula_version) VALUES(?,?,?,?,?,?::jsonb,?)",
                            UUID.randomUUID(), workspaceId, findingId, risk.score(), finding.severity().name(), mapper.writeValueAsString(risk.factors()), risk.formulaVersion());
                    jdbc.update("INSERT INTO remediations(id,workspace_id,finding_id,summary,steps,priority,source_type,requires_human_review) VALUES(?,?,?,?,?::jsonb,?,'RULE',TRUE)",
                            UUID.randomUUID(), workspaceId, findingId, finding.remediationSummary(),
                            mapper.writeValueAsString(finding.remediationSteps()), finding.severity().name().equals("INFO") ? "LOW" : finding.severity().name());
                    findingCount++;
                }
            }
        }
        int updated = jdbc.update("""
          UPDATE scans SET status='COMPLETED',completed_at=CURRENT_TIMESTAMP,lease_owner=NULL,lease_expires_at=NULL,
            assets_discovered=?,assets_scanned=?,findings_created=?,updated_at=CURRENT_TIMESTAMP
          WHERE id=? AND status='RUNNING' AND lease_owner=? AND lease_expires_at>CURRENT_TIMESTAMP
          """, assets.size(), assets.size(), findingCount, scanId, workerId);
        if (updated != 1) throw new IllegalStateException("Lease lost before commit");
    }

    private boolean ownsLease(UUID id) {
        Boolean result = jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM scans WHERE id=? AND status='RUNNING' AND lease_owner=? AND lease_expires_at>CURRENT_TIMESTAMP)", Boolean.class, id, workerId);
        return Boolean.TRUE.equals(result);
    }

    @Transactional
    protected void retry(UUID id, String code, String message) {
        jdbc.update("""
          UPDATE scans SET status=CASE WHEN attempt_count>=max_attempts THEN 'FAILED' ELSE 'QUEUED' END,
            completed_at=CASE WHEN attempt_count>=max_attempts THEN CURRENT_TIMESTAMP ELSE NULL END,
            error_code=CASE WHEN attempt_count>=max_attempts THEN ? ELSE NULL END,
            safe_error_message=CASE WHEN attempt_count>=max_attempts THEN ? ELSE NULL END,
            lease_owner=NULL,lease_expires_at=NULL,updated_at=CURRENT_TIMESTAMP
          WHERE id=? AND status='RUNNING' AND lease_owner=?
          """, code, message, id, workerId);
    }

    private static final class ScanProcessingException extends RuntimeException {\n        private ScanProcessingException(Throwable cause) { super(cause); }\n    }\n\n    private static String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) { throw new IllegalStateException("SHA-256 unavailable", e); }
    }
}

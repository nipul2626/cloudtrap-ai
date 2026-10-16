package com.cloudtrap.api.scanning.persistence;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "scans")
public class ScanEntity {
    @Id private UUID id;
    @Column(name = "workspace_id", nullable = false, updatable = false) private UUID workspaceId;
    @Column(name = "requested_by", nullable = false, updatable = false) private UUID requestedBy;
    @Column(name = "source", nullable = false, length = 20, updatable = false) private String source;
    @Column(name = "dataset_id", nullable = false, length = 120, updatable = false) private String datasetId;
    @Column(name = "status", nullable = false, length = 20) private String status;
    @Column(name = "rule_set_version", nullable = false, length = 80, updatable = false) private String ruleSetVersion;
    @Column(name = "risk_formula_version", nullable = false, length = 40, updatable = false) private String riskFormulaVersion;
    @Column(name = "idempotency_key", length = 128, updatable = false) private String idempotencyKey;
    @Column(name = "request_fingerprint", nullable = false, length = 64, updatable = false) private String requestFingerprint;
    @Column(name = "attempt_count", nullable = false) private short attemptCount;
    @Column(name = "max_attempts", nullable = false) private short maxAttempts;
    @Column(name = "lease_owner", length = 200) private String leaseOwner;
    @Column(name = "lease_expires_at") private Instant leaseExpiresAt;
    @Column(name = "queued_at", nullable = false, updatable = false) private Instant queuedAt;
    @Column(name = "started_at") private Instant startedAt;
    @Column(name = "completed_at") private Instant completedAt;
    @Column(name = "assets_discovered", nullable = false) private int assetsDiscovered;
    @Column(name = "assets_scanned", nullable = false) private int assetsScanned;
    @Column(name = "findings_created", nullable = false) private int findingsCreated;
    @Column(name = "error_code", length = 80) private String errorCode;
    @Column(name = "safe_error_message", length = 500) private String safeErrorMessage;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;

    protected ScanEntity() {}
    public static ScanEntity queued(UUID workspaceId, UUID requestedBy, String datasetId, String ruleSetVersion, String idempotencyKey, String fingerprint) {
        ScanEntity s = new ScanEntity(); s.id=UUID.randomUUID(); s.workspaceId=workspaceId; s.requestedBy=requestedBy;
        s.source="DEMO"; s.datasetId=datasetId; s.status="QUEUED"; s.ruleSetVersion=ruleSetVersion;
        s.riskFormulaVersion="cloudtrap-risk-v1"; s.idempotencyKey=idempotencyKey; s.requestFingerprint=fingerprint;
        s.maxAttempts=3; s.queuedAt=Instant.now(); s.createdAt=s.queuedAt; s.updatedAt=s.queuedAt; return s;
    }
    @PreUpdate protected void onUpdate(){updatedAt=Instant.now();}
    public UUID getId(){return id;} public UUID getWorkspaceId(){return workspaceId;} public String getStatus(){return status;}
    public String getDatasetId(){return datasetId;} public String getRuleSetVersion(){return ruleSetVersion;}
    public Instant getQueuedAt(){return queuedAt;} public Instant getStartedAt(){return startedAt;} public Instant getCompletedAt(){return completedAt;}
    public int getAssetsDiscovered(){return assetsDiscovered;} public int getAssetsScanned(){return assetsScanned;}
    public int getFindingsCreated(){return findingsCreated;} public String getErrorCode(){return errorCode;}
    public String getSafeErrorMessage(){return safeErrorMessage;} public int getAttemptCount(){return attemptCount;}
}

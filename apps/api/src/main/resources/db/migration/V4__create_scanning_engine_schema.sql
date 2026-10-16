-- V4: durable scan jobs, normalized inventory, deterministic findings and risk history.
-- Additive migration: do not modify V1-V3.

CREATE TABLE assets (
    id UUID PRIMARY KEY,
    workspace_id UUID NOT NULL,
    cloud_account_id UUID,
    provider VARCHAR(30) NOT NULL,
    external_id VARCHAR(512) NOT NULL,
    asset_type VARCHAR(100) NOT NULL,
    name VARCHAR(255) NOT NULL,
    region VARCHAR(120),
    exposure VARCHAR(20) NOT NULL DEFAULT 'UNKNOWN',
    criticality SMALLINT NOT NULL DEFAULT 3,
    normalized_attributes JSONB NOT NULL DEFAULT '{}'::jsonb,
    first_seen_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_seen_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_assets_workspace FOREIGN KEY (workspace_id) REFERENCES workspaces(id),
    CONSTRAINT fk_assets_cloud_account FOREIGN KEY (cloud_account_id) REFERENCES cloud_accounts(id),
    CONSTRAINT uq_assets_identity UNIQUE (workspace_id, provider, external_id),
    CONSTRAINT chk_assets_provider CHECK (provider IN ('AWS', 'AZURE', 'GCP', 'DEMO')),
    CONSTRAINT chk_assets_exposure CHECK (exposure IN ('PUBLIC', 'PRIVATE', 'UNKNOWN')),
    CONSTRAINT chk_assets_criticality CHECK (criticality BETWEEN 1 AND 5),
    CONSTRAINT chk_assets_attributes_object CHECK (jsonb_typeof(normalized_attributes) = 'object')
);

CREATE INDEX idx_assets_workspace_type ON assets(workspace_id, asset_type, last_seen_at DESC);
CREATE INDEX idx_assets_workspace_exposure ON assets(workspace_id, exposure);
CREATE INDEX idx_assets_cloud_account ON assets(cloud_account_id) WHERE cloud_account_id IS NOT NULL;

CREATE TABLE asset_relationships (
    id UUID PRIMARY KEY,
    workspace_id UUID NOT NULL,
    source_asset_id UUID NOT NULL,
    target_asset_id UUID NOT NULL,
    relationship_type VARCHAR(80) NOT NULL,
    attributes JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_asset_relationships_workspace FOREIGN KEY (workspace_id) REFERENCES workspaces(id),
    CONSTRAINT fk_asset_relationships_source FOREIGN KEY (source_asset_id) REFERENCES assets(id),
    CONSTRAINT fk_asset_relationships_target FOREIGN KEY (target_asset_id) REFERENCES assets(id),
    CONSTRAINT uq_asset_relationship UNIQUE (workspace_id, source_asset_id, target_asset_id, relationship_type),
    CONSTRAINT chk_asset_relationship_not_self CHECK (source_asset_id <> target_asset_id),
    CONSTRAINT chk_asset_relationship_attributes_object CHECK (jsonb_typeof(attributes) = 'object')
);

CREATE INDEX idx_asset_relationships_source ON asset_relationships(workspace_id, source_asset_id);
CREATE INDEX idx_asset_relationships_target ON asset_relationships(workspace_id, target_asset_id);

CREATE TABLE scans (
    id UUID PRIMARY KEY,
    workspace_id UUID NOT NULL,
    cloud_account_id UUID,
    requested_by UUID NOT NULL,
    source VARCHAR(20) NOT NULL,
    dataset_id VARCHAR(120) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'QUEUED',
    rule_set_version VARCHAR(80) NOT NULL,
    risk_formula_version VARCHAR(40) NOT NULL,
    idempotency_key VARCHAR(128),
    request_fingerprint CHAR(64) NOT NULL,
    attempt_count SMALLINT NOT NULL DEFAULT 0,
    max_attempts SMALLINT NOT NULL DEFAULT 3,
    lease_owner VARCHAR(200),
    lease_expires_at TIMESTAMPTZ,
    queued_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    started_at TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,
    assets_discovered INTEGER NOT NULL DEFAULT 0,
    assets_scanned INTEGER NOT NULL DEFAULT 0,
    findings_created INTEGER NOT NULL DEFAULT 0,
    error_code VARCHAR(80),
    safe_error_message VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_scans_workspace FOREIGN KEY (workspace_id) REFERENCES workspaces(id),
    CONSTRAINT fk_scans_cloud_account FOREIGN KEY (cloud_account_id) REFERENCES cloud_accounts(id),
    CONSTRAINT fk_scans_requested_by FOREIGN KEY (requested_by) REFERENCES users(id),
    CONSTRAINT chk_scans_source CHECK (source IN ('DEMO', 'AWS', 'AZURE', 'GCP')),
    CONSTRAINT chk_scans_status CHECK (status IN ('QUEUED', 'RUNNING', 'COMPLETED', 'FAILED')),
    CONSTRAINT chk_scans_attempts CHECK (attempt_count >= 0 AND max_attempts BETWEEN 1 AND 10),
    CONSTRAINT chk_scans_counts CHECK (assets_discovered >= 0 AND assets_scanned >= 0 AND findings_created >= 0),
    CONSTRAINT chk_scans_terminal_timestamp CHECK (
        (status IN ('COMPLETED', 'FAILED') AND completed_at IS NOT NULL)
        OR (status IN ('QUEUED', 'RUNNING') AND completed_at IS NULL)
    ),
    CONSTRAINT chk_scans_failure_details CHECK (
        status <> 'FAILED' OR (error_code IS NOT NULL AND safe_error_message IS NOT NULL)
    )
);

CREATE UNIQUE INDEX uq_scans_idempotency ON scans(workspace_id, idempotency_key) WHERE idempotency_key IS NOT NULL;
CREATE INDEX idx_scans_worker_queue ON scans(status, queued_at, created_at) WHERE status = 'QUEUED';
CREATE INDEX idx_scans_expired_leases ON scans(lease_expires_at) WHERE status = 'RUNNING';
CREATE INDEX idx_scans_workspace_history ON scans(workspace_id, created_at DESC, id DESC);

CREATE TABLE scan_assets (
    scan_id UUID NOT NULL,
    asset_id UUID NOT NULL,
    workspace_id UUID NOT NULL,
    observed_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (scan_id, asset_id),
    CONSTRAINT fk_scan_assets_scan FOREIGN KEY (scan_id) REFERENCES scans(id),
    CONSTRAINT fk_scan_assets_asset FOREIGN KEY (asset_id) REFERENCES assets(id),
    CONSTRAINT fk_scan_assets_workspace FOREIGN KEY (workspace_id) REFERENCES workspaces(id)
);
CREATE INDEX idx_scan_assets_workspace ON scan_assets(workspace_id, asset_id);

CREATE TABLE security_rules (
    id UUID PRIMARY KEY,
    rule_id VARCHAR(120) NOT NULL,
    version VARCHAR(40) NOT NULL,
    title VARCHAR(240) NOT NULL,
    description TEXT NOT NULL,
    category VARCHAR(100) NOT NULL,
    severity VARCHAR(20) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    tags JSONB NOT NULL DEFAULT '[]'::jsonb,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_security_rules_version UNIQUE (rule_id, version),
    CONSTRAINT chk_security_rules_severity CHECK (severity IN ('CRITICAL', 'HIGH', 'MEDIUM', 'LOW', 'INFO')),
    CONSTRAINT chk_security_rules_tags_array CHECK (jsonb_typeof(tags) = 'array')
);
CREATE INDEX idx_security_rules_enabled ON security_rules(enabled, rule_id);

CREATE TABLE findings (
    id UUID PRIMARY KEY,
    workspace_id UUID NOT NULL,
    scan_id UUID NOT NULL,
    asset_id UUID NOT NULL,
    rule_id VARCHAR(120) NOT NULL,
    rule_version VARCHAR(40) NOT NULL,
    source_type VARCHAR(30) NOT NULL DEFAULT 'DETERMINISTIC_RULE',
    title VARCHAR(240) NOT NULL,
    description TEXT NOT NULL,
    category VARCHAR(100) NOT NULL,
    severity VARCHAR(20) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'OPEN',
    deduplication_key CHAR(64) NOT NULL,
    evidence JSONB NOT NULL,
    first_detected_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_detected_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    resolved_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_findings_workspace FOREIGN KEY (workspace_id) REFERENCES workspaces(id),
    CONSTRAINT fk_findings_scan FOREIGN KEY (scan_id) REFERENCES scans(id),
    CONSTRAINT fk_findings_asset FOREIGN KEY (asset_id) REFERENCES assets(id),
    CONSTRAINT fk_findings_rule FOREIGN KEY (rule_id, rule_version) REFERENCES security_rules(rule_id, version),
    CONSTRAINT uq_findings_scan_rule_asset UNIQUE (scan_id, asset_id, rule_id, rule_version),
    CONSTRAINT chk_findings_source CHECK (source_type IN ('DETERMINISTIC_RULE', 'AI_SUGGESTED')),
    CONSTRAINT chk_findings_severity CHECK (severity IN ('CRITICAL', 'HIGH', 'MEDIUM', 'LOW', 'INFO')),
    CONSTRAINT chk_findings_status CHECK (status IN ('OPEN', 'ACKNOWLEDGED', 'RESOLVED', 'ACCEPTED_RISK')),
    CONSTRAINT chk_findings_evidence_array CHECK (jsonb_typeof(evidence) = 'array')
);
CREATE INDEX idx_findings_workspace_status_severity ON findings(workspace_id, status, severity, last_detected_at DESC);
CREATE INDEX idx_findings_workspace_scan ON findings(workspace_id, scan_id);
CREATE INDEX idx_findings_workspace_asset ON findings(workspace_id, asset_id);
CREATE INDEX idx_findings_rule_version ON findings(rule_id, rule_version);

CREATE TABLE risk_assessments (
    id UUID PRIMARY KEY,
    workspace_id UUID NOT NULL,
    finding_id UUID NOT NULL,
    score SMALLINT NOT NULL,
    severity VARCHAR(20) NOT NULL,
    factors JSONB NOT NULL,
    formula_version VARCHAR(40) NOT NULL,
    calculated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_risk_assessments_workspace FOREIGN KEY (workspace_id) REFERENCES workspaces(id),
    CONSTRAINT fk_risk_assessments_finding FOREIGN KEY (finding_id) REFERENCES findings(id),
    CONSTRAINT chk_risk_assessments_score CHECK (score BETWEEN 0 AND 100),
    CONSTRAINT chk_risk_assessments_severity CHECK (severity IN ('CRITICAL', 'HIGH', 'MEDIUM', 'LOW', 'INFO')),
    CONSTRAINT chk_risk_assessments_factors_array CHECK (jsonb_typeof(factors) = 'array')
);
CREATE INDEX idx_risk_assessments_finding_current ON risk_assessments(finding_id, calculated_at DESC, id DESC);

CREATE TABLE remediations (
    id UUID PRIMARY KEY,
    workspace_id UUID NOT NULL,
    finding_id UUID NOT NULL,
    summary VARCHAR(500) NOT NULL,
    steps JSONB NOT NULL,
    priority VARCHAR(20) NOT NULL,
    source_type VARCHAR(20) NOT NULL DEFAULT 'RULE',
    requires_human_review BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_remediations_workspace FOREIGN KEY (workspace_id) REFERENCES workspaces(id),
    CONSTRAINT fk_remediations_finding FOREIGN KEY (finding_id) REFERENCES findings(id),
    CONSTRAINT chk_remediations_priority CHECK (priority IN ('CRITICAL', 'HIGH', 'MEDIUM', 'LOW')),
    CONSTRAINT chk_remediations_source CHECK (source_type IN ('RULE', 'AI', 'MANUAL')),
    CONSTRAINT chk_remediations_steps_array CHECK (jsonb_typeof(steps) = 'array')
);
CREATE INDEX idx_remediations_finding ON remediations(finding_id, created_at DESC);

CREATE TABLE audit_events (
    id UUID PRIMARY KEY,
    workspace_id UUID,
    actor_user_id UUID,
    action VARCHAR(120) NOT NULL,
    entity_type VARCHAR(100),
    entity_id UUID,
    outcome VARCHAR(20) NOT NULL,
    request_id VARCHAR(100),
    details JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_audit_events_workspace FOREIGN KEY (workspace_id) REFERENCES workspaces(id),
    CONSTRAINT fk_audit_events_actor FOREIGN KEY (actor_user_id) REFERENCES users(id),
    CONSTRAINT chk_audit_events_outcome CHECK (outcome IN ('SUCCESS', 'FAILURE')),
    CONSTRAINT chk_audit_events_details_object CHECK (details IS NULL OR jsonb_typeof(details) = 'object')
);
CREATE INDEX idx_audit_events_workspace_time ON audit_events(workspace_id, created_at DESC) WHERE workspace_id IS NOT NULL;
CREATE INDEX idx_audit_events_entity ON audit_events(entity_type, entity_id, created_at DESC);

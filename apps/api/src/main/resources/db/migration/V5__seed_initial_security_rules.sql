-- V5: immutable versioned metadata for the initial deterministic rule pack.
-- Rule IDs and versions are referenced by findings; never rewrite historical versions.

INSERT INTO security_rules
    (id, rule_id, version, title, description, category, severity, enabled, tags)
VALUES
    ('00000000-0000-4000-8000-000000000101', 'CLOUDTRAP-ASSET-001', '1.0.0',
     'Asset is publicly exposed',
     'The normalized inventory reports public exposure.',
     'EXPOSURE', 'HIGH', TRUE, '["exposure","public-access"]'::jsonb),
    ('00000000-0000-4000-8000-000000000102', 'CLOUDTRAP-STORAGE-001', '1.0.0',
     'Storage encryption is disabled',
     'The normalized inventory reports encryption at rest disabled.',
     'DATA_PROTECTION', 'HIGH', TRUE, '["encryption","storage"]'::jsonb),
    ('00000000-0000-4000-8000-000000000103', 'CLOUDTRAP-NETWORK-001', '1.0.0',
     'Administrative network access is unrestricted',
     'The normalized inventory identifies an administrative port reachable from an unrestricted source range.',
     'NETWORK_SECURITY', 'CRITICAL', TRUE, '["network","ingress"]'::jsonb),
    ('00000000-0000-4000-8000-000000000104', 'CLOUDTRAP-AUDIT-001', '1.0.0',
     'Audit logging is disabled',
     'The normalized inventory reports audit logging disabled.',
     'DETECTION_AND_RESPONSE', 'HIGH', TRUE, '["audit","logging"]'::jsonb);

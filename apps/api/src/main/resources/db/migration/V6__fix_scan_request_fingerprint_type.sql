
-- Align the scans column with ScanEntity's VARCHAR(64) mapping.
-- Preserve existing values and the existing column constraints.

ALTER TABLE scans
ALTER COLUMN request_fingerprint TYPE VARCHAR(64)
    USING BTRIM(request_fingerprint)::VARCHAR(64);

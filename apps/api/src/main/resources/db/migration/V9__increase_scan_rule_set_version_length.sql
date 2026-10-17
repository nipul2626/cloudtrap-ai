-- Allow longer snapshots of enabled security rule versions.
ALTER TABLE scans
    ALTER COLUMN rule_set_version TYPE VARCHAR(1024);

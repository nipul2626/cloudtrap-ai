
-- V7: persist the provider-neutral scope for real cloud scans.
-- Keep V1-V6 unchanged; existing DEMO scans use safe defaults.

ALTER TABLE scans
    ADD COLUMN collector_id VARCHAR(120),
    ADD COLUMN collector_version VARCHAR(40),
    ADD COLUMN collection_regions JSONB NOT NULL DEFAULT '[]'::jsonb,
    ADD COLUMN collection_resource_types JSONB NOT NULL DEFAULT '[]'::jsonb,
    ADD COLUMN maximum_assets INTEGER,
    ADD COLUMN collection_timeout_seconds INTEGER,
    ADD COLUMN collection_coverage VARCHAR(20),
    ADD COLUMN collection_errors JSONB NOT NULL DEFAULT '[]'::jsonb;

ALTER TABLE scans
    ADD CONSTRAINT chk_scans_collection_regions_array
        CHECK (jsonb_typeof(collection_regions) = 'array'),
    ADD CONSTRAINT chk_scans_collection_resource_types_array
        CHECK (jsonb_typeof(collection_resource_types) = 'array'),
    ADD CONSTRAINT chk_scans_collection_errors_array
        CHECK (jsonb_typeof(collection_errors) = 'array'),
    ADD CONSTRAINT chk_scans_collection_coverage
        CHECK (
            collection_coverage IS NULL
            OR collection_coverage IN ('COMPLETE', 'PARTIAL', 'FAILED')
        ),
    ADD CONSTRAINT chk_scans_maximum_assets
        CHECK (
            maximum_assets IS NULL
            OR maximum_assets BETWEEN 1 AND 100000
        ),
    ADD CONSTRAINT chk_scans_collection_timeout
        CHECK (
            collection_timeout_seconds IS NULL
            OR collection_timeout_seconds BETWEEN 1 AND 1800
        ),
    ADD CONSTRAINT chk_scans_provider_collection_contract
        CHECK (
            source = 'DEMO'
            OR (
                collector_id IS NOT NULL
                AND collector_version IS NOT NULL
                AND maximum_assets IS NOT NULL
                AND collection_timeout_seconds IS NOT NULL
            )
        );

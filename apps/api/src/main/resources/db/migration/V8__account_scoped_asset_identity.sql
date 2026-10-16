
-- V8: make cloud asset identity account-scoped.
-- V1-V7 remain unchanged.
--
-- DEMO assets retain workspace/provider/external_id identity.
-- Cloud assets are uniquely identified within their cloud account.

ALTER TABLE assets
DROP CONSTRAINT uq_assets_identity;

CREATE UNIQUE INDEX uq_assets_account_identity
    ON assets (
               workspace_id,
               cloud_account_id,
               provider,
               external_id
        )
    WHERE cloud_account_id IS NOT NULL;

CREATE UNIQUE INDEX uq_assets_unscoped_identity
    ON assets (
               workspace_id,
               provider,
               external_id
        )
    WHERE cloud_account_id IS NULL;

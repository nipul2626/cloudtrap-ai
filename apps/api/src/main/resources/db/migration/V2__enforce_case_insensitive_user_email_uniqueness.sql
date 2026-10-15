
-- Enforce case-insensitive uniqueness for user email addresses.
-- Keep the existing V1 migration unchanged.

CREATE UNIQUE INDEX uq_users_email_lower
    ON users (LOWER(email));

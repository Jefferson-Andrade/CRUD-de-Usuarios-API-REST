-- Existing records receive the migration transaction timestamp, not their unknown historical dates.
-- Defaults also support inserts made outside JPA; updates outside JPA must set updated_at explicitly.
ALTER TABLE users
    ADD COLUMN created_at TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ADD COLUMN updated_at TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP;

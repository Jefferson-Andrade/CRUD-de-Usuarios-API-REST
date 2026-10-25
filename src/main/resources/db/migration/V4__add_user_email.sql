-- Preserve legacy users without inventing personal information.
ALTER TABLE users ADD COLUMN email VARCHAR(254);
-- PostgreSQL permits multiple NULLs. The index also protects concurrent writes
-- and direct SQL with different letter case.
CREATE UNIQUE INDEX uk_users_email ON users (lower(email));

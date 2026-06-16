-- ============================================================================
-- FIH 2025 Companion — Read-only database role
-- ----------------------------------------------------------------------------
-- This role is THE safety net. Even if application code had a bug that tried
-- to write, PostgreSQL itself rejects it because this role only has SELECT.
--
-- Run this against your LOCAL dev database, connected as a superuser (postgres):
--   psql -U postgres -d billeterie_fih_dev -f db/01_create_readonly_role.sql
--
-- For production later, run the SAME script against the shared DB, changing the
-- database name in the CONNECT grant below to the production database name.
-- ============================================================================

-- 1) Create the login role. Change the password before using it anywhere real.
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'fih_ro') THEN
        CREATE ROLE fih_ro LOGIN PASSWORD 'fih_ro_pwd';
    END IF;
END
$$;

-- 2) Allow it to connect to THIS database (change name for production).
GRANT CONNECT ON DATABASE billeterie_fih_dev TO fih_ro;

-- 3) Allow it to "see" the public schema (USAGE = enter the schema; no write).
GRANT USAGE ON SCHEMA public TO fih_ro;

-- 4) Allow SELECT on every table that exists RIGHT NOW.
GRANT SELECT ON ALL TABLES IN SCHEMA public TO fih_ro;

-- 5) Allow SELECT on any table created LATER (e.g. if the legacy team adds one).
ALTER DEFAULT PRIVILEGES IN SCHEMA public
    GRANT SELECT ON TABLES TO fih_ro;

-- 6) Note what we deliberately DID NOT grant:
--    no INSERT/UPDATE/DELETE/TRUNCATE on tables,
--    no USAGE/UPDATE on sequences (so it cannot consume IDs),
--    no CREATE on the schema (so it cannot make new objects),
--    no DDL of any kind. SELECT is the entire surface area.

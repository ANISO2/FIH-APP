-- ============================================================================
--  FIH Companion — remove the DEMO today data created by insert_today_demo.sql.
--    psql -U postgres -d billeterie_fih_dev -f cleanup_today_demo.sql
--  Deletes in FK-safe order. Touches ONLY rows tied to 'DEMO Aujourd''hui%'
--  events (the legacy data is untouched).
-- ============================================================================
\set ON_ERROR_STOP on
BEGIN;

DO $$
DECLARE r record;
BEGIN
    FOR r IN SELECT reference FROM evenement WHERE titre LIKE 'DEMO Aujourd''hui%' LOOP
        DELETE FROM tturnstile
         WHERE billet  IN (SELECT numeroserie FROM billet  WHERE evenement = r.reference)
            OR voucher IN (SELECT numeroserie FROM voucher WHERE evenement = r.reference);
        DELETE FROM billet     WHERE evenement = r.reference;
        DELETE FROM voucher    WHERE evenement = r.reference;
        DELETE FROM generation WHERE evenement = r.reference;
        DELETE FROM evenement  WHERE reference = r.reference;
        RAISE NOTICE 'Removed demo event #%', r.reference;
    END LOOP;
END $$;

COMMIT;

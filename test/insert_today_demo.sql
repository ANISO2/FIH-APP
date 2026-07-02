-- ============================================================================
--  FIH Companion — insert a TODAY demo event so the stats light up
--  (mobile dashboard "Aujourd'hui", the new tourniquet details screen, and the
--  backoffice "Statistique des tourniquets").
--
--  Run as the postgres superuser (the app's fih_ro role is read-only):
--    psql -U postgres -d billeterie_fih_dev -f insert_today_demo.sql
--
--  Mirrors the backoffice screenshot: 140 émis / 77 entrées / 55,0 % présence.
--    - "Billet Gradins" (model 22): 100 vouchers, 63 scanned
--    - "Invitation PDF" (model 36): 40 billets, 14 scanned
--  Each run makes a NEW today event (unique title) so it is safe to re-run.
--  Remove demo data later with cleanup_today_demo.sql.
-- ============================================================================
\set ON_ERROR_STOP on
BEGIN;

DO $$
DECLARE
    v_loc        int;
    v_evid       int;
    v_txbase     int;
    -- ----- demo sizes (tweak freely) -----
    n_voucher    int := 100;  -- Billet Gradins (model 22), issued as vouchers
    n_voucher_tx int := 63;   -- of which scanned (entrées)
    n_billet     int := 40;   -- Invitation PDF (model 36), issued as billets
    n_billet_tx  int := 14;   -- of which scanned (entrées)
    m_voucher    int := 22;
    m_billet     int := 36;
BEGIN
    -- 1) a turnstile location (reuse the existing one)
    SELECT reference INTO v_loc FROM location WHERE turnstile ORDER BY reference LIMIT 1;
    IF v_loc IS NULL THEN
        SELECT reference INTO v_loc FROM location ORDER BY reference LIMIT 1;
    END IF;

    -- 2) the TODAY event
    v_evid := (SELECT COALESCE(MAX(reference),0)+1 FROM evenement);
    INSERT INTO evenement(reference, billet, ddate, titre, voucher, location)
    VALUES (v_evid, true, CURRENT_DATE,
            'DEMO Aujourd''hui ('||to_char(CURRENT_DATE,'DD/MM')||') #'||v_evid,
            true, v_loc);

    -- 3) generation rows — REQUIRED: billet/voucher FK references
    --    generation(evenement, modelebillet).
    INSERT INTO generation(activation, counterbillet, counterkit, countervoucher,
                           prefixe, prix, stockbillet, stockvoucher, web,
                           evenement, modelebillet, client_reference)
    VALUES
      (true, 0, 0, 0, 'DEMO-'||v_evid||'-'||m_voucher, 30.0, 0, n_voucher,
       'demo'||v_evid||'-'||m_voucher, v_evid, m_voucher, NULL),
      (true, 0, 0, 0, 'DEMO-'||v_evid||'-'||m_billet, 0.0, n_billet, 0,
       'demo'||v_evid||'-'||m_billet, v_evid, m_billet, NULL);

    -- 4) issued codes ("Code à barre accessibles")
    INSERT INTO voucher(numeroserie, accesscounter, activation, codebarre,
                        datevente, reservation, utilisation, vendu,
                        evenement, modelebillet)
    SELECT 'DEMOV'||v_evid||'-'||g, 1, true, 'CBV'||v_evid||'-'||g,
           CURRENT_DATE, false, true, true, v_evid, m_voucher
    FROM generate_series(1, n_voucher) g;

    INSERT INTO billet(numeroserie, activation, codebarre, etatlivraison,
                       nombreacces, reservation, utilisation, vendu,
                       evenement, modelebillet)
    SELECT 'DEMOB'||v_evid||'-'||g, true, 'CBB'||v_evid||'-'||g, true,
           1, false, true, true, v_evid, m_billet
    FROM generate_series(1, n_billet) g;

    -- 5) turnstile passages ("Transactions tourniquet" / entrées).
    --    transactionstate = true  => granted = entrée.
    v_txbase := (SELECT COALESCE(MAX(reference),0) FROM tturnstile);
    INSERT INTO tturnstile(reference, codebarre, datetransaction, description,
                           heuretransaction, porte, transactionstate,
                           billet, location, voucher)
    SELECT v_txbase + g, 'CBV'||v_evid||'-'||g, CURRENT_DATE, 'Acces autorise',
           now(), 'Porte A', true, NULL, v_loc, 'DEMOV'||v_evid||'-'||g
    FROM generate_series(1, n_voucher_tx) g;

    v_txbase := (SELECT COALESCE(MAX(reference),0) FROM tturnstile);
    INSERT INTO tturnstile(reference, codebarre, datetransaction, description,
                           heuretransaction, porte, transactionstate,
                           billet, location, voucher)
    SELECT v_txbase + g, 'CBB'||v_evid||'-'||g, CURRENT_DATE, 'Acces autorise',
           now(), 'Porte B', true, 'DEMOB'||v_evid||'-'||g, v_loc, NULL
    FROM generate_series(1, n_billet_tx) g;

    RAISE NOTICE 'Demo event #% for % : % émis, % entrées.',
        v_evid, CURRENT_DATE, n_voucher + n_billet, n_voucher_tx + n_billet_tx;
END $$;

COMMIT;

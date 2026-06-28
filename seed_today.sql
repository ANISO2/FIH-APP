-- ============================================================================
-- Seed TODAY's turnstile data so the mobile "Aujourd'hui" dashboard goes live.
-- Run as a WRITE-capable role (e.g. postgres / the DB owner) — NOT fih_ro,
-- which is read-only. Safe & reversible: every row is tagged so clean_today.sql
-- removes exactly what this added.
--
-- Why an event + turnstile rows? entries-by-day does:
--   FROM evenement e JOIN tturnstile t ON t.datetransaction = e.ddate
-- so "today" only appears when an EVENT is dated today AND there are turnstile
-- rows dated today.
-- ============================================================================

-- 0) Make sure the auto-increment sequences are ahead of existing data
--    (harmless if already correct; prevents primary-key collisions).
SELECT setval('public.tturnstile_reference_seq', (SELECT MAX(reference) FROM public.tturnstile));
SELECT setval('public.evenement_reference_seq',  (SELECT MAX(reference) FROM public.evenement));

-- 1) Exactly ONE event dated today (guarded so re-running won't duplicate it,
--    which would double-count via the date join).
INSERT INTO public.evenement (billet, ddate, titre, voucher, location)
SELECT true, CURRENT_DATE, 'TEST LIVE ' || to_char(CURRENT_DATE, 'YYYY-MM-DD'), false, 1
WHERE NOT EXISTS (SELECT 1 FROM public.evenement WHERE ddate = CURRENT_DATE);

-- 2) 40 ACCEPTED entries today (transactionstate = true), spread over the last
--    ~13 min so heuretransaction looks realistic.
INSERT INTO public.tturnstile
  (codebarre, datetransaction, description, heuretransaction, porte, transactionstate, billet, location)
SELECT '700824QD', CURRENT_DATE, 'TEST_LIVE',
       now() - (g * interval '20 seconds'), 'Public', true, '8250000019', 1
FROM generate_series(1, 40) AS g;

-- 3) 6 REFUSED entries today (transactionstate = false).
INSERT INTO public.tturnstile
  (codebarre, datetransaction, description, heuretransaction, porte, transactionstate, billet, location)
SELECT '700824QD', CURRENT_DATE, 'TEST_LIVE', now(), 'Public', false, '8250000019', 1
FROM generate_series(1, 6) AS g;

-- 4) Confirm what today now holds.
SELECT count(*) FILTER (WHERE transactionstate)       AS entrees,
       count(*) FILTER (WHERE NOT transactionstate)   AS refuses,
       count(*)                                        AS total
FROM public.tturnstile
WHERE datetransaction = CURRENT_DATE AND description = 'TEST_LIVE';

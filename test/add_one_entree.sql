-- Scan one more (unused) voucher of the latest DEMO today event => +1 entrée.
-- Run repeatedly to watch the mobile dashboard tick up live (polls ~15s).
--   psql -U postgres -d billeterie_fih_dev -f add_one_entree.sql
\set ON_ERROR_STOP on
INSERT INTO tturnstile(reference, codebarre, datetransaction, description,
                       heuretransaction, porte, transactionstate, billet, location, voucher)
SELECT (SELECT COALESCE(MAX(reference),0)+1 FROM tturnstile),
       v.codebarre, CURRENT_DATE, 'Acces autorise', now(), 'Porte A', true,
       NULL, e.location, v.numeroserie
FROM evenement e
JOIN voucher v ON v.evenement = e.reference
WHERE e.titre LIKE 'DEMO Aujourd''hui%'
  AND v.numeroserie NOT IN (SELECT voucher FROM tturnstile WHERE voucher IS NOT NULL)
ORDER BY e.reference DESC, v.numeroserie
LIMIT 1;

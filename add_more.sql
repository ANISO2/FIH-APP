-- Run this repeatedly to watch the live counter climb on the phone.
-- Each run adds 5 accepted entries for today. Bump 5 -> 2000 to simulate a rush.
INSERT INTO public.tturnstile
  (codebarre, datetransaction, description, heuretransaction, porte, transactionstate, billet, location)
SELECT '700824QD', CURRENT_DATE, 'TEST_LIVE', now(), 'Public', true, '8250000019', 1
FROM generate_series(1, 5) AS g;

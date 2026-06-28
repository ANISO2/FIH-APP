-- Remove everything the seed added (turnstile rows + the test event).
DELETE FROM public.tturnstile WHERE description = 'TEST_LIVE';
DELETE FROM public.evenement  WHERE titre LIKE 'TEST LIVE %';

package com.fih.companion.verification;

import com.fih.companion.access.AccessZoneResolver;
import com.fih.companion.repository.BilletRepository;
import com.fih.companion.repository.VoucherRepository;
import com.fih.companion.verification.projection.BilletVerifyProjection;
import com.fih.companion.verification.projection.VoucherVerifyProjection;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Computes a verdict for a billet or voucher. Strictly read-only: it reads rows
 * and returns a verdict, but NEVER marks anything used. The legacy turnstile
 * system owns that write.
 *
 * SPIKE FIX (gate opening): each verification is now ONE database round trip.
 * The finder joins billet/voucher + modelebillet + evenement + holder +
 * badge_affectation and returns exactly the fields below; previously this was
 * 5–6 separate queries. Under load it is round trips × concurrency that
 * saturates the shared database and the connection pool, so collapsing them is
 * the single biggest win — each scan now holds a pooled connection for one short
 * read, letting a small pool absorb a large burst.
 *
 * The verdict is still decided HERE, in Java, on every call, from the live
 * counters (utilisation / nombreacces / accesscounter). Nothing about the
 * verdict is cached — a ticket used a moment ago immediately reads ALREADY_USED.
 */
@Service
@Transactional(readOnly = true)
public class VerificationService {

    private final BilletRepository billetRepository;
    private final VoucherRepository voucherRepository;
    private final AccessZoneResolver accessZoneResolver;

    public VerificationService(BilletRepository billetRepository,
                               VoucherRepository voucherRepository,
                               AccessZoneResolver accessZoneResolver) {
        this.billetRepository = billetRepository;
        this.voucherRepository = voucherRepository;
        this.accessZoneResolver = accessZoneResolver;
    }

    // ---------------------------------------------------------------- BILLET
    public VerificationResult verifyBillet(String code) {
        BilletVerifyProjection b = billetRepository.findForVerification(code).orElse(null);
        if (b == null) {
            return VerificationResult.notFound("BILLET", code);
        }

        int maxAccess = b.getMaxAccess() == null ? 0 : b.getMaxAccess();
        int uses = b.getNombreacces() == null ? 0 : b.getNombreacces();
        boolean activation = Boolean.TRUE.equals(b.getActivation());
        boolean utilisation = Boolean.TRUE.equals(b.getUtilisation());
        List<String> zones = accessZoneResolver.resolve(b.getModelId());

        Verdict verdict;
        if (!activation) {
            verdict = Verdict.NOT_ACTIVE;
        } else if (utilisation || (maxAccess > 0 && uses >= maxAccess)) {
            verdict = Verdict.ALREADY_USED;
        } else {
            verdict = Verdict.VALID;
        }

        return new VerificationResult(
                "BILLET",
                verdict,
                b.getNumeroserie(),
                b.getCodebarre(),
                b.getEventTitle(),
                b.getEventDate(),
                b.getModelName(),
                zones,
                maxAccess,
                uses,
                b.getHolderName(),
                b.getAffecteeA(),
                new VerificationResult.Flags(
                        activation,
                        utilisation,
                        Boolean.TRUE.equals(b.getVendu()),
                        Boolean.TRUE.equals(b.getReservation()),
                        false));
    }

    // --------------------------------------------------------------- VOUCHER
    public VerificationResult verifyVoucher(String code) {
        VoucherVerifyProjection v = voucherRepository.findForVerification(code).orElse(null);
        if (v == null) {
            return VerificationResult.notFound("VOUCHER", code);
        }

        int maxAccess = v.getMaxAccess() == null ? 0 : v.getMaxAccess();
        int uses = v.getAccesscounter() == null ? 0 : v.getAccesscounter();
        boolean cancelled = v.getDateannulation() != null;
        boolean active = Boolean.TRUE.equals(v.getActivation());
        boolean used = Boolean.TRUE.equals(v.getUtilisation());
        List<String> zones = accessZoneResolver.resolve(v.getModelId());

        Verdict verdict;
        if (cancelled) {
            verdict = Verdict.CANCELLED;
        } else if (!active) {
            verdict = Verdict.NOT_ACTIVE;
        } else if (used || (maxAccess > 0 && uses >= maxAccess)) {
            verdict = Verdict.ALREADY_USED;
        } else {
            verdict = Verdict.VALID;
        }

        return new VerificationResult(
                "VOUCHER",
                verdict,
                v.getNumeroserie(),
                v.getCodebarre(),
                v.getEventTitle(),
                v.getEventDate(),
                v.getModelName(),
                zones,
                maxAccess,
                uses,
                null,
                v.getAffecteeA(),
                new VerificationResult.Flags(
                        active,
                        used,
                        Boolean.TRUE.equals(v.getVendu()),
                        Boolean.TRUE.equals(v.getReservation()),
                        cancelled));
    }
}

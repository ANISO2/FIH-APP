package com.fih.companion.verification;

import com.fih.companion.access.AccessZoneResolver;
import com.fih.companion.diagnostics.ConsoleLog;
import com.fih.companion.repository.BilletRepository;
import com.fih.companion.repository.VoucherRepository;
import com.fih.companion.verification.projection.BilletVerifyProjection;
import com.fih.companion.verification.projection.VoucherVerifyProjection;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;


@Service
@Transactional(readOnly = true)
public class VerificationService {

    private static final String TAG = "VERIFY";

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
        ConsoleLog.log(TAG, "BILLET step 1 — DB lookup by codebarre/numeroserie for code='" + code + "'.");
        BilletVerifyProjection b;
        try {
            b = billetRepository.findForVerification(code).orElse(null);
        } catch (RuntimeException ex) {
            // Feature 1 — DB connectivity / query failures were the root cause of
            // production 504s; make them scream on the console with a full trace.
            ConsoleLog.error(TAG, "BILLET step 1 FAILED — DB lookup threw for code='" + code
                    + "'. reason=" + ex.getClass().getSimpleName() + ": " + ex.getMessage(), ex);
            throw ex;
        }

        if (b == null) {
            ConsoleLog.log(TAG, "BILLET verdict=NOT_FOUND for code='" + code
                    + "' — reason=no billet row matches this codebarre or numeroserie.");
            return VerificationResult.notFound("BILLET", code);
        }

        int maxAccess = b.getMaxAccess() == null ? 0 : b.getMaxAccess();
        int uses = b.getNombreacces() == null ? 0 : b.getNombreacces();
        boolean activation = Boolean.TRUE.equals(b.getActivation());
        boolean utilisation = Boolean.TRUE.equals(b.getUtilisation());
        List<String> zones = accessZoneResolver.resolve(b.getModelId());
        ConsoleLog.log(TAG, "BILLET step 2 — found numeroserie=" + b.getNumeroserie()
                + ", codebarre=" + b.getCodebarre() + ", model=" + b.getModelId() + " (" + b.getModelName() + ")"
                + ", event='" + b.getEventTitle() + "', zones=" + zones
                + ", flags[activation=" + activation + ", utilisation=" + utilisation
                + ", vendu=" + Boolean.TRUE.equals(b.getVendu()) + ", reservation=" + Boolean.TRUE.equals(b.getReservation())
                + "], access uses=" + uses + "/" + maxAccess + ".");

        Verdict verdict;
        String reason;
        if (!activation) {
            verdict = Verdict.NOT_ACTIVE;
            reason = "activation flag is false (ticket not activated).";
        } else if (utilisation || (maxAccess > 0 && uses >= maxAccess)) {
            verdict = Verdict.ALREADY_USED;
            reason = utilisation
                    ? "utilisation flag is true (already used)."
                    : "access count " + uses + " >= maxaccess " + maxAccess + " (quota exhausted).";
        } else {
            verdict = Verdict.VALID;
            reason = "activated, not used, and within the access quota (" + uses + "/" + maxAccess + ").";
        }
        ConsoleLog.log(TAG, "BILLET step 3 — DECISION verdict=" + verdict
                + " for numeroserie=" + b.getNumeroserie() + " — reason=" + reason);

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
        ConsoleLog.log(TAG, "VOUCHER step 1 — DB lookup by codebarre/numeroserie for code='" + code + "'.");
        VoucherVerifyProjection v;
        try {
            v = voucherRepository.findForVerification(code).orElse(null);
        } catch (RuntimeException ex) {
            ConsoleLog.error(TAG, "VOUCHER step 1 FAILED — DB lookup threw for code='" + code
                    + "'. reason=" + ex.getClass().getSimpleName() + ": " + ex.getMessage(), ex);
            throw ex;
        }

        if (v == null) {
            ConsoleLog.log(TAG, "VOUCHER verdict=NOT_FOUND for code='" + code
                    + "' — reason=no voucher row matches this codebarre or numeroserie.");
            return VerificationResult.notFound("VOUCHER", code);
        }

        int maxAccess = v.getMaxAccess() == null ? 0 : v.getMaxAccess();
        int uses = v.getAccesscounter() == null ? 0 : v.getAccesscounter();
        boolean cancelled = v.getDateannulation() != null;
        boolean active = Boolean.TRUE.equals(v.getActivation());
        boolean used = Boolean.TRUE.equals(v.getUtilisation());
        List<String> zones = accessZoneResolver.resolve(v.getModelId());
        ConsoleLog.log(TAG, "VOUCHER step 2 — found numeroserie=" + v.getNumeroserie()
                + ", codebarre=" + v.getCodebarre() + ", model=" + v.getModelId() + " (" + v.getModelName() + ")"
                + ", event='" + v.getEventTitle() + "', zones=" + zones
                + ", flags[activation=" + active + ", utilisation=" + used
                + ", vendu=" + Boolean.TRUE.equals(v.getVendu()) + ", reservation=" + Boolean.TRUE.equals(v.getReservation())
                + ", cancelled=" + cancelled + "], access uses=" + uses + "/" + maxAccess + ".");

        Verdict verdict;
        String reason;
        if (cancelled) {
            verdict = Verdict.CANCELLED;
            reason = "dateannulation is set (voucher cancelled).";
        } else if (!active) {
            verdict = Verdict.NOT_ACTIVE;
            reason = "activation flag is false (voucher not activated).";
        } else if (used || (maxAccess > 0 && uses >= maxAccess)) {
            verdict = Verdict.ALREADY_USED;
            reason = used
                    ? "utilisation flag is true (already used)."
                    : "access count " + uses + " >= maxaccess " + maxAccess + " (quota exhausted).";
        } else {
            verdict = Verdict.VALID;
            reason = "not cancelled, activated, not used, and within the access quota (" + uses + "/" + maxAccess + ").";
        }
        ConsoleLog.log(TAG, "VOUCHER step 3 — DECISION verdict=" + verdict
                + " for numeroserie=" + v.getNumeroserie() + " — reason=" + reason);

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

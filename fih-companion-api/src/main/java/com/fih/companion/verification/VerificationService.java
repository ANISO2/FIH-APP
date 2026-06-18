package com.fih.companion.verification;

import com.fih.companion.access.AccessZoneResolver;
import com.fih.companion.domain.*;
import com.fih.companion.evenement.Evenement;
import com.fih.companion.repository.*;
import com.fih.companion.evenement.EvenementRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Computes a verdict for a billet or voucher. Strictly read-only: it reads rows
 * and returns a verdict, but NEVER marks anything used. The legacy turnstile
 * system owns that write.
 */
@Service
@Transactional(readOnly = true)
public class VerificationService {

    private final BilletRepository billetRepository;
    private final VoucherRepository voucherRepository;
    private final ModeleBilletRepository modeleBilletRepository;
    private final EvenementRepository evenementRepository;
    private final HolderRepository holderRepository;
    private final BadgeAffectationRepository affectationRepository;
    private final AccessZoneResolver accessZoneResolver;

    public VerificationService(BilletRepository billetRepository,
                               VoucherRepository voucherRepository,
                               ModeleBilletRepository modeleBilletRepository,
                               EvenementRepository evenementRepository,
                               HolderRepository holderRepository,
                               BadgeAffectationRepository affectationRepository,
                               AccessZoneResolver accessZoneResolver) {
        this.billetRepository = billetRepository;
        this.voucherRepository = voucherRepository;
        this.modeleBilletRepository = modeleBilletRepository;
        this.evenementRepository = evenementRepository;
        this.holderRepository = holderRepository;
        this.affectationRepository = affectationRepository;
        this.accessZoneResolver = accessZoneResolver;
    }

    // ---------------------------------------------------------------- BILLET
    public VerificationResult verifyBillet(String code) {
        Optional<Billet> found = billetRepository.findByCodebarre(code);
        if (found.isEmpty()) {
            found = billetRepository.findByNumeroserie(code);
        }
        if (found.isEmpty()) {
            return VerificationResult.notFound("BILLET", code);
        }
        Billet b = found.get();

        ModeleBillet model = b.getModelebillet() == null ? null
                : modeleBilletRepository.findById(b.getModelebillet()).orElse(null);
        Evenement event = b.getEvenement() == null ? null
                : evenementRepository.findById(b.getEvenement()).orElse(null);
        String holderName = holderRepository.findByBillet(b.getNumeroserie())
                .map(this::fullName).orElse(null);
        String affecteeA = affecteeName(b.getNumeroserie());

        int maxAccess = model == null ? 0 : model.getMaxaccess();
        int uses = b.getNombreacces();
        List<String> zones = accessZoneResolver.resolve(b.getModelebillet());

        Verdict verdict;
        if (!b.isActivation()) {
            verdict = Verdict.NOT_ACTIVE;
        } else if (b.isUtilisation() || (maxAccess > 0 && uses >= maxAccess)) {
            verdict = Verdict.ALREADY_USED;
        } else {
            verdict = Verdict.VALID;
        }

        return new VerificationResult(
                "BILLET",
                verdict,
                b.getNumeroserie(),
                b.getCodebarre(),
                event == null ? null : event.getTitre(),
                event == null ? null : event.getDdate(),
                model == null ? null : model.getModele(),
                zones,
                maxAccess,
                uses,
                holderName,
                affecteeA,
                new VerificationResult.Flags(
                        b.isActivation(),
                        b.isUtilisation(),
                        b.isVendu(),
                        b.isReservation(),
                        false));
    }

    // --------------------------------------------------------------- VOUCHER
    public VerificationResult verifyVoucher(String code) {
        Optional<Voucher> found = voucherRepository.findByCodebarre(code);
        if (found.isEmpty()) {
            found = voucherRepository.findByNumeroserie(code);
        }
        if (found.isEmpty()) {
            return VerificationResult.notFound("VOUCHER", code);
        }
        Voucher v = found.get();

        ModeleBillet model = v.getModelebillet() == null ? null
                : modeleBilletRepository.findById(v.getModelebillet()).orElse(null);
        Evenement event = v.getEvenement() == null ? null
                : evenementRepository.findById(v.getEvenement()).orElse(null);

        int maxAccess = model == null ? 0 : model.getMaxaccess();
        int uses = v.getAccesscounter() == null ? 0 : v.getAccesscounter();
        boolean cancelled = v.getDateannulation() != null;
        boolean active = Boolean.TRUE.equals(v.getActivation());
        boolean used = Boolean.TRUE.equals(v.getUtilisation());
        List<String> zones = accessZoneResolver.resolve(v.getModelebillet());

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
                event == null ? null : event.getTitre(),
                event == null ? null : event.getDdate(),
                model == null ? null : model.getModele(),
                zones,
                maxAccess,
                uses,
                null,
                affecteeName(v.getNumeroserie()),
                new VerificationResult.Flags(
                        active,
                        used,
                        Boolean.TRUE.equals(v.getVendu()),
                        Boolean.TRUE.equals(v.getReservation()),
                        cancelled));
    }

    private String fullName(Holder h) {
        String first = h.getFirstname() == null ? "" : h.getFirstname().trim();
        String last = h.getLastname() == null ? "" : h.getLastname().trim();
        String name = (first + " " + last).trim();
        return name.isEmpty() ? null : name;
    }

    /** The assigned "Affectée à" name for a serial, or null if none set. */
    private String affecteeName(String numeroserie) {
        return affectationRepository.findById(numeroserie)
                .map(BadgeAffectation::getAffecteeA).orElse(null);
    }
}

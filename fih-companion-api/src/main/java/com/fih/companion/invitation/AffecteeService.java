package com.fih.companion.invitation;

import com.fih.companion.badge.BadgeProperties;
import com.fih.companion.domain.BadgeAffectation;
import com.fih.companion.domain.Billet;
import com.fih.companion.invitation.dto.AffecteeDto;
import com.fih.companion.invitation.dto.LotItemDto;
import com.fih.companion.invitation.dto.LotPreviewDto;
import com.fih.companion.invitation.dto.LotRequest;
import com.fih.companion.invitation.dto.LotResultDto;
import com.fih.companion.invitation.projection.LotRowProjection;
import com.fih.companion.repository.BadgeAffectationRepository;
import com.fih.companion.repository.BilletRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reads and writes the "Affectée à" name for invitation billets.
 *
 * This is the ONLY service in the app that writes. It writes rows in the
 * app-owned badge_affectation table and never touches a legacy table.
 *
 * AUTO-NUMBERED UNIQUE NAMES (Change A)
 * -------------------------------------
 * The admin only ever types a BASE name (e.g. "ANIS"). We never store the bare
 * base and we never reject a "duplicate name". Instead we always append a
 * continuing, zero-padded number so the stored value is unique:
 *   - base is new            -> ANIS-01
 *   - ANIS-01, ANIS-02 exist -> ANIS-03   (max existing number + 1, NOT a count)
 *   - a lot of N             -> ANIS-(k) … ANIS-(k+N-1), continuing the sequence
 * Base matching is case-insensitive, so "anis" and "ANIS" share one sequence.
 * A UNIQUE constraint on badge_affectation.affectee_a is the safety net; if two
 * admins assign at the very same instant and collide, we simply recompute the
 * next number and retry (see {@link #set} / {@link #assignLot}).
 *
 * ONE-TIME RULE
 * -------------
 * An invitation may be named only once. Once a row exists, the name is permanent:
 * {@link #set} rejects re-assignment with 409 Conflict, and the lot endpoints
 * refuse to touch any serial that already has a name. (Re-printing the PDF is a
 * separate action and stays allowed — see {@link #markPrinted}.)
 */
@Service
public class AffecteeService {

    /** How many times we retry a write if the affectee_a UNIQUE safety net trips. */
    private static final int NUMBERING_RETRIES = 5;

    private final BadgeAffectationRepository affectationRepository;
    private final BilletRepository billetRepository;
    private final BadgeProperties badgeProperties;

    /**
     * A reference to this same bean, injected lazily by Spring. We call the
     * transactional *Once methods THROUGH this proxy (self.setOnce(...)) so each
     * attempt runs in its own transaction. Calling them directly (this.setOnce)
     * would bypass Spring's transaction proxy and the retry would not get a fresh
     * transaction. @Lazy breaks the "a bean that needs itself" startup cycle.
     */
    @Autowired
    @Lazy
    private AffecteeService self;

    public AffecteeService(BadgeAffectationRepository affectationRepository,
                           BilletRepository billetRepository,
                           BadgeProperties badgeProperties) {
        this.affectationRepository = affectationRepository;
        this.billetRepository = billetRepository;
        this.badgeProperties = badgeProperties;
    }

    /** Current name for a serial, if one has been set. Read-only. */
    @Transactional(readOnly = true)
    public Optional<AffecteeDto> get(String numeroserie) {
        return affectationRepository.findById(numeroserie).map(this::toDto);
    }

    // -------------------------------------------------------------- single (A)

    /**
     * Assign the name for an invitation serial ONCE. The admin gives a BASE name;
     * we store BASE-NN (Change A). Retries on the rare UNIQUE collision.
     */
    public AffecteeDto set(String numeroserie, String name, String updatedBy) {
        for (int attempt = 0; attempt < NUMBERING_RETRIES; attempt++) {
            try {
                return self.setOnce(numeroserie, name, updatedBy);
            } catch (DataIntegrityViolationException collision) {
                // Another write grabbed our number first — recompute and retry.
            }
        }
        throw new ResponseStatusException(HttpStatus.CONFLICT,
                "Conflit de numérotation, veuillez réessayer.");
    }

    /** One transactional attempt of {@link #set}. Public so the proxy can wrap it. */
    @Transactional
    public AffecteeDto setOnce(String numeroserie, String name, String updatedBy) {
        Billet billet = billetRepository.findByNumeroserie(numeroserie)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Aucun billet trouvé pour le numéro de série : " + numeroserie));

        if (!badgeProperties.isInvitationModel(billet.getModelebillet())) {
            throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "Le numéro de série ne correspond pas à un billet d'invitation.");
        }

        String base = name == null ? "" : name.trim();
        if (base.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le nom est obligatoire.");
        }

        // One-time guard: a row already here means the invitation was delivered.
        if (affectationRepository.existsById(numeroserie)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Cette invitation est déjà affectée et ne peut plus être modifiée.");
        }

        // Change A: never store the bare base — always BASE-NN, continuing the
        // sequence from the highest number already used for this base.
        int next = nextNumberForBase(base);
        String unique = formatName(base, next, widthFor(next));

        BadgeAffectation entity = new BadgeAffectation(numeroserie, unique, updatedBy);
        return toDto(affectationRepository.save(entity));
    }

    // ------------------------------------------------------------------ lot (A/C)

    /** Read-only dry-run of a lot: matched rows, proposed names, conflicts, warnings. */
    @Transactional(readOnly = true)
    public LotPreviewDto previewLot(LotRequest req) {
        Lot lot = buildLot(req);

        List<LotItemDto> items = new ArrayList<>(lot.eligible.size());
        List<String> assignedSerials = new ArrayList<>();
        int assignedCount = 0;
        for (Assignment a : lot.eligible) {
            boolean assigned = a.existingName != null;
            if (assigned) {
                assignedCount++;
                assignedSerials.add(a.row.getNumeroserie());
            }
            items.add(new LotItemDto(
                    a.row.getNumeroserie(), a.row.getCodebarre(),
                    a.row.getEventId(), a.row.getEventTitle(),
                    a.row.getModelId(), a.row.getModelName(),
                    a.proposedName, assigned, a.existingName));
        }

        boolean baseUsed = affectationRepository.baseNameUsed(lot.baseName);
        boolean canAssign = !lot.eligible.isEmpty() && assignedCount == 0;

        return new LotPreviewDto(
                lot.eligible.size(), assignedCount, lot.nonInvitationCount,
                baseUsed, canAssign, items, assignedSerials);
    }

    /**
     * Assign a whole lot immutably. Blocks (409) if ANY serial in range is already
     * named. Names continue the base sequence (Change A); retries on collision.
     */
    public LotResultDto assignLot(LotRequest req, String updatedBy) {
        for (int attempt = 0; attempt < NUMBERING_RETRIES; attempt++) {
            try {
                return self.assignLotOnce(req, updatedBy);
            } catch (DataIntegrityViolationException collision) {
                // A name we picked was taken in parallel — recompute and retry.
            }
        }
        throw new ResponseStatusException(HttpStatus.CONFLICT,
                "Conflit de numérotation sur le lot, veuillez réessayer.");
    }

    /** One transactional attempt of {@link #assignLot}. Public so the proxy can wrap it. */
    @Transactional
    public LotResultDto assignLotOnce(LotRequest req, String updatedBy) {
        Lot lot = buildLot(req);

        if (lot.eligible.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Aucune invitation éligible dans cette plage de numéros de série.");
        }

        List<String> already = lot.eligible.stream()
                .filter(a -> a.existingName != null)
                .map(a -> a.row.getNumeroserie())
                .toList();
        if (!already.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Lot bloqué : " + already.size() + " invitation(s) déjà affectée(s) dans cette plage ("
                            + String.join(", ", already) + "). Ajustez la plage.");
        }

        List<BadgeAffectation> toSave = new ArrayList<>(lot.eligible.size());
        for (Assignment a : lot.eligible) {
            toSave.add(new BadgeAffectation(a.row.getNumeroserie(), a.proposedName, updatedBy));
        }
        List<AffecteeDto> assigned = affectationRepository.saveAll(toSave).stream().map(this::toDto).toList();
        return new LotResultDto(assigned.size(), assigned);
    }

    /** CSV manifest of the assigned names in a range: nom,numeroserie,codebarre,evenement. */
    @Transactional(readOnly = true)
    public String manifestCsv(String startSerie, String endSerie) {
        String start = trimOr400(startSerie, "Le numéro de série de début est obligatoire.");
        String end = trimOr400(endSerie, "Le numéro de série de fin est obligatoire.");
        StringBuilder sb = new StringBuilder("nom,numeroserie,codebarre,evenement\n");
        for (LotRowProjection r : billetRepository.findRange(start, end)) {
            if (!badgeProperties.isInvitationModel(r.getModelId())) continue;
            if (r.getAffecteeA() == null) continue;
            sb.append(csv(r.getAffecteeA())).append(',')
                    .append(csv(r.getNumeroserie())).append(',')
                    .append(csv(r.getCodebarre())).append(',')
                    .append(csv(r.getEventTitle())).append('\n');
        }
        return sb.toString();
    }

    /** §6 — stamp printed_at on every serial that has a name (others are skipped). */
    @Transactional
    public void markPrinted(Collection<String> serials) {
        if (serials == null || serials.isEmpty()) return;
        affectationRepository.markPrinted(serials, LocalDateTime.now());
    }

    // ----------------------------------------------------------------- helpers

    /** Shared lot computation used by both preview and assign. */
    private Lot buildLot(LotRequest req) {
        if (req == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Requête de lot manquante.");
        }
        String start = trimOr400(req.startSerie(), "Le numéro de série de début est obligatoire.");
        String end = trimOr400(req.endSerie(), "Le numéro de série de fin est obligatoire.");
        String baseName = trimOr400(req.baseName(), "Le nom de base est obligatoire.");
        if (start.compareTo(end) > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Le numéro de série de début doit être inférieur ou égal à celui de fin.");
        }

        List<LotRowProjection> rows = billetRepository.findRange(start, end);
        List<LotRowProjection> eligibleRows = new ArrayList<>();
        int nonInvitation = 0;
        for (LotRowProjection r : rows) {
            if (badgeProperties.isInvitationModel(r.getModelId())) eligibleRows.add(r);
            else nonInvitation++;
        }

        // Change A: continue the base sequence. If ANIS-01, ANIS-02 already exist
        // and the lot has 50 rows, names run ANIS-03 … ANIS-52 (start = max + 1).
        // Padding is wide enough for the largest number in the run (min 2 digits).
        int total = eligibleRows.size();
        int startNum = nextNumberForBase(baseName);
        int lastNum = startNum + Math.max(total, 1) - 1;
        int width = widthFor(lastNum);

        List<Assignment> eligible = new ArrayList<>(total);
        int i = startNum;
        for (LotRowProjection r : eligibleRows) {
            String proposed = formatName(baseName, i++, width);
            eligible.add(new Assignment(r, proposed, r.getAffecteeA()));
        }

        Lot lot = new Lot();
        lot.baseName = baseName;
        lot.eligible = eligible;
        lot.nonInvitationCount = nonInvitation;
        return lot;
    }

    /**
     * Change A — the next free number for a base name. We read every stored name
     * that belongs to this base, keep only the ones shaped exactly "base-<digits>"
     * (case-insensitive), and return (highest digits) + 1. A brand-new base gives
     * 1. We use the MAX, not the count, so deletions/gaps never reuse a number.
     */
    private int nextNumberForBase(String base) {
        Pattern shape = Pattern.compile("^" + Pattern.quote(base) + "-(\\d+)$", Pattern.CASE_INSENSITIVE);
        int max = 0;
        for (String stored : affectationRepository.findNamesForBase(base)) {
            if (stored == null) continue;
            Matcher m = shape.matcher(stored.trim());
            if (m.matches()) {
                try {
                    max = Math.max(max, Integer.parseInt(m.group(1)));
                } catch (NumberFormatException ignore) {
                    // absurdly long number — ignore it rather than fail the assign
                }
            }
        }
        return max + 1;
    }

    /** Zero-padded "BASE-07" style name. */
    private String formatName(String base, int number, int width) {
        return String.format("%s-%0" + width + "d", base, number);
    }

    /** Padding width: at least 2 digits, more if the number itself is longer. */
    private int widthFor(int number) {
        return Math.max(2, Integer.toString(Math.max(number, 1)).length());
    }

    private String trimOr400(String s, String message) {
        String t = s == null ? "" : s.trim();
        if (t.isEmpty()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
        return t;
    }

    /** Minimal CSV escaping (wrap in quotes when the value has a comma/quote/newline). */
    private String csv(String v) {
        if (v == null) return "";
        if (v.contains(",") || v.contains("\"") || v.contains("\n")) {
            return "\"" + v.replace("\"", "\"\"") + "\"";
        }
        return v;
    }

    private AffecteeDto toDto(BadgeAffectation e) {
        return new AffecteeDto(e.getNumeroserie(), e.getAffecteeA(), e.getUpdatedAt(),
                e.getUpdatedBy(), e.getPrintedAt());
    }

    /** Small carriers so preview and assign share one computation. */
    private static final class Lot {
        String baseName;
        List<Assignment> eligible;
        int nonInvitationCount;
    }

    private static final class Assignment {
        final LotRowProjection row;
        final String proposedName;
        final String existingName;
        Assignment(LotRowProjection row, String proposedName, String existingName) {
            this.row = row;
            this.proposedName = proposedName;
            this.existingName = existingName;
        }
    }
}

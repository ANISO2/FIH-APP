package com.fih.companion.invitation;

import com.fih.companion.badge.ModelClassificationService;
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


@Service
public class AffecteeService {

    private static final int NUMBERING_RETRIES = 5;

    private final BadgeAffectationRepository affectationRepository;
    private final BilletRepository billetRepository;
    private final ModelClassificationService classification;


    @Autowired
    @Lazy
    private AffecteeService self;

    public AffecteeService(BadgeAffectationRepository affectationRepository,
                           BilletRepository billetRepository,
                           ModelClassificationService classification) {
        this.affectationRepository = affectationRepository;
        this.billetRepository = billetRepository;
        this.classification = classification;
    }

    @Transactional(readOnly = true)
    public Optional<AffecteeDto> get(String numeroserie) {
        return affectationRepository.findById(numeroserie).map(this::toDto);
    }

    // -------------------------------------------------------------- single (A)


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

    @Transactional
    public AffecteeDto setOnce(String numeroserie, String name, String updatedBy) {
        Billet billet = billetRepository.findByNumeroserie(numeroserie)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Aucun billet trouvé pour le numéro de série : " + numeroserie));

        if (!classification.isAffectable(billet.getModelebillet())) {
            throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "Ce numéro de série correspond à un billet payant et ne peut pas être affecté ici.");
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

        // sequence from the highest number already used for this base.
        int next = nextNumberForBase(base);
        String unique = formatName(base, next, widthFor(next));

        BadgeAffectation entity = new BadgeAffectation(numeroserie, unique, updatedBy);
        return toDto(affectationRepository.save(entity));
    }


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

    @Transactional(readOnly = true)
    public String manifestCsv(String startSerie, String endSerie) {
        String start = trimOr400(startSerie, "Le numéro de série de début est obligatoire.");
        String end = trimOr400(endSerie, "Le numéro de série de fin est obligatoire.");
        StringBuilder sb = new StringBuilder("nom,numeroserie,codebarre,evenement\n");
        for (LotRowProjection r : billetRepository.findRange(start, end)) {
            if (!classification.isAffectable(r.getModelId())) continue;
            if (r.getAffecteeA() == null) continue;
            sb.append(csv(r.getAffecteeA())).append(',')
                    .append(csv(r.getNumeroserie())).append(',')
                    .append(csv(r.getCodebarre())).append(',')
                    .append(csv(r.getEventTitle())).append('\n');
        }
        return sb.toString();
    }

    @Transactional
    public void markPrinted(Collection<String> serials) {
        if (serials == null || serials.isEmpty()) return;
        affectationRepository.markPrinted(serials, LocalDateTime.now());
    }


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
            if (classification.isAffectable(r.getModelId())) eligibleRows.add(r);
            else nonInvitation++;
        }


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
                }
            }
        }
        return max + 1;
    }

    private String formatName(String base, int number, int width) {
        return String.format("%s-%0" + width + "d", base, number);
    }

    private int widthFor(int number) {
        return Math.max(2, Integer.toString(Math.max(number, 1)).length());
    }

    private String trimOr400(String s, String message) {
        String t = s == null ? "" : s.trim();
        if (t.isEmpty()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
        return t;
    }

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

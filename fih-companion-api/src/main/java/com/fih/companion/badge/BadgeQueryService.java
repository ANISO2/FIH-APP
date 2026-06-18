package com.fih.companion.badge;

import com.fih.companion.access.AccessZoneResolver;
import com.fih.companion.badge.dto.*;
import com.fih.companion.badge.projection.AvailabilityProjection;
import com.fih.companion.badge.projection.BadgeItemProjection;
import com.fih.companion.badge.projection.CodeRowProjection;
import com.fih.companion.domain.BadgeAffectation;
import com.fih.companion.domain.Billet;
import com.fih.companion.domain.ModeleBillet;
import com.fih.companion.domain.Voucher;
import com.fih.companion.evenement.Evenement;
import com.fih.companion.evenement.EvenementRepository;
import com.fih.companion.repository.BadgeAffectationRepository;
import com.fih.companion.repository.BilletRepository;
import com.fih.companion.repository.HolderRepository;
import com.fih.companion.repository.ModeleBilletRepository;
import com.fih.companion.repository.VoucherRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.*;

/**
 * Read-only data side of the badge feature: counts, item lists, photo coverage.
 *
 * INVITATION-ONLY (3.1)
 * ---------------------
 * PDF/badge generation is restricted to invitation-type models. The set of
 * allowed modelebillet.reference values lives in application.yml
 * (fih.badge.invitation-models) via {@link BadgeProperties}. Here we:
 *   - filter the availability list down to invitation models only, and
 *   - guard every per-model entry point (items, photoCheck, batch) and the
 *     single-code entry point so a non-invitation model is rejected with 404
 *     even if a client calls the API directly.
 * Nothing else about the PDF pipeline changes.
 */
@Service
@Transactional(readOnly = true)
public class BadgeQueryService {

    private final BadgeRepository badgeRepo;
    private final BilletRepository billetRepo;
    private final VoucherRepository voucherRepo;
    private final ModeleBilletRepository modeleRepo;
    private final EvenementRepository eventRepo;
    private final HolderRepository holderRepo;
    private final BadgeAffectationRepository affectationRepo;
    private final AccessZoneResolver zones;
    private final PhotoResolver photos;
    private final BadgeProperties props;

    public BadgeQueryService(BadgeRepository badgeRepo, BilletRepository billetRepo, VoucherRepository voucherRepo,
                             ModeleBilletRepository modeleRepo, EvenementRepository eventRepo,
                             HolderRepository holderRepo, BadgeAffectationRepository affectationRepo,
                             AccessZoneResolver zones, PhotoResolver photos,
                             BadgeProperties props) {
        this.badgeRepo = badgeRepo;
        this.billetRepo = billetRepo;
        this.voucherRepo = voucherRepo;
        this.modeleRepo = modeleRepo;
        this.eventRepo = eventRepo;
        this.holderRepo = holderRepo;
        this.affectationRepo = affectationRepo;
        this.zones = zones;
        this.photos = photos;
        this.props = props;
    }

    // -------------------------------------------------------------- availability
    public List<AvailabilityDto> availability(Integer eventId, boolean withPhotoCheck) {
        List<AvailabilityProjection> rows = badgeRepo.availability(eventId).stream()
                // 3.1 — only expose invitation models in the availability list.
                .filter(r -> props.isInvitationModel(r.getModelId()))
                .toList();

        // Optional, slower: count photos present per (event, model).
        Map<String, int[]> photoCounts = withPhotoCheck ? photoCountsByGroup(eventId) : Map.of();

        List<AvailabilityDto> out = new ArrayList<>(rows.size());
        for (AvailabilityProjection r : rows) {
            Integer withPhoto = null, missing = null;
            if (withPhotoCheck) {
                int[] wc = photoCounts.getOrDefault(r.getEventId() + ":" + r.getModelId(), new int[]{0, 0});
                withPhoto = wc[0];
                missing = (int) r.getInjectedCount() - wc[0];
            }
            out.add(new AvailabilityDto(
                    r.getEventId(), r.getEventTitle(), r.getEventDate().toLocalDate(),
                    r.getModelId(), r.getModelName(), zones.resolve(r.getModelId()),
                    (int) r.getInjectedCount(), (int) r.getBilletCount(), (int) r.getVoucherCount(),
                    withPhoto, missing));
        }
        return out;
    }

    private Map<String, int[]> photoCountsByGroup(Integer eventId) {
        Map<String, int[]> map = new HashMap<>();
        for (CodeRowProjection c : badgeRepo.codeRows(eventId)) {
            // Only count photos for invitation models (matches the filtered list).
            if (!props.isInvitationModel(c.getModelId())) {
                continue;
            }
            String key = c.getEventId() + ":" + c.getModelId();
            int[] wc = map.computeIfAbsent(key, k -> new int[]{0, 0});
            if (photos.exists(c.getCodebarre(), c.getNumeroserie())) wc[0]++;
            else wc[1]++;
        }
        return map;
    }

    // -------------------------------------------------------------- items page
    public PageDto<BadgeItemDto> items(int eventId, int modelId, int page, int size, String search) {
        requireInvitation(modelId);
        String s = (search == null || search.isBlank()) ? null : search.trim();
        long total = badgeRepo.itemsCount(eventId, modelId, s);
        List<BadgeItemDto> content = badgeRepo.items(eventId, modelId, s, size, page * size).stream()
                .map(this::toItemDto)
                .toList();
        int totalPages = size == 0 ? 0 : (int) Math.ceil((double) total / size);
        return new PageDto<>(content, page, size, total, totalPages);
    }

    private BadgeItemDto toItemDto(BadgeItemProjection p) {
        return new BadgeItemDto(p.getType(), p.getNumeroserie(), p.getCodebarre(),
                p.getHolderName(), p.getAffecteeA(), photos.exists(p.getCodebarre(), p.getNumeroserie()));
    }

    // -------------------------------------------------------------- photo check
    public PhotoCheckDto photoCheck(int eventId, int modelId) {
        requireInvitation(modelId);
        List<BadgeItemProjection> all = badgeRepo.allItems(eventId, modelId);
        List<String> missing = new ArrayList<>();
        int withPhoto = 0;
        for (BadgeItemProjection p : all) {
            if (photos.exists(p.getCodebarre(), p.getNumeroserie())) withPhoto++;
            else missing.add(p.getCodebarre());
        }
        return new PhotoCheckDto(all.size(), withPhoto, missing.size(), missing);
    }

    // -------------------------------------------------------------- build records for PDF
    public BadgeRecord single(String type, String code) {
        if ("voucher".equalsIgnoreCase(type)) {
            Voucher v = voucherRepo.findByCodebarre(code).or(() -> voucherRepo.findByNumeroserie(code))
                    .orElseThrow(() -> notFound(code));
            requireInvitation(v.getModelebillet());
            return record("VOUCHER", v.getNumeroserie(), v.getCodebarre(), null, v.getEvenement(), v.getModelebillet());
        }
        Billet b = billetRepo.findByCodebarre(code).or(() -> billetRepo.findByNumeroserie(code))
                .orElseThrow(() -> notFound(code));
        requireInvitation(b.getModelebillet());
        String holder = holderRepo.findByBillet(b.getNumeroserie()).map(this::name).orElse(null);
        return record("BILLET", b.getNumeroserie(), b.getCodebarre(), holder, b.getEvenement(), b.getModelebillet());
    }

    public List<BadgeRecord> batch(int eventId, int modelId, List<String> codes) {
        requireInvitation(modelId);
        Evenement e = eventRepo.findById(eventId).orElseThrow(() -> notFound("event " + eventId));
        ModeleBillet m = modeleRepo.findById(modelId).orElse(null);
        List<String> zoneList = zones.resolve(modelId);
        Set<String> wanted = (codes == null || codes.isEmpty()) ? null : new HashSet<>(codes);

        List<BadgeRecord> out = new ArrayList<>();
        for (BadgeItemProjection p : badgeRepo.allItems(eventId, modelId)) {
            if (wanted != null && !wanted.contains(p.getCodebarre()) && !wanted.contains(p.getNumeroserie())) {
                continue;
            }
            out.add(new BadgeRecord(p.getType(), p.getNumeroserie(), p.getCodebarre(), p.getHolderName(),
                    p.getAffecteeA(),
                    e.getTitre(), e.getDdate(), m == null ? null : m.getModele(), zoneList,
                    photos.resolve(p.getCodebarre(), p.getNumeroserie()).orElse(null)));
        }
        if (out.isEmpty()) throw notFound("no records for event " + eventId + " / model " + modelId);
        return out;
    }

    private BadgeRecord record(String type, String numeroserie, String codebarre, String holder,
                               Integer eventId, Integer modelId) {
        Evenement e = eventId == null ? null : eventRepo.findById(eventId).orElse(null);
        ModeleBillet m = modelId == null ? null : modeleRepo.findById(modelId).orElse(null);
        Path photo = photos.resolve(codebarre, numeroserie).orElse(null);
        LocalDate date = e == null ? null : e.getDdate();
        String affectee = affecteeName(numeroserie);
        return new BadgeRecord(type, numeroserie, codebarre, holder, affectee,
                e == null ? null : e.getTitre(), date,
                m == null ? null : m.getModele(), zones.resolve(modelId), photo);
    }

    /** The assigned "Affectée à" name for a serial, or null if none set. */
    private String affecteeName(String numeroserie) {
        return affectationRepo.findById(numeroserie).map(BadgeAffectation::getAffecteeA).orElse(null);
    }

    private String name(com.fih.companion.domain.Holder h) {
        String full = ((h.getFirstname() == null ? "" : h.getFirstname()) + " "
                + (h.getLastname() == null ? "" : h.getLastname())).trim();
        return full.isEmpty() ? null : full;
    }

    /** 3.1 guard: reject any model that is not a configured invitation model. */
    private void requireInvitation(Integer modelId) {
        if (!props.isInvitationModel(modelId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Badge generation is restricted to invitation models");
        }
    }

    private ResponseStatusException notFound(String what) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Not found: " + what);
    }
}

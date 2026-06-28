package com.fih.companion.badge;

import com.fih.companion.access.AccessZoneResolver;
import com.fih.companion.badge.dto.*;
import com.fih.companion.badge.projection.AvailabilityProjection;
import com.fih.companion.badge.projection.BadgeItemProjection;
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

import java.time.LocalDate;
import java.util.*;


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
    private final PosterResolver posters;
    private final BadgeProperties props;

    public BadgeQueryService(BadgeRepository badgeRepo, BilletRepository billetRepo, VoucherRepository voucherRepo,
                             ModeleBilletRepository modeleRepo, EvenementRepository eventRepo,
                             HolderRepository holderRepo, BadgeAffectationRepository affectationRepo,
                             AccessZoneResolver zones, PosterResolver posters,
                             BadgeProperties props) {
        this.badgeRepo = badgeRepo;
        this.billetRepo = billetRepo;
        this.voucherRepo = voucherRepo;
        this.modeleRepo = modeleRepo;
        this.eventRepo = eventRepo;
        this.holderRepo = holderRepo;
        this.affectationRepo = affectationRepo;
        this.zones = zones;
        this.posters = posters;
        this.props = props;
    }

    // -------------------------------------------------------------- availability
    public List<AvailabilityDto> availability(Integer eventId) {
        List<AvailabilityProjection> rows = badgeRepo.availability(eventId).stream()
                .filter(r -> props.isInvitationModel(r.getModelId()))
                .toList();

         Map<String, Boolean> posterCache = new HashMap<>();

        List<AvailabilityDto> out = new ArrayList<>(rows.size());
        for (AvailabilityProjection r : rows) {
            boolean hasPoster = posterCache.computeIfAbsent(
                    r.getEventTitle() == null ? "" : r.getEventTitle(), posters::exists);
            out.add(new AvailabilityDto(
                    r.getEventId(), r.getEventTitle(), r.getEventDate().toLocalDate(),
                    r.getModelId(), r.getModelName(), zones.resolve(r.getModelId()),
                    (int) r.getInjectedCount(), (int) r.getBilletCount(), (int) r.getVoucherCount(),
                    hasPoster));
        }
        return out;
    }

    // ------------------------------------------------------------- missing posters
     public List<MissingPosterDto> missingPosters() {
        Map<Integer, MissingPosterDto> byEvent = new LinkedHashMap<>();
        for (AvailabilityProjection r : badgeRepo.availability(null)) {
            if (!props.isInvitationModel(r.getModelId())) continue;
            if (posters.exists(r.getEventTitle())) continue;
            int ev = r.getEventId();
            int add = (int) r.getInjectedCount();
            MissingPosterDto cur = byEvent.get(ev);
            if (cur == null) {
                byEvent.put(ev, new MissingPosterDto(ev, r.getEventTitle(), r.getEventDate().toLocalDate(), add));
            } else {
                byEvent.put(ev, new MissingPosterDto(ev, cur.eventTitle(), cur.eventDate(), cur.invitationCount() + add));
            }
        }
        return new ArrayList<>(byEvent.values());
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
        java.time.LocalDateTime printedAt = p.getPrintedAt() == null ? null : p.getPrintedAt().toLocalDateTime();
        return new BadgeItemDto(p.getType(), p.getNumeroserie(), p.getCodebarre(),
                p.getHolderName(), p.getAffecteeA(), printedAt);
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
                    e.getTitre(), e.getDdate(), m == null ? null : m.getModele(), zoneList, null));
        }
        if (out.isEmpty()) throw notFound("no records for event " + eventId + " / model " + modelId);
        return out;
    }

    private BadgeRecord record(String type, String numeroserie, String codebarre, String holder,
                               Integer eventId, Integer modelId) {
        Evenement e = eventId == null ? null : eventRepo.findById(eventId).orElse(null);
        ModeleBillet m = modelId == null ? null : modeleRepo.findById(modelId).orElse(null);
        LocalDate date = e == null ? null : e.getDdate();
        String affectee = affecteeName(numeroserie);
        return new BadgeRecord(type, numeroserie, codebarre, holder, affectee,
                e == null ? null : e.getTitre(), date,
                m == null ? null : m.getModele(), zones.resolve(modelId), null);
    }

     private String affecteeName(String numeroserie) {
        return affectationRepo.findById(numeroserie).map(BadgeAffectation::getAffecteeA).orElse(null);
    }

    private String name(com.fih.companion.domain.Holder h) {
        String full = ((h.getFirstname() == null ? "" : h.getFirstname()) + " "
                + (h.getLastname() == null ? "" : h.getLastname())).trim();
        return full.isEmpty() ? null : full;
    }

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

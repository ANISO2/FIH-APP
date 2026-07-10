package com.fih.companion.stats;

import com.fih.companion.stats.dto.*;
import com.fih.companion.stats.projection.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.sql.Date;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;


@Service
@Transactional(readOnly = true)
public class StatsService {

    private final StatsRepository repo;

    private final long statsCacheTtlSeconds;
    private final ConcurrentHashMap<String, CacheEntry> statsCache = new ConcurrentHashMap<>();

    private record CacheEntry(long expiresAtMillis, Object value) {}

    public StatsService(StatsRepository repo,
                        @Value("${fih.stats.cache-ttl-seconds:${fih.recette.cache-ttl-seconds:30}}") long statsCacheTtlSeconds) {
        this.repo = repo;
        this.statsCacheTtlSeconds = statsCacheTtlSeconds;
    }


    @SuppressWarnings("unchecked")
    private <T> T cached(String key, boolean refresh, Supplier<T> loader) {
        if (statsCacheTtlSeconds <= 0) {
            return loader.get();
        }
        long now = System.currentTimeMillis();
        if (!refresh) {
            CacheEntry e = statsCache.get(key);
            if (e != null && e.expiresAtMillis() > now) {
                return (T) e.value();
            }
        }
        T value = loader.get();
        statsCache.put(key, new CacheEntry(now + statsCacheTtlSeconds * 1000L, value));
        return value;
    }

    public OverviewDto overview() {
        OverviewCountsProjection c = repo.overviewCounts();
        BusiestEventProjection busiest = repo.busiestEvent();
        return new OverviewDto(
                c.getTotalEvents(),
                c.getTotalBillets(),
                c.getTotalVouchers(),
                c.getTotalScans(),
                c.getAcceptedScans(),
                c.getRejectedScans(),
                rate(c.getAcceptedScans(), c.getTotalScans()),
                c.getPublicScans(),
                c.getVipScans(),
                busiest == null ? null : busiest.getTitle(),
                toLocalDate(busiest == null ? null : busiest.getDate()),
                busiest == null ? 0 : busiest.getScans());
    }

    public List<EntryByDayDto> entriesByDay() {
        return repo.entriesByDay().stream()
                .map(p -> new EntryByDayDto(
                        toLocalDate(p.getDate()), p.getScans(), p.getAccepted(), p.getRejected()))
                .toList();
    }

    public GateDto gate() {
        return toGateDto(repo.gateBreakdown());
    }

    public TicketTypesDto ticketTypes() {
        TicketTypesProjection t = repo.ticketTypes();
        return new TicketTypesDto(
                new TicketBucketDto(t.getBilletIssued(), t.getBilletScanned()),
                new TicketBucketDto(t.getVoucherIssued(), t.getVoucherScanned()));
    }

    public List<EventRollupDto> events() {
        return repo.eventRollups().stream().map(this::toRollupDto).toList();
    }

    public EventDetailDto eventDetail(int id) {
        EventRollupProjection e = repo.eventRollup(id);
        if (e == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Événement introuvable.");
        }
        GateDto gate = toGateDto(repo.gateForEvent(id));
        List<HourEntryDto> hours = repo.entriesByHour(id).stream()
                .map(h -> new HourEntryDto(h.getHour(), h.getScans()))
                .toList();
        return new EventDetailDto(
                e.getEventId(), e.getTitle(), toLocalDate(e.getDate()),
                e.getScans(), e.getAccepted(), e.getRejected(),
                rate(e.getAccepted(), e.getScans()),
                gate, hours);
    }

    // ----------------------------------------------------------- Recette
    public List<RecetteSummaryDto> recetteSummary(boolean refresh) {
        return cached("summary", refresh, () ->
                repo.recetteSummary().stream()
                        .map(p -> new RecetteSummaryDto(
                                p.getEventId(), p.getEventTitle(), toLocalDate(p.getEventDate()),
                                p.getBillet(), p.getVoucher(), p.getTotal()))
                        .toList());
    }


    public List<RecetteEventHeaderDto> recetteDetailHeaders(boolean refresh) {
        return cached("detailHeaders", refresh, () ->
                repo.recetteDetailHeaders().stream()
                        .map(p -> new RecetteEventHeaderDto(
                                p.getEventId(), p.getEventTitle(), toLocalDate(p.getEventDate()),
                                p.getTotalGenere(), p.getTotalVendu(), p.getTotalReste(),
                                p.getRecetteTotale(), rate(p.getTotalVendu(), p.getTotalGenere())))
                        .toList());
    }


    public List<RecetteModelRowDto> recetteDetailRows(int eventId) {
        return repo.recetteDetailRows(eventId).stream()
                .map(p -> new RecetteModelRowDto(
                        p.getModelId(), p.getModelName(), p.getMontant(),
                        p.getBilletGeneration(), p.getBilletVente(), p.getBilletReste(),
                        p.getVoucherGeneration(), p.getVoucherVente(), p.getVoucherReste(),
                        p.getTotalVendu(), p.getRecetteTnd(),
                        rate(p.getTotalVendu(), p.getBilletGeneration() + p.getVoucherGeneration())))
                .toList();
    }

    // ------------------------------------------------- Recette par guichet
    public List<RecetteGuichetSummaryDto> recetteGuichetSummary() {
        return repo.recetteGuichetSummary().stream()
                .map(p -> new RecetteGuichetSummaryDto(
                        p.getEventId(), p.getEventTitle(), toLocalDate(p.getEventDate()),
                        p.getBillet(), p.getKit(), p.getTotal()))
                .toList();
    }

    public List<RecetteGuichetDetailDto> recetteGuichetDetail() {
        return repo.recetteGuichetDetail().stream()
                .map(p -> new RecetteGuichetDetailDto(
                        p.getEventId(), p.getEventTitle(), toLocalDate(p.getEventDate()),
                        p.getModelId(), p.getModelName(),
                        p.getBilletLivraison(), p.getBilletVente(), p.getBilletPrixUnitaire(),
                        p.getBilletRecette(), p.getBilletReste(), p.getKit()))
                .toList();
    }

    // --------------------------------------------- Statistique des tourniquets

    public List<TourniquetEventDto> tourniquets(boolean refresh) {
        return cached("tourniquets", refresh, () -> loadTourniquets());
    }

    private List<TourniquetEventDto> loadTourniquets() {
        List<TourniquetEventDto> out = new java.util.ArrayList<>();
        java.util.Map<Integer, Integer> indexByEvent = new java.util.HashMap<>();
        // mutable accumulators per event, indexed in parallel with `out`
        List<List<TourniquetRowDto>> rowsByIndex = new java.util.ArrayList<>();
        List<long[]> totalsByIndex = new java.util.ArrayList<>(); // [audience, billetTx, voucherTx]
        List<Object[]> headByIndex = new java.util.ArrayList<>();  // [eventId, title, date]

        for (TourniquetProjection p : repo.tourniquets()) {
            long audienceRow = p.getBilletCodes() + p.getVoucherCodes();
            TourniquetRowDto row = new TourniquetRowDto(
                    p.getModelId(), p.getModelName(),
                    p.getBilletCodes(), p.getVoucherCodes(), audienceRow,
                    p.getBilletTx(), p.getVoucherTx());

            Integer idx = indexByEvent.get(p.getEventId());
            if (idx == null) {
                idx = out.size();
                indexByEvent.put(p.getEventId(), idx);
                out.add(null); // placeholder, filled at the end
                rowsByIndex.add(new java.util.ArrayList<>());
                totalsByIndex.add(new long[]{0, 0, 0});
                headByIndex.add(new Object[]{p.getEventId(), p.getEventTitle(), toLocalDate(p.getEventDate())});
            }
            rowsByIndex.get(idx).add(row);
            long[] tot = totalsByIndex.get(idx);
            tot[0] += audienceRow;
            tot[1] += p.getBilletTx();
            tot[2] += p.getVoucherTx();
        }

        for (int i = 0; i < out.size(); i++) {
            Object[] head = headByIndex.get(i);
            long[] tot = totalsByIndex.get(i);
            out.set(i, new TourniquetEventDto(
                    (Integer) head[0], (String) head[1], (LocalDate) head[2],
                    tot[0], tot[1], tot[2], tot[1] + tot[2],
                    rowsByIndex.get(i)));
        }
        return out;
    }

    // --------------------------------------------------- Analyse des rejets
    public RejetsDto rejets(boolean refresh) {
        return cached("rejets", refresh, () -> loadRejets());
    }

    private RejetsDto loadRejets() {
        var kpi = repo.rejetsKpi();
        long rejets = kpi == null ? 0 : kpi.getRejets();
        long total = kpi == null ? 0 : kpi.getTotal();
        long acceptes = total - rejets;
        double taux = total > 0 ? (rejets * 100.0) / total : 0.0;

        var scans = repo.rejetsScans().stream()
                .map(s -> new RejetsDto.Scan(
                        s.getCodebarre(), s.getEventTitle(), s.getPorte(),
                        s.getDateTime() == null ? null : s.getDateTime().toLocalDateTime(),
                        s.getDescription()))
                .toList();

        return new RejetsDto(
                rejets, acceptes, total, Math.round(taux * 10.0) / 10.0,
                repo.rejetsParCategorie().stream()
                        .map(g -> new RejetsDto.Groupe(g.getLabel(), g.getValeur())).toList(),
                repo.rejetsParEvenement().stream()
                        .map(e -> new RejetsDto.Evenement(e.getEventId(), e.getEventTitle(),
                                toLocalDate(e.getEventDate()), e.getRejets())).toList(),
                repo.rejetsParPorte().stream()
                        .map(g -> new RejetsDto.Groupe(g.getLabel(), g.getValeur())).toList(),
                repo.rejetsParModele().stream()
                        .map(m -> new RejetsDto.Modele(m.getModelId(), m.getModelName(), m.getRejets())).toList(),
                repo.rejetsParJour().stream()
                        .map(j -> new RejetsDto.Jour(toLocalDate(j.getJour()), j.getRejets())).toList(),
                scans,
                scans.size() >= 2000);
    }
    private EventRollupDto toRollupDto(EventRollupProjection p) {
        return new EventRollupDto(
                p.getEventId(), p.getTitle(), toLocalDate(p.getDate()),
                p.getScans(), p.getAccepted(), p.getRejected(),
                rate(p.getAccepted(), p.getScans()),
                p.getPublicScans(), p.getVipScans());
    }

    private GateDto toGateDto(List<GateProjection> rows) {
        GateBucketDto pub = new GateBucketDto(0, 0, 0);
        GateBucketDto vip = new GateBucketDto(0, 0, 0);
        for (GateProjection g : rows) {
            GateBucketDto bucket = new GateBucketDto(g.getScans(), g.getAccepted(), g.getRejected());
            if ("public".equals(g.getGate())) {
                pub = bucket;
            } else if ("vip".equals(g.getGate())) {
                vip = bucket;
            }
        }
        return new GateDto(pub, vip);
    }

    /** Acceptance rate 0..100 rounded to one decimal; 0 when there are no scans. */
    private double rate(long accepted, long total) {
        if (total <= 0) {
            return 0.0;
        }
        return Math.round((accepted * 1000.0) / total) / 10.0;
    }

    private LocalDate toLocalDate(Date d) {
        return d == null ? null : d.toLocalDate();
    }
}

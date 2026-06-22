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

/**
 * Assembles the stats DTOs from the read-only aggregate queries.
 *
 * Year awareness: every dashboard method now takes a nullable {@code year}.
 * {@code null} means "Toutes les années" and reproduces the original numbers;
 * a concrete year filters by evenement.ddate's year. The per-event detail
 * (eventDetail) is intentionally NOT year-filtered: it is reached by a unique
 * event id, which already pins it to a single edition.
 */
@Service
@Transactional(readOnly = true)
public class StatsService {

    private final StatsRepository repo;

    /**
     * Optional short-TTL cache for the Recette aggregates (Change §5).
     * 0 disables it. Configurable via fih.recette.cache-ttl-seconds.
     *
     * Trade-off: a non-zero TTL means a value can be up to <ttl> seconds stale,
     * but repeated views inside that window cost ONE query instead of many. For
     * Recette the underlying queries are already cheap (they read the tiny,
     * non-growing `generation` table), so this is a light optimisation, not a
     * necessity — keep it small (30 s) or set it to 0 to always read live.
     */
    private final long recetteCacheTtlSeconds;
    private final ConcurrentHashMap<String, CacheEntry> recetteCache = new ConcurrentHashMap<>();

    private record CacheEntry(long expiresAtMillis, Object value) {}

    public StatsService(StatsRepository repo,
                        @Value("${fih.recette.cache-ttl-seconds:30}") long recetteCacheTtlSeconds) {
        this.repo = repo;
        this.recetteCacheTtlSeconds = recetteCacheTtlSeconds;
    }

    /**
     * Returns a cached value if present and fresh, otherwise runs {@code loader},
     * stores it and returns it. {@code refresh=true} (the "Actualiser" button)
     * forces a reload and refreshes the entry. TTL <= 0 bypasses the cache
     * entirely. Stored values are immutable DTO lists, so sharing them is safe.
     */
    @SuppressWarnings("unchecked")
    private <T> T cached(String key, boolean refresh, Supplier<T> loader) {
        if (recetteCacheTtlSeconds <= 0) {
            return loader.get();
        }
        long now = System.currentTimeMillis();
        if (!refresh) {
            CacheEntry e = recetteCache.get(key);
            if (e != null && e.expiresAtMillis() > now) {
                return (T) e.value();
            }
        }
        T value = loader.get();
        recetteCache.put(key, new CacheEntry(now + recetteCacheTtlSeconds * 1000L, value));
        return value;
    }

    /** Distinct festival years present in the database, most-recent first. */
    public List<Integer> availableYears() {
        return repo.availableYears();
    }

    public OverviewDto overview(Integer year) {
        OverviewCountsProjection c = repo.overviewCounts(year);
        BusiestEventProjection busiest = repo.busiestEvent(year);
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

    public List<EntryByDayDto> entriesByDay(Integer year) {
        return repo.entriesByDay(year).stream()
                .map(p -> new EntryByDayDto(
                        toLocalDate(p.getDate()), p.getScans(), p.getAccepted(), p.getRejected()))
                .toList();
    }

    public GateDto gate(Integer year) {
        return toGateDto(repo.gateBreakdown(year));
    }

    public TicketTypesDto ticketTypes(Integer year) {
        TicketTypesProjection t = repo.ticketTypes(year);
        return new TicketTypesDto(
                new TicketBucketDto(t.getBilletIssued(), t.getBilletScanned()),
                new TicketBucketDto(t.getVoucherIssued(), t.getVoucherScanned()));
    }

    public List<EventRollupDto> events(Integer year) {
        return repo.eventRollups(year).stream().map(this::toRollupDto).toList();
    }

    public EventDetailDto eventDetail(int id) {
        EventRollupProjection e = repo.eventRollup(id);
        if (e == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Event not found");
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
    /** Recette résumé (Change B): revenue per event, Billet / Voucher / Total. */
    public List<RecetteSummaryDto> recetteSummary(Integer year, boolean refresh) {
        return cached("summary:" + year, refresh, () ->
                repo.recetteSummary(year).stream()
                        .map(p -> new RecetteSummaryDto(
                                p.getEventId(), p.getEventTitle(), toLocalDate(p.getEventDate()),
                                p.getBillet(), p.getVoucher(), p.getTotal()))
                        .toList());
    }

    /**
     * Recette détaillée — panel headers (Change C): one aggregated row per event,
     * with a sell-through rate computed from the totals. The per-model rows are
     * loaded separately on expand (recetteDetailRows), so this list stays small.
     */
    public List<RecetteEventHeaderDto> recetteDetailHeaders(Integer year, boolean refresh) {
        return cached("detailHeaders:" + year, refresh, () ->
                repo.recetteDetailHeaders(year).stream()
                        .map(p -> new RecetteEventHeaderDto(
                                p.getEventId(), p.getEventTitle(), toLocalDate(p.getEventDate()),
                                p.getTotalGenere(), p.getTotalVendu(), p.getTotalReste(),
                                p.getRecetteTotale(), rate(p.getTotalVendu(), p.getTotalGenere())))
                        .toList());
    }

    /**
     * Recette détaillée — per-model rows for ONE event (Change C). Always read
     * live (not cached): it is a small, on-demand call triggered by expanding a
     * panel, so it should reflect the latest counters. Taux = vendu / généré.
     */
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
    public List<RecetteGuichetSummaryDto> recetteGuichetSummary(Integer year) {
        return repo.recetteGuichetSummary(year).stream()
                .map(p -> new RecetteGuichetSummaryDto(
                        p.getEventId(), p.getEventTitle(), toLocalDate(p.getEventDate()),
                        p.getBillet(), p.getKit(), p.getTotal()))
                .toList();
    }

    public List<RecetteGuichetDetailDto> recetteGuichetDetail(Integer year) {
        return repo.recetteGuichetDetail(year).stream()
                .map(p -> new RecetteGuichetDetailDto(
                        p.getEventId(), p.getEventTitle(), toLocalDate(p.getEventDate()),
                        p.getModelId(), p.getModelName(),
                        p.getBilletLivraison(), p.getBilletVente(), p.getBilletPrixUnitaire(),
                        p.getBilletRecette(), p.getBilletReste(), p.getKit()))
                .toList();
    }

    // --------------------------------------------- Statistique des tourniquets
    /**
     * Groups the per-(event x model) rows into one block per event, computing the
     * header totals (Audience, Transactions Billets/Vouchers, Tourniquets) from the
     * rows so they always match the table. Event order from the query is preserved.
     */
    public List<TourniquetEventDto> tourniquets(Integer year) {
        List<TourniquetEventDto> out = new java.util.ArrayList<>();
        java.util.Map<Integer, Integer> indexByEvent = new java.util.HashMap<>();
        // mutable accumulators per event, indexed in parallel with `out`
        List<List<TourniquetRowDto>> rowsByIndex = new java.util.ArrayList<>();
        List<long[]> totalsByIndex = new java.util.ArrayList<>(); // [audience, billetTx, voucherTx]
        List<Object[]> headByIndex = new java.util.ArrayList<>();  // [eventId, title, date]

        for (TourniquetProjection p : repo.tourniquets(year)) {
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
    public RejetsDto rejets(Integer year) {
        var kpi = repo.rejetsKpi(year);
        long rejets = kpi == null ? 0 : kpi.getRejets();
        long total = kpi == null ? 0 : kpi.getTotal();
        long acceptes = total - rejets;
        double taux = total > 0 ? (rejets * 100.0) / total : 0.0;

        var scans = repo.rejetsScans(year).stream()
                .map(s -> new RejetsDto.Scan(
                        s.getCodebarre(), s.getEventTitle(), s.getPorte(),
                        s.getDateTime() == null ? null : s.getDateTime().toLocalDateTime(),
                        s.getDescription()))
                .toList();

        return new RejetsDto(
                rejets, acceptes, total, Math.round(taux * 10.0) / 10.0,
                repo.rejetsParCategorie(year).stream()
                        .map(g -> new RejetsDto.Groupe(g.getLabel(), g.getValeur())).toList(),
                repo.rejetsParEvenement(year).stream()
                        .map(e -> new RejetsDto.Evenement(e.getEventId(), e.getEventTitle(),
                                toLocalDate(e.getEventDate()), e.getRejets())).toList(),
                repo.rejetsParPorte(year).stream()
                        .map(g -> new RejetsDto.Groupe(g.getLabel(), g.getValeur())).toList(),
                repo.rejetsParModele(year).stream()
                        .map(m -> new RejetsDto.Modele(m.getModelId(), m.getModelName(), m.getRejets())).toList(),
                repo.rejetsParJour(year).stream()
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

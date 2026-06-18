package com.fih.companion.stats;

import com.fih.companion.stats.dto.*;
import com.fih.companion.stats.projection.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.sql.Date;
import java.time.LocalDate;
import java.util.List;

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

    public StatsService(StatsRepository repo) {
        this.repo = repo;
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
    public List<RecetteSummaryDto> recetteSummary(Integer year) {
        return repo.recetteSummary(year).stream()
                .map(p -> new RecetteSummaryDto(
                        p.getEventId(), p.getEventTitle(), toLocalDate(p.getEventDate()),
                        p.getBillet(), p.getVoucher(), p.getKit(), p.getTotal()))
                .toList();
    }

    public List<RecetteDetailDto> recetteDetail(Integer year) {
        return repo.recetteDetail(year).stream()
                .map(p -> new RecetteDetailDto(
                        p.getEventId(), p.getEventTitle(), toLocalDate(p.getEventDate()),
                        p.getModelId(), p.getModelName(), p.getMontant(),
                        p.getVoucherGeneration(), p.getVoucherVente(), p.getVoucherReste(),
                        p.getBilletGeneration(), p.getBilletVente(), p.getBilletReste(),
                        p.getKitGeneration(), p.getKitVente(), p.getKitReste(),
                        p.getTotal(), p.getRecetteTnd()))
                .toList();
    }

    // ----------------------------------------------------------- helpers
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

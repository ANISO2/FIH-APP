package com.fih.companion.stats;

import com.fih.companion.stats.dto.EntryByDayDto;
import com.fih.companion.stats.dto.EventDetailDto;
import com.fih.companion.stats.dto.EventRollupDto;
import com.fih.companion.stats.dto.GateDto;
import com.fih.companion.stats.dto.OverviewDto;
import com.fih.companion.stats.dto.RecetteEventHeaderDto;
import com.fih.companion.stats.dto.RecetteGuichetDetailDto;
import com.fih.companion.stats.dto.RecetteGuichetSummaryDto;
import com.fih.companion.stats.dto.RecetteModelRowDto;
import com.fih.companion.stats.dto.RecetteSummaryDto;
import com.fih.companion.stats.dto.RejetsDto;
import com.fih.companion.stats.dto.TicketTypesDto;
import com.fih.companion.stats.dto.TourniquetEventDto;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * HTTP surface for {@link StatsService}.
 *
 * Every route below is a thin pass-through: no logic lives here, the service
 * already owns the caching, the SQL and the DTO shaping. Nothing was invented —
 * each mapping is the pairing of an existing StatsService public method with the
 * URL the Angular backoffice already calls in fih-admin/src/app/core/stats.service.ts.
 *
 * Authorization is declared centrally in SecurityConfig, not here:
 *   - GET /overview, /entries-by-day, /gate, /ticket-types, /tourniquets, /rejets
 *       -> hasAnyRole("DEVICE", "ADMIN")   (the Flutter scanner dashboard reads these)
 *   - everything else under /api/stats/**  -> hasRole("ADMIN")
 *
 * NOTE — /api/stats/years is referenced by fih-verifier's ApiEndpoints.statsYears()
 * but StatsService exposes no years() method, so no mapping is declared for it.
 * Adding one would mean inventing a return shape. Flagged, deliberately untouched.
 */
@RestController
@RequestMapping("/api/stats")
public class StatsController {

    private final StatsService service;

    public StatsController(StatsService service) {
        this.service = service;
    }

    // ------------------------------------------------------- Vue d'ensemble

    @GetMapping("/overview")
    public OverviewDto overview() {
        return service.overview();
    }

    @GetMapping("/entries-by-day")
    public List<EntryByDayDto> entriesByDay() {
        return service.entriesByDay();
    }

    @GetMapping("/gate")
    public GateDto gate() {
        return service.gate();
    }

    @GetMapping("/ticket-types")
    public TicketTypesDto ticketTypes() {
        return service.ticketTypes();
    }

    // -------------------------------------------------------------- Events

    @GetMapping("/events")
    public List<EventRollupDto> events() {
        return service.events();
    }

    @GetMapping("/events/{id}")
    public EventDetailDto eventDetail(@PathVariable int id) {
        return service.eventDetail(id);
    }

    // ------------------------------------------------------------- Recette
    // `refresh=true` (bouton « Actualiser ») bypasses the service's short cache.
    // The front omits the param entirely when it is false, hence defaultValue.

    @GetMapping("/recette/summary")
    public List<RecetteSummaryDto> recetteSummary(
            @RequestParam(required = false, defaultValue = "false") boolean refresh) {
        return service.recetteSummary(refresh);
    }

    @GetMapping("/recette/detail")
    public List<RecetteEventHeaderDto> recetteDetailHeaders(
            @RequestParam(required = false, defaultValue = "false") boolean refresh) {
        return service.recetteDetailHeaders(refresh);
    }

    @GetMapping("/recette/detail/{eventId}")
    public List<RecetteModelRowDto> recetteDetailRows(@PathVariable int eventId) {
        return service.recetteDetailRows(eventId);
    }

    // --------------------------------------------------- Recette / guichet

    @GetMapping("/recette/guichet/summary")
    public List<RecetteGuichetSummaryDto> recetteGuichetSummary() {
        return service.recetteGuichetSummary();
    }

    @GetMapping("/recette/guichet/detail")
    public List<RecetteGuichetDetailDto> recetteGuichetDetail() {
        return service.recetteGuichetDetail();
    }

    // --------------------------------------------------------- Tourniquets

    @GetMapping("/tourniquets")
    public List<TourniquetEventDto> tourniquets(
            @RequestParam(required = false, defaultValue = "false") boolean refresh) {
        return service.tourniquets(refresh);
    }

    // -------------------------------------------------------------- Rejets

    @GetMapping("/rejets")
    public RejetsDto rejets(
            @RequestParam(required = false, defaultValue = "false") boolean refresh) {
        return service.rejets(refresh);
    }
}

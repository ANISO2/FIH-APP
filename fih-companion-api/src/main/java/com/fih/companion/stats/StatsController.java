package com.fih.companion.stats;

import com.fih.companion.stats.dto.*;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Read-only statistics endpoints. All of /api/stats/** is already locked behind
 * the admin JWT by SecurityConfig, so no extra security annotations are needed.
 *
 * Year filter: every dashboard endpoint accepts an optional {@code ?year=YYYY}.
 * Omit it (or send nothing) for "Toutes les années". The list of valid years is
 * served by {@code GET /api/stats/years} so the backoffice never hardcodes them.
 */
@RestController
@RequestMapping("/api/stats")
public class StatsController {

    private final StatsService service;

    public StatsController(StatsService service) {
        this.service = service;
    }

    /** Distinct festival years present in the DB, most-recent first. */
    @GetMapping("/years")
    public List<Integer> years() {
        return service.availableYears();
    }

    @GetMapping("/overview")
    public OverviewDto overview(@RequestParam(required = false) Integer year) {
        return service.overview(year);
    }

    @GetMapping("/entries-by-day")
    public List<EntryByDayDto> entriesByDay(@RequestParam(required = false) Integer year) {
        return service.entriesByDay(year);
    }

    @GetMapping("/gate")
    public GateDto gate(@RequestParam(required = false) Integer year) {
        return service.gate(year);
    }

    @GetMapping("/ticket-types")
    public TicketTypesDto ticketTypes(@RequestParam(required = false) Integer year) {
        return service.ticketTypes(year);
    }

    @GetMapping("/events")
    public List<EventRollupDto> events(@RequestParam(required = false) Integer year) {
        return service.events(year);
    }

    @GetMapping("/events/{id}")
    public EventDetailDto eventDetail(@PathVariable int id) {
        return service.eventDetail(id);
    }

    // ----------------------------------------------------------- Recette
    // The summary and the détaillée panel HEADERS support an optional
    // ?refresh=true (the "Actualiser" button) which bypasses the short-TTL
    // server cache and reloads live. The per-event ROWS are loaded lazily, one
    // event at a time, when a panel is expanded.
    @GetMapping("/recette/summary")
    public List<RecetteSummaryDto> recetteSummary(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false, defaultValue = "false") boolean refresh) {
        return service.recetteSummary(year, refresh);
    }

    /** Détaillée — one collapsible panel header (totals) per event. */
    @GetMapping("/recette/detail")
    public List<RecetteEventHeaderDto> recetteDetail(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false, defaultValue = "false") boolean refresh) {
        return service.recetteDetailHeaders(year, refresh);
    }

    /** Détaillée — per-model rows for one event, loaded on expand. */
    @GetMapping("/recette/detail/{eventId}")
    public List<RecetteModelRowDto> recetteDetailRows(@PathVariable int eventId) {
        return service.recetteDetailRows(eventId);
    }

    // ------------------------------------------------- Recette par guichet (§5.2)
    @GetMapping("/recette/guichet/summary")
    public List<RecetteGuichetSummaryDto> recetteGuichetSummary(@RequestParam(required = false) Integer year) {
        return service.recetteGuichetSummary(year);
    }

    @GetMapping("/recette/guichet/detail")
    public List<RecetteGuichetDetailDto> recetteGuichetDetail(@RequestParam(required = false) Integer year) {
        return service.recetteGuichetDetail(year);
    }

    // --------------------------------------------- Statistique des tourniquets (§5.3)
    @GetMapping("/tourniquets")
    public List<TourniquetEventDto> tourniquets(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false, defaultValue = "false") boolean refresh) {
        return service.tourniquets(year, refresh);
    }

    // --------------------------------------------- Analyse des rejets (Part C)
    @GetMapping("/rejets")
    public RejetsDto rejets(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false, defaultValue = "false") boolean refresh) {
        return service.rejets(year, refresh);
    }
}

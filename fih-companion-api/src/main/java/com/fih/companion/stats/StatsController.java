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
    @GetMapping("/recette/summary")
    public List<RecetteSummaryDto> recetteSummary(@RequestParam(required = false) Integer year) {
        return service.recetteSummary(year);
    }

    @GetMapping("/recette/detail")
    public List<RecetteDetailDto> recetteDetail(@RequestParam(required = false) Integer year) {
        return service.recetteDetail(year);
    }
}

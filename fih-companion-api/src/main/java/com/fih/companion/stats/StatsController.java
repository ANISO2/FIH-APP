package com.fih.companion.stats;

import com.fih.companion.stats.dto.*;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;


@RestController
@RequestMapping("/api/stats")
public class StatsController {

    private final StatsService service;

    public StatsController(StatsService service) {
        this.service = service;
    }

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

    // ------------------------------------------------- Recette par guichet
    @GetMapping("/recette/guichet/summary")
    public List<RecetteGuichetSummaryDto> recetteGuichetSummary(@RequestParam(required = false) Integer year) {
        return service.recetteGuichetSummary(year);
    }

    @GetMapping("/recette/guichet/detail")
    public List<RecetteGuichetDetailDto> recetteGuichetDetail(@RequestParam(required = false) Integer year) {
        return service.recetteGuichetDetail(year);
    }

    // --------------------------------------------- Statistique des tourniquets
    @GetMapping("/tourniquets")
    public List<TourniquetEventDto> tourniquets(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false, defaultValue = "false") boolean refresh) {
        return service.tourniquets(year, refresh);
    }

    // --------------------------------------------- Analyse des rejets (
    @GetMapping("/rejets")
    public RejetsDto rejets(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false, defaultValue = "false") boolean refresh) {
        return service.rejets(year, refresh);
    }
}

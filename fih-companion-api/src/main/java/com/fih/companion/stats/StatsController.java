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

    @GetMapping("/events")
    public List<EventRollupDto> events() {
        return service.events();
    }

    @GetMapping("/events/{id}")
    public EventDetailDto eventDetail(@PathVariable int id) {
        return service.eventDetail(id);
    }

    // ----------------------------------------------------------- Recette

    @GetMapping("/recette/summary")
    public List<RecetteSummaryDto> recetteSummary(
            @RequestParam(required = false, defaultValue = "false") boolean refresh) {
        return service.recetteSummary(refresh);
    }

    /** Détaillée — one collapsible panel header (totals) per event. */
    @GetMapping("/recette/detail")
    public List<RecetteEventHeaderDto> recetteDetail(
            @RequestParam(required = false, defaultValue = "false") boolean refresh) {
        return service.recetteDetailHeaders(refresh);
    }

    /** Détaillée — per-model rows for one event, loaded on expand. */
    @GetMapping("/recette/detail/{eventId}")
    public List<RecetteModelRowDto> recetteDetailRows(@PathVariable int eventId) {
        return service.recetteDetailRows(eventId);
    }

    // ------------------------------------------------- Recette par guichet
    @GetMapping("/recette/guichet/summary")
    public List<RecetteGuichetSummaryDto> recetteGuichetSummary() {
        return service.recetteGuichetSummary();
    }

    @GetMapping("/recette/guichet/detail")
    public List<RecetteGuichetDetailDto> recetteGuichetDetail() {
        return service.recetteGuichetDetail();
    }

    // --------------------------------------------- Statistique des tourniquets
    @GetMapping("/tourniquets")
    public List<TourniquetEventDto> tourniquets(
            @RequestParam(required = false, defaultValue = "false") boolean refresh) {
        return service.tourniquets(refresh);
    }

    // --------------------------------------------- Analyse des rejets (
    @GetMapping("/rejets")
    public RejetsDto rejets(
            @RequestParam(required = false, defaultValue = "false") boolean refresh) {
        return service.rejets(refresh);
    }
}

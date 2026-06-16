package com.fih.companion.evenement;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * GET /api/events -> the 34 events as DTOs.
 *
 * @Transactional(readOnly = true) is our default transaction posture for read
 * paths: it tells Spring/Hibernate this unit of work will not write, which lets
 * the driver/DB optimize and documents intent. It is defense-in-depth on top of
 * the fih_ro role.
 */
@RestController
@RequestMapping("/api/events")
public class EventController {

    private final EvenementRepository repository;
    private final EventMapper mapper;

    public EventController(EvenementRepository repository, EventMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @GetMapping
    @Transactional(readOnly = true)
    public List<EventDto> list() {
        return repository.findAll()
                .stream()
                .map(mapper::toDto)
                .toList();
    }
}

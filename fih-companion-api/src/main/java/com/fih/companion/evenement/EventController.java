package com.fih.companion.evenement;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;


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
        return repository.findVisible()
                .stream()
                .map(mapper::toDto)
                .toList();
    }
}

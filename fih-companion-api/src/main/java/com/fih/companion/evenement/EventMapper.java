package com.fih.companion.evenement;

import org.springframework.stereotype.Component;

 @Component
public class EventMapper {

    public EventDto toDto(Evenement e) {
        return new EventDto(
                e.getReference(),
                e.getTitre(),
                e.getDdate(),
                e.isBillet(),
                e.isVoucher(),
                e.getLocation() == null ? null : e.getLocation().getReference()
        );
    }
}

package com.fih.companion.stats.projection;

import java.sql.Date;

/** Rejets count per event. */
public interface RejetEvenementProjection {
    int getEventId();
    String getEventTitle();
    Date getEventDate();
    long getRejets();
}

package com.fih.companion.stats.projection;

import java.sql.Date;

 public interface RejetEvenementProjection {
    int getEventId();
    String getEventTitle();
    Date getEventDate();
    long getRejets();
}

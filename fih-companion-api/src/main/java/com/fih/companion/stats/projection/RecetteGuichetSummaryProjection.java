package com.fih.companion.stats.projection;

import java.sql.Date;

 public interface RecetteGuichetSummaryProjection {
    int getEventId();
    String getEventTitle();
    Date getEventDate();
    double getBillet();
    double getKit();
    double getTotal();
}

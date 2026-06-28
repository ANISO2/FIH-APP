package com.fih.companion.stats.projection;

import java.sql.Date;

 public interface RecetteSummaryProjection {
    int getEventId();
    String getEventTitle();
    Date getEventDate();
    double getBillet();
    double getVoucher();
    double getTotal();
}

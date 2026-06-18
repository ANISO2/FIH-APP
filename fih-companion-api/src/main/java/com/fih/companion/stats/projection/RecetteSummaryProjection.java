package com.fih.companion.stats.projection;

import java.sql.Date;

/** Native-query projection backing the "Recette résumé" table (revenue TND). */
public interface RecetteSummaryProjection {
    int getEventId();
    String getEventTitle();
    Date getEventDate();
    double getBillet();
    double getVoucher();
    double getKit();
    double getTotal();
}

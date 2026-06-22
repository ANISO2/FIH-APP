package com.fih.companion.stats.projection;

import java.sql.Date;

/** Projection for "Recette par guichet — résumé" (guichet revenue TND per event). */
public interface RecetteGuichetSummaryProjection {
    int getEventId();
    String getEventTitle();
    Date getEventDate();
    double getBillet();
    double getKit();
    double getTotal();
}

package com.fih.companion.stats.projection;

import java.sql.Date;

 public interface RecetteEventHeaderProjection {
    int getEventId();
    String getEventTitle();
    Date getEventDate();
    long getTotalGenere();
    long getTotalVendu();
    long getTotalReste();
    double getRecetteTotale();
}

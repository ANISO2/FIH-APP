package com.fih.companion.stats.projection;

import java.sql.Date;

/** Rejets count per day (datetransaction). */
public interface RejetJourProjection {
    Date getJour();
    long getRejets();
}

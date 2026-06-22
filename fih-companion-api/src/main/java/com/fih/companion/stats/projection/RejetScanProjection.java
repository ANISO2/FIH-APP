package com.fih.companion.stats.projection;

import java.sql.Timestamp;

/** One rejected scan row for the table. */
public interface RejetScanProjection {
    String getCodebarre();
    String getEventTitle();
    String getPorte();
    Timestamp getDateTime();
    String getDescription();
}

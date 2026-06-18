package com.fih.companion.stats.projection;

import java.sql.Date;

public interface EntryByDayProjection {
    Date getDate();
    long getScans();
    long getAccepted();
    long getRejected();
}

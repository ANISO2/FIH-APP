package com.fih.companion.stats.projection;

import java.sql.Date;

public interface EventRollupProjection {
    int getEventId();
    String getTitle();
    Date getDate();
    long getScans();
    long getAccepted();
    long getRejected();
    long getPublicScans();
    long getVipScans();
}

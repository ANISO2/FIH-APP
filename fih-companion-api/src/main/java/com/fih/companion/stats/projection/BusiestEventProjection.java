package com.fih.companion.stats.projection;

import java.sql.Date;

public interface BusiestEventProjection {
    String getTitle();
    Date getDate();
    long getScans();
}

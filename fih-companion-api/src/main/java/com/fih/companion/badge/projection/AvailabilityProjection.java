package com.fih.companion.badge.projection;

import java.sql.Date;

public interface AvailabilityProjection {
    int getEventId();
    String getEventTitle();
    Date getEventDate();
    int getModelId();
    String getModelName();
    long getBilletCount();
    long getVoucherCount();
    long getInjectedCount();
}

package com.fih.companion.stats.projection;

import java.sql.Date;


public interface TourniquetProjection {
    int getEventId();
    String getEventTitle();
    Date getEventDate();
    int getModelId();
    String getModelName();
    long getBilletCodes();
    long getVoucherCodes();
    long getBilletTx();
    long getVoucherTx();
}

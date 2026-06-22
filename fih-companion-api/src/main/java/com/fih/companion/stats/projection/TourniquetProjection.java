package com.fih.companion.stats.projection;

import java.sql.Date;

/**
 * Native-query projection for one (event x model) row of "Statistique des
 * tourniquets". Codes = issued barcodes that can grant access; Tx = turnstile
 * scan events resolving to that ticket's event x model.
 */
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

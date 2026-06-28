package com.fih.companion.stats.projection;

import java.sql.Date;

 public interface RecetteDetailProjection {
    int getEventId();
    String getEventTitle();
    Date getEventDate();
    int getModelId();
    String getModelName();
    double getMontant();
    long getVoucherGeneration();
    long getVoucherVente();
    long getVoucherReste();
    long getBilletGeneration();
    long getBilletVente();
    long getBilletReste();
    long getKitGeneration();
    long getKitVente();
    long getKitReste();
    long getTotal();
    double getRecetteTnd();
}

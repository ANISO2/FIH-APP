package com.fih.companion.stats.projection;

import java.sql.Date;

 public interface RecetteGuichetDetailProjection {
    int getEventId();
    String getEventTitle();
    Date getEventDate();
    int getModelId();
    String getModelName();
    long getBilletLivraison();
    long getBilletVente();
    double getBilletPrixUnitaire();
    double getBilletRecette();
    long getBilletReste();
    double getKit();
}

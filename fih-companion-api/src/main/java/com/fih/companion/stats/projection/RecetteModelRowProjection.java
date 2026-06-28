package com.fih.companion.stats.projection;
 public interface RecetteModelRowProjection {
    int getModelId();
    String getModelName();
    double getMontant();
    long getBilletGeneration();
    long getBilletVente();
    long getBilletReste();
    long getVoucherGeneration();
    long getVoucherVente();
    long getVoucherReste();
    long getTotalVendu();
    double getRecetteTnd();
}

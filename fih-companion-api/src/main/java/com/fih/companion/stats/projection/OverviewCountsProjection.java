package com.fih.companion.stats.projection;

 public interface OverviewCountsProjection {
    long getTotalEvents();
    long getTotalBillets();
    long getTotalVouchers();
    long getTotalScans();
    long getAcceptedScans();
    long getRejectedScans();
    long getPublicScans();
    long getVipScans();
}

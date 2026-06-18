package com.fih.companion.stats.projection;

/** Scalar counts for the overview (assembled with busiest event in the service). */
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

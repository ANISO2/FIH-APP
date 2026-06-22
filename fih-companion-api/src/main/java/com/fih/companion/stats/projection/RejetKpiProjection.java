package com.fih.companion.stats.projection;

/** Single-row KPI: refused count + total scans for the year. */
public interface RejetKpiProjection {
    long getRejets();
    long getTotal();
}

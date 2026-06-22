package com.fih.companion.stats.projection;

/** Rejets count per ticket model. */
public interface RejetModeleProjection {
    int getModelId();
    String getModelName();
    long getRejets();
}

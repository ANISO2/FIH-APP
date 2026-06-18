package com.fih.companion.badge.projection;

/** Light row for photo-coverage counting across many records. */
public interface CodeRowProjection {
    int getEventId();
    int getModelId();
    String getCodebarre();
    String getNumeroserie();
}

package com.fih.companion.badge.projection;

public interface CountsProjection {
    long getAffected();
    long getPending();
    long getTotal();
}

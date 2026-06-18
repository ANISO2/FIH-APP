package com.fih.companion.stats.projection;

public interface GateProjection {
    String getGate();   // "public" or "vip"
    long getScans();
    long getAccepted();
    long getRejected();
}

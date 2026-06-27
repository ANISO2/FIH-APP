package com.fih.companion.verification.projection;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * One row of a turnstile access log (tturnstile = Public, vipaccess = VIP).
 * Closed interface projection: each getter maps to the matching column alias in
 * the native query. Read-only.
 */
public interface AccessLogProjection {
    Integer getReference();
    String getCodebarre();
    LocalDate getDatetransaction();
    LocalDateTime getHeuretransaction();
    String getPorte();
    Boolean getTransactionstate(); // true = granted (green), false = denied (red)
}

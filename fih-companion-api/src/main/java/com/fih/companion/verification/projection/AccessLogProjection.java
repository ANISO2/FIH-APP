package com.fih.companion.verification.projection;

import java.time.LocalDate;
import java.time.LocalDateTime;


public interface AccessLogProjection {
    Integer getReference();
    String getCodebarre();
    LocalDate getDatetransaction();
    LocalDateTime getHeuretransaction();
    String getPorte();
    Boolean getTransactionstate(); // true = granted (green), false = denied (red)
}

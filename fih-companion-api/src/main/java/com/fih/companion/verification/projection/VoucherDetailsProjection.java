package com.fih.companion.verification.projection;

import java.time.LocalDate;

/** Management extras for a voucher, loaded lazily on the details screen. */
public interface VoucherDetailsProjection {
    String getNumeroserie();
    String getCodebarre();
    Integer getCommande();    // voucherorder.code
    LocalDate getDateVente();  // voucher.datevente
}

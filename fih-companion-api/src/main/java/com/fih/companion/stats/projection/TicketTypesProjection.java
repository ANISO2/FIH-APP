package com.fih.companion.stats.projection;

public interface TicketTypesProjection {
    long getBilletIssued();
    long getBilletScanned();
    long getVoucherIssued();
    long getVoucherScanned();
}

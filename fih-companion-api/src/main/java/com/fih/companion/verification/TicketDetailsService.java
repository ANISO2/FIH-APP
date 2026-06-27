package com.fih.companion.verification;

import com.fih.companion.repository.BilletRepository;
import com.fih.companion.repository.VoucherRepository;
import com.fih.companion.verification.dto.AccessLogEntry;
import com.fih.companion.verification.dto.TicketDetailsResponse;
import com.fih.companion.verification.projection.AccessLogProjection;
import com.fih.companion.verification.projection.BilletDetailsProjection;
import com.fih.companion.verification.projection.VoucherDetailsProjection;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Lazy detail loader for the ℹ screen: management extras + the Public/VIP access
 * history. Strictly read-only. Kept OFF the hot verify path so scanning stays a
 * single lean query; this only runs when the operator opens details.
 *
 * All access-log reads are by the INDEXED numeroserie (billet/voucher FK on
 * tturnstile/vipaccess), never the un-indexed codebarre — so detail loads are
 * fast even for a ticket with thousands of scans.
 */
@Service
@Transactional(readOnly = true)
public class TicketDetailsService {

    private final BilletRepository billetRepository;
    private final VoucherRepository voucherRepository;

    public TicketDetailsService(BilletRepository billetRepository,
                                VoucherRepository voucherRepository) {
        this.billetRepository = billetRepository;
        this.voucherRepository = voucherRepository;
    }

    public TicketDetailsResponse billetDetails(String numeroserie) {
        BilletDetailsProjection d = billetRepository.findBilletDetails(numeroserie).orElse(null);
        List<AccessLogEntry> pub = map(billetRepository.findPublicAccessLog(numeroserie));
        List<AccessLogEntry> vip = map(billetRepository.findVipAccessLog(numeroserie));
        return new TicketDetailsResponse(
                "BILLET",
                numeroserie,
                d != null ? d.getCodebarre() : null,
                d != null ? d.getLivre() : null,
                d != null ? d.getDateLivraison() : null,
                d != null ? d.getDateVente() : null,
                null,
                pub,
                vip);
    }

    public TicketDetailsResponse voucherDetails(String numeroserie) {
        VoucherDetailsProjection d = voucherRepository.findVoucherDetails(numeroserie).orElse(null);
        List<AccessLogEntry> pub = map(voucherRepository.findPublicAccessLog(numeroserie));
        List<AccessLogEntry> vip = map(voucherRepository.findVipAccessLog(numeroserie));
        return new TicketDetailsResponse(
                "VOUCHER",
                numeroserie,
                d != null ? d.getCodebarre() : null,
                null,
                null,
                d != null ? d.getDateVente() : null,
                d != null ? d.getCommande() : null,
                pub,
                vip);
    }

    private List<AccessLogEntry> map(List<AccessLogProjection> rows) {
        return rows.stream()
                .map(r -> new AccessLogEntry(
                        r.getReference(),
                        r.getCodebarre(),
                        r.getDatetransaction(),
                        r.getHeuretransaction(),
                        r.getPorte(),
                        Boolean.TRUE.equals(r.getTransactionstate())))
                .toList();
    }
}

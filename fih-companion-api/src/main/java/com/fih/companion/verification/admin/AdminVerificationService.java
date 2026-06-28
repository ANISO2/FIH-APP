package com.fih.companion.verification.admin;

import com.fih.companion.badge.dto.PageDto;
import com.fih.companion.repository.BilletRepository;
import com.fih.companion.repository.VoucherRepository;
import com.fih.companion.verification.VerificationResult;
import com.fih.companion.verification.VerificationService;
import com.fih.companion.verification.dto.AccessLogEntry;
import com.fih.companion.verification.dto.AdminTicketDetailsDto;
import com.fih.companion.verification.dto.BilletSearchRowDto;
import com.fih.companion.verification.dto.VoucherSearchRowDto;
import com.fih.companion.verification.projection.AccessLogProjection;
import com.fih.companion.verification.projection.BilletSearchProjection;
import com.fih.companion.verification.projection.VoucherSearchProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;


@Service
@Transactional(readOnly = true)
public class AdminVerificationService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final int DEFAULT_PAGE_SIZE = 20;

    private final AdminBilletSearchRepository billetSearch;
    private final AdminVoucherSearchRepository voucherSearch;
    private final VerificationService verificationService;
    private final BilletRepository billetRepository;
    private final VoucherRepository voucherRepository;

    public AdminVerificationService(AdminBilletSearchRepository billetSearch,
                                    AdminVoucherSearchRepository voucherSearch,
                                    VerificationService verificationService,
                                    BilletRepository billetRepository,
                                    VoucherRepository voucherRepository) {
        this.billetSearch = billetSearch;
        this.voucherSearch = voucherSearch;
        this.verificationService = verificationService;
        this.billetRepository = billetRepository;
        this.voucherRepository = voucherRepository;
    }

    // -------------------------------------------------------------- BILLET
    public PageDto<BilletSearchRowDto> searchBillets(String value, String field, String mode, int page, int size) {
        String v = value == null ? "" : value.trim();
        Pageable pageable = pageable(page, size);
        Page<BilletSearchProjection> result;
        if (v.isEmpty()) {
            return empty(page, size);
        }
        if (numeroserie(field)) {
            result = prefix(mode)
                    ? billetSearch.searchByNumeroseriePrefix(like(v), pageable)
                    : billetSearch.searchByNumeroserie(v, pageable);
        } else {
            result = prefix(mode)
                    ? billetSearch.searchByCodebarrePrefix(like(v), pageable)
                    : billetSearch.searchByCodebarre(v, pageable);
        }
        return toPage(result.map(this::toBilletRow));
    }

    public AdminTicketDetailsDto billetDetails(String numeroserie) {
        VerificationResult r = verificationService.verifyBillet(numeroserie);
        List<AccessLogEntry> pub = mapLog(billetRepository.findPublicAccessLog(numeroserie));
        List<AccessLogEntry> vip = mapLog(billetRepository.findVipAccessLog(numeroserie));
        return toDetails("BILLET", numeroserie, r, pub, vip);
    }

    // ------------------------------------------------------------- VOUCHER
    public PageDto<VoucherSearchRowDto> searchVouchers(String value, String field, String mode, int page, int size) {
        String v = value == null ? "" : value.trim();
        Pageable pageable = pageable(page, size);
        Page<VoucherSearchProjection> result;
        if (v.isEmpty()) {
            return empty(page, size);
        }
        if (numeroserie(field)) {
            result = prefix(mode)
                    ? voucherSearch.searchByNumeroseriePrefix(like(v), pageable)
                    : voucherSearch.searchByNumeroserie(v, pageable);
        } else {
            result = prefix(mode)
                    ? voucherSearch.searchByCodebarrePrefix(like(v), pageable)
                    : voucherSearch.searchByCodebarre(v, pageable);
        }
        return toPage(result.map(this::toVoucherRow));
    }

    public AdminTicketDetailsDto voucherDetails(String numeroserie) {
        VerificationResult r = verificationService.verifyVoucher(numeroserie);
        List<AccessLogEntry> pub = mapLog(voucherRepository.findPublicAccessLog(numeroserie));
        List<AccessLogEntry> vip = mapLog(voucherRepository.findVipAccessLog(numeroserie));
        return toDetails("VOUCHER", numeroserie, r, pub, vip);
    }

    // ------------------------------------------------------------- helpers
    private boolean numeroserie(String field) {
        return "numeroserie".equalsIgnoreCase(field);
    }

    private boolean prefix(String mode) {
        return "prefix".equalsIgnoreCase(mode);
    }

     private String like(String value) {
        String escaped = value
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
        return escaped + "%";
    }

    private Pageable pageable(int page, int size) {
        int p = Math.max(page, 0);
        int s = size <= 0 ? DEFAULT_PAGE_SIZE : Math.min(size, MAX_PAGE_SIZE);
        return PageRequest.of(p, s);
    }

    private BilletSearchRowDto toBilletRow(BilletSearchProjection p) {
        return new BilletSearchRowDto(
                p.getNumeroserie(), p.getCodebarre(),
                Boolean.TRUE.equals(p.getActivation()),
                Boolean.TRUE.equals(p.getLivre()),
                Boolean.TRUE.equals(p.getVendu()),
                Boolean.TRUE.equals(p.getUtilise()),
                p.getEventTitle(), p.getModelName(),
                p.getDateVente(), p.getLivreur(), p.getDateLivraison());
    }

    private VoucherSearchRowDto toVoucherRow(VoucherSearchProjection p) {
        return new VoucherSearchRowDto(
                p.getEventTitle(), p.getModelName(),
                p.getNumeroserie(), p.getCodebarre(),
                Boolean.TRUE.equals(p.getUtilisation()),
                Boolean.TRUE.equals(p.getVendu()),
                Boolean.TRUE.equals(p.getActivation()),
                Boolean.TRUE.equals(p.getReservation()),
                p.getCommande());
    }

    private AdminTicketDetailsDto toDetails(String type, String numeroserie,
                                            VerificationResult r, List<AccessLogEntry> pub, List<AccessLogEntry> vip) {
        VerificationResult.Flags f = r.flags();
        return new AdminTicketDetailsDto(
                type, numeroserie, r.codebarre(), r.eventTitle(), r.ticketModel(),
                f.vendu(), f.utilisation(), f.reservation(), f.activation(),
                pub, vip);
    }

    private List<AccessLogEntry> mapLog(List<AccessLogProjection> rows) {
        return rows.stream()
                .map(r -> new AccessLogEntry(
                        r.getReference(), r.getCodebarre(),
                        r.getDatetransaction(),
                        r.getHeuretransaction(),
                        r.getPorte(),
                        Boolean.TRUE.equals(r.getTransactionstate())))
                .toList();
    }

    private <T> PageDto<T> toPage(Page<T> page) {
        return new PageDto<>(page.getContent(), page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages());
    }

    private <T> PageDto<T> empty(int page, int size) {
        int s = size <= 0 ? DEFAULT_PAGE_SIZE : Math.min(size, MAX_PAGE_SIZE);
        return new PageDto<>(List.of(), Math.max(page, 0), s, 0, 0);
    }
}

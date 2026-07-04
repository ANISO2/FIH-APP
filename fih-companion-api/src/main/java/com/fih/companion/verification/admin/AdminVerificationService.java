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
        if (v.isEmpty()) {
            return empty(page, size);
        }
        int p = Math.max(page, 0);
        int s = pageSize(size);
        int offset = p * s;

        long total;
        List<BilletSearchProjection> rows;
        if (numeroserie(field)) {
            if (prefix(mode)) {
                String like = like(v);
                total = billetSearch.countByNumeroseriePrefix(like);
                rows = billetSearch.searchByNumeroseriePrefix(like, s, offset);
            } else {
                total = billetSearch.countByNumeroserie(v);
                rows = billetSearch.searchByNumeroserie(v, s, offset);
            }
        } else {
            if (prefix(mode)) {
                String like = like(v);
                total = billetSearch.countByCodebarrePrefix(like);
                rows = billetSearch.searchByCodebarrePrefix(like, s, offset);
            } else {
                total = billetSearch.countByCodebarre(v);
                rows = billetSearch.searchByCodebarre(v, s, offset);
            }
        }
        return page(rows.stream().map(this::toBilletRow).toList(), p, s, total);
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
        if (v.isEmpty()) {
            return empty(page, size);
        }
        int p = Math.max(page, 0);
        int s = pageSize(size);
        int offset = p * s;

        long total;
        List<VoucherSearchProjection> rows;
        if (numeroserie(field)) {
            if (prefix(mode)) {
                String like = like(v);
                total = voucherSearch.countByNumeroseriePrefix(like);
                rows = voucherSearch.searchByNumeroseriePrefix(like, s, offset);
            } else {
                total = voucherSearch.countByNumeroserie(v);
                rows = voucherSearch.searchByNumeroserie(v, s, offset);
            }
        } else {
            if (prefix(mode)) {
                String like = like(v);
                total = voucherSearch.countByCodebarrePrefix(like);
                rows = voucherSearch.searchByCodebarrePrefix(like, s, offset);
            } else {
                total = voucherSearch.countByCodebarre(v);
                rows = voucherSearch.searchByCodebarre(v, s, offset);
            }
        }
        return page(rows.stream().map(this::toVoucherRow).toList(), p, s, total);
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

    private int pageSize(int size) {
        return size <= 0 ? DEFAULT_PAGE_SIZE : Math.min(size, MAX_PAGE_SIZE);
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

    private <T> PageDto<T> page(List<T> content, int page, int size, long total) {
        int totalPages = size <= 0 ? 0 : (int) Math.ceil((double) total / size);
        return new PageDto<>(content, page, size, total, totalPages);
    }

    private <T> PageDto<T> empty(int page, int size) {
        int s = pageSize(size);
        return new PageDto<>(List.of(), Math.max(page, 0), s, 0, 0);
    }
}

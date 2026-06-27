package com.fih.companion.verification.admin;

import com.fih.companion.badge.dto.PageDto;
import com.fih.companion.verification.dto.AdminTicketDetailsDto;
import com.fih.companion.verification.dto.BilletSearchRowDto;
import com.fih.companion.verification.dto.VoucherSearchRowDto;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Backoffice "Vérification" endpoints (3.2).
 *
 * SECURITY: mounted under /api/stats/** on purpose. SecurityConfig already locks
 * /api/stats/** to ROLE_ADMIN, and this sub-path is NOT in the mobile allow-list
 * (MOBILE_STATS_GET), so it is admin-only with ZERO security change — the mobile
 * device token cannot reach it and the mobile config is untouched.
 *
 * These are distinct from the mobile /api/verify/** verdict endpoints: here an
 * admin searches by an indexed code and gets a paginated LIST, then opens a
 * details modal. Read-only throughout; verdict/identity reads are never cached.
 */
@RestController
@RequestMapping("/api/stats/verification")
public class AdminVerificationController {

    private final AdminVerificationService service;

    public AdminVerificationController(AdminVerificationService service) {
        this.service = service;
    }

    // ----------------------------------------------------------- Billet
    @GetMapping("/billets")
    public PageDto<BilletSearchRowDto> searchBillets(
            @RequestParam(required = false) String value,
            @RequestParam(required = false, defaultValue = "codebarre") String field,
            @RequestParam(required = false, defaultValue = "exact") String mode,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "20") int size) {
        return service.searchBillets(value, field, mode, page, size);
    }

    @GetMapping("/billets/{numeroserie}/details")
    public AdminTicketDetailsDto billetDetails(@PathVariable String numeroserie) {
        return service.billetDetails(numeroserie);
    }

    // ---------------------------------------------------------- Voucher
    @GetMapping("/vouchers")
    public PageDto<VoucherSearchRowDto> searchVouchers(
            @RequestParam(required = false) String value,
            @RequestParam(required = false, defaultValue = "codebarre") String field,
            @RequestParam(required = false, defaultValue = "exact") String mode,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "20") int size) {
        return service.searchVouchers(value, field, mode, page, size);
    }

    @GetMapping("/vouchers/{numeroserie}/details")
    public AdminTicketDetailsDto voucherDetails(@PathVariable String numeroserie) {
        return service.voucherDetails(numeroserie);
    }
}

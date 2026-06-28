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

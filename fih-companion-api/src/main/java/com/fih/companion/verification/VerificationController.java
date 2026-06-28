package com.fih.companion.verification;

import com.fih.companion.verification.dto.TicketDetailsResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/api/verify")
public class VerificationController {

    private final VerificationService service;
    private final TicketDetailsService detailsService;

    public VerificationController(VerificationService service,
                                  TicketDetailsService detailsService) {
        this.service = service;
        this.detailsService = detailsService;
    }

    @GetMapping("/billet")
    public VerificationResult verifyBillet(@RequestParam("code") String code) {
        return service.verifyBillet(code.trim());
    }

    @GetMapping("/voucher")
    public VerificationResult verifyVoucher(@RequestParam("code") String code) {
        return service.verifyVoucher(code.trim());
    }

    /** Lazy details (management extras + Public/VIP access log) for a billet. */
    @GetMapping("/billet/{numeroserie}/details")
    public TicketDetailsResponse billetDetails(@PathVariable String numeroserie) {
        return detailsService.billetDetails(numeroserie);
    }

    /** Lazy details (management extras + Public/VIP access log) for a voucher. */
    @GetMapping("/voucher/{numeroserie}/details")
    public TicketDetailsResponse voucherDetails(@PathVariable String numeroserie) {
        return detailsService.voucherDetails(numeroserie);
    }
}
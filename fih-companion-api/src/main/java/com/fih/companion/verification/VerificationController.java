package com.fih.companion.verification;

import com.fih.companion.verification.dto.TicketDetailsResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints the mobile app calls. Always returns HTTP 200 with a verdict
 * (including NOT_FOUND) so the client handles one shape, never a 500 for a
 * simple miss.
 *
 * SCAN CODE PASSED AS A QUERY PARAM (?code=...), NOT IN THE PATH.
 * --------------------------------------------------------------
 * A scanned QR/barcode can contain anything — a trailing newline, a slash, even
 * a full URL. Putting that raw value in the URL PATH made Tomcat/Spring's
 * firewall reject it (encoded slash / control chars) and return a server error,
 * which looked like "erreur serveur" for every QR. As a query parameter the
 * value is transport-safe, so any code reaches the lookup and simply resolves to
 * a verdict or NOT_FOUND. We also trim it defensively. The backoffice does not
 * use these endpoints, so this change is mobile-only.
 *
 * The /details endpoints keep the numéro de série in the path: that value comes
 * from our own DB (clean digits), never from a raw scan.
 */
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
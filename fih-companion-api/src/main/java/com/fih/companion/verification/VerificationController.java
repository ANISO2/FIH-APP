package com.fih.companion.verification;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints the mobile app calls. Always returns HTTP 200 with a verdict
 * (including NOT_FOUND) so the client handles one shape, never a 500 for a
 * simple miss.
 */
@RestController
@RequestMapping("/api/verify")
public class VerificationController {

    private final VerificationService service;

    public VerificationController(VerificationService service) {
        this.service = service;
    }

    @GetMapping("/billet/{code}")
    public VerificationResult verifyBillet(@PathVariable String code) {
        return service.verifyBillet(code);
    }

    @GetMapping("/voucher/{code}")
    public VerificationResult verifyVoucher(@PathVariable String code) {
        return service.verifyVoucher(code);
    }
}

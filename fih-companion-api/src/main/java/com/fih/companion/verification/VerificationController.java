package com.fih.companion.verification;

import com.fih.companion.diagnostics.ConsoleLog;
import com.fih.companion.verification.dto.TicketDetailsResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/api/verify")
public class VerificationController {

    private static final String TAG = "VERIFY";

    private final VerificationService service;
    private final TicketDetailsService detailsService;

    public VerificationController(VerificationService service,
                                  TicketDetailsService detailsService) {
        this.service = service;
        this.detailsService = detailsService;
    }

    @GetMapping("/billet")
    public VerificationResult verifyBillet(@RequestParam("code") String code) {
        String c = code.trim();
        ConsoleLog.log(TAG, "==> HTTP GET /api/verify/billet — raw code='" + code + "', trimmed='" + c + "'.");
        VerificationResult result = service.verifyBillet(c);
        ConsoleLog.log(TAG, "<== /api/verify/billet — code='" + c + "' verdict=" + result.verdict() + ".");
        return result;
    }

    @GetMapping("/voucher")
    public VerificationResult verifyVoucher(@RequestParam("code") String code) {
        String c = code.trim();
        ConsoleLog.log(TAG, "==> HTTP GET /api/verify/voucher — raw code='" + code + "', trimmed='" + c + "'.");
        VerificationResult result = service.verifyVoucher(c);
        ConsoleLog.log(TAG, "<== /api/verify/voucher — code='" + c + "' verdict=" + result.verdict() + ".");
        return result;
    }

    /** Lazy details (management extras + Public/VIP access log) for a billet. */
    @GetMapping("/billet/{numeroserie}/details")
    public TicketDetailsResponse billetDetails(@PathVariable String numeroserie) {
        ConsoleLog.log(TAG, "==> HTTP GET /api/verify/billet/" + numeroserie + "/details.");
        return detailsService.billetDetails(numeroserie);
    }

    /** Lazy details (management extras + Public/VIP access log) for a voucher. */
    @GetMapping("/voucher/{numeroserie}/details")
    public TicketDetailsResponse voucherDetails(@PathVariable String numeroserie) {
        ConsoleLog.log(TAG, "==> HTTP GET /api/verify/voucher/" + numeroserie + "/details.");
        return detailsService.voucherDetails(numeroserie);
    }
}

package com.fih.companion.verification;

import com.fih.companion.verification.dto.VoucherInfoRequest;
import com.fih.companion.verification.dto.VoucherInfoResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Feature 1 — voucher info via the EXTERNAL web service.
 *
 * Lives under /api/verify/** so it is already secured (ROLE_DEVICE or ROLE_ADMIN)
 * by SecurityConfig, alongside the direct-DB checks in VerificationController.
 *
 * The scanned code is taken as a QUERY PARAM (?code=...), not in the path, so a
 * QR/barcode containing slashes, newlines or other characters can't trip the
 * servlet firewall (which previously caused "erreur serveur"). Trimmed
 * defensively. This controller only delegates to the gateway.
 */
@RestController
@RequestMapping("/api/verify")
public class VoucherInfoController {

    private final VoucherVerificationGateway gateway;

    public VoucherInfoController(VoucherVerificationGateway gateway) {
        this.gateway = gateway;
    }

    /** All info of a paid voucher for a scanned code, via the external service. */
    @GetMapping("/voucher-info")
    public VoucherInfoResponse voucherInfo(@RequestParam("code") String code) {
        return gateway.fetch(new VoucherInfoRequest(code.trim()));
    }
}
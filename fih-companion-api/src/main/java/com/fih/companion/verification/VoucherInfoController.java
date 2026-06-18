package com.fih.companion.verification;

import com.fih.companion.verification.dto.VoucherInfoRequest;
import com.fih.companion.verification.dto.VoucherInfoResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Feature 1 — voucher info via the EXTERNAL web service.
 *
 * Lives under /api/verify/** so it is already secured (ROLE_DEVICE or ROLE_ADMIN)
 * by SecurityConfig, alongside the direct-DB checks in VerificationController.
 * The path is /voucher-info/{code}, distinct from /voucher/{code} (Feature 2,
 * direct to our database).
 *
 * This controller only delegates to {@link VoucherVerificationGateway}; it does
 * not verify anything itself, because verification belongs to the other team.
 */
@RestController
@RequestMapping("/api/verify")
public class VoucherInfoController {

    private final VoucherVerificationGateway gateway;

    public VoucherInfoController(VoucherVerificationGateway gateway) {
        this.gateway = gateway;
    }

    /** All info of a paid voucher for a scanned code, via the external service. */
    @GetMapping("/voucher-info/{code}")
    public VoucherInfoResponse voucherInfo(@PathVariable String code) {
        return gateway.fetch(new VoucherInfoRequest(code));
    }
}

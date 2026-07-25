package com.fih.companion.verification;

import com.fih.companion.verification.dto.VoucherInfoResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;


/**
 * "Info voucher" endpoint (Feature 1). On a voucher scan the mobile calls this;
 * we now delegate to the external ticket-verification service to surface the
 * holder + used status instead of the old "intégration à venir" placeholder.
 * Always HTTP 200 — failures come back as a status + French message in the body.
 */
@RestController
@RequestMapping("/api/verify")
public class VoucherInfoController {

    private final TicketVerifyGateway gateway;

    public VoucherInfoController(TicketVerifyGateway gateway) {
        this.gateway = gateway;
    }

    @GetMapping("/voucher-info")
    public VoucherInfoResponse voucherInfo(@RequestParam("code") String code) {
        return gateway.fetch(code.trim());
    }
}

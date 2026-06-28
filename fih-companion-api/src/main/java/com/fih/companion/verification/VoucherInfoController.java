package com.fih.companion.verification;

import com.fih.companion.verification.dto.VoucherInfoRequest;
import com.fih.companion.verification.dto.VoucherInfoResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/api/verify")
public class VoucherInfoController {

    private final VoucherVerificationGateway gateway;

    public VoucherInfoController(VoucherVerificationGateway gateway) {
        this.gateway = gateway;
    }

     @GetMapping("/voucher-info")
    public VoucherInfoResponse voucherInfo(@RequestParam("code") String code) {
        return gateway.fetch(new VoucherInfoRequest(code.trim()));
    }
}
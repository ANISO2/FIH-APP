package com.fih.companion.verification;

import com.fih.companion.verification.dto.VoucherInfoRequest;
import com.fih.companion.verification.dto.VoucherInfoResponse;
import org.springframework.stereotype.Component;

@Component
public class StubVoucherVerificationGateway implements VoucherVerificationGateway {

    @Override
    public VoucherInfoResponse fetch(VoucherInfoRequest request) {
        String code = request == null ? null : request.code();
        return VoucherInfoResponse.pendingIntegration(code);
    }
}

package com.fih.companion.verification;

import com.fih.companion.verification.dto.VoucherInfoRequest;
import com.fih.companion.verification.dto.VoucherInfoResponse;


public interface VoucherVerificationGateway {

    VoucherInfoResponse fetch(VoucherInfoRequest request);
}

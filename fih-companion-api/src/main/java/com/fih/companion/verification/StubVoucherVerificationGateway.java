package com.fih.companion.verification;

import com.fih.companion.verification.dto.VoucherInfoRequest;
import com.fih.companion.verification.dto.VoucherInfoResponse;
import org.springframework.stereotype.Component;

/**
 * Placeholder implementation of {@link VoucherVerificationGateway}.
 *
 * It exists only so the application starts and the endpoint returns a well-formed
 * response while the external service is not yet connected. It performs NO
 * verification and reaches NO database — it simply returns a PENDING_INTEGRATION
 * answer carrying the scanned code.
 *
 * When the real gateway is added, give it @Primary (or delete this class) so it
 * takes precedence.
 */
@Component
public class StubVoucherVerificationGateway implements VoucherVerificationGateway {

    @Override
    public VoucherInfoResponse fetch(VoucherInfoRequest request) {
        String code = request == null ? null : request.code();
        return VoucherInfoResponse.pendingIntegration(code);
    }
}

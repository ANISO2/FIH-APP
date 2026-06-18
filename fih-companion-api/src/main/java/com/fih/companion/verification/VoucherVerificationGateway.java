package com.fih.companion.verification;

import com.fih.companion.verification.dto.VoucherInfoRequest;
import com.fih.companion.verification.dto.VoucherInfoResponse;

/**
 * Integration boundary for Feature 1 (voucher info via the EXTERNAL web service).
 *
 * The actual voucher verification is owned by ANOTHER team's web service, so we
 * do NOT implement it here. This interface is the seam: the controller depends on
 * it, the mobile app is built against the response contract, and when the
 * external service is ready you provide a real implementation.
 *
 * TODO: wire external team web service — add a real @Component (e.g.
 *       HttpVoucherVerificationGateway) that calls their endpoint and maps the
 *       answer to VoucherInfoResponse, then mark it @Primary or remove the stub.
 */
public interface VoucherVerificationGateway {

    VoucherInfoResponse fetch(VoucherInfoRequest request);
}

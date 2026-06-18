package com.fih.companion.verification.dto;

/**
 * Input for the external voucher-info lookup (Feature 1). The mobile app sends a
 * scanned code (codebarre or numeroserie); the external team's web service owns
 * the actual verification.
 */
public record VoucherInfoRequest(String code) {
}

package com.fih.companion.verification.dto;

import com.fasterxml.jackson.annotation.JsonProperty;


/**
 * Body sent to POST {base-url}: {@code {"qrcode": "...", "product_id": <id>}}.
 * {@code product_id} is the programme id — mapped to our event reference.
 */
public record TicketVerifyRequest(
        String qrcode,
        @JsonProperty("product_id") Integer productId
) {
}

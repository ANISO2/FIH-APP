package com.fih.companion.stats.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Gate breakdown. "public" is a Java keyword, so the field is publicGate but it
 * is serialized to JSON as "public" via @JsonProperty.
 */
public record GateDto(
        @JsonProperty("public") GateBucketDto publicGate,
        GateBucketDto vip
) {
}

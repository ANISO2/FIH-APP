package com.fih.companion.stats.dto;

import com.fasterxml.jackson.annotation.JsonProperty;


public record GateDto(
        @JsonProperty("public") GateBucketDto publicGate,
        GateBucketDto vip
) {
}

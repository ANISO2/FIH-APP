package com.fih.companion.access;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Turns a modelebillet reference into human-readable access zones, using the
 * configured code map. Codes: P=Public, V=Vip, R=Press. This is the single
 * source of zone truth — the legacy "access" blob is never deserialized.
 */
@Component
public class AccessZoneResolver {

    private static final Map<String, String> LABELS = Map.of(
            "P", "Public",
            "V", "Vip",
            "R", "Press"
    );

    private final AccessZoneProperties properties;

    public AccessZoneResolver(AccessZoneProperties properties) {
        this.properties = properties;
    }

    public List<String> resolve(Integer modeleReference) {
        if (modeleReference == null) {
            return List.of();
        }
        List<String> codes = properties.getAccessZones().getOrDefault(modeleReference, List.of());
        return codes.stream()
                .map(code -> LABELS.getOrDefault(code.trim().toUpperCase(), code))
                .toList();
    }
}

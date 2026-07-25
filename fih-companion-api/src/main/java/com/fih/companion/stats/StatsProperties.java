package com.fih.companion.stats;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.LinkedHashSet;
import java.util.Set;


/**
 * App config under {@code fih.stats}. Currently holds the set of ticket models
 * treated as "Billet Gradins" for the tourniquet stats, where "Émis" must count
 * tickets actually SOLD (vendu) rather than merely generated.
 *
 * <p>(The {@code cache-ttl-seconds} property is still read separately via
 * {@code @Value} in {@link StatsService} and is intentionally left untouched.)
 */
@ConfigurationProperties(prefix = "fih.stats")
public class StatsProperties {

    /** Comma/semicolon/space separated list of Gradins model references. */
    private String gradinsModelIds = "22";

    private volatile Set<Integer> gradinsCache;

    public String getGradinsModelIds() {
        return gradinsModelIds;
    }

    public void setGradinsModelIds(String gradinsModelIds) {
        this.gradinsModelIds = gradinsModelIds;
        this.gradinsCache = null; // re-parse on next access
    }

    /** Parsed Gradins model set. Never empty — falls back to a harmless sentinel. */
    public Set<Integer> gradinsModelSet() {
        Set<Integer> cache = this.gradinsCache;
        if (cache == null) {
            cache = new LinkedHashSet<>();
            if (gradinsModelIds != null && !gradinsModelIds.isBlank()) {
                for (String token : gradinsModelIds.split("[,;\\s]+")) {
                    if (!token.isBlank()) {
                        try {
                            cache.add(Integer.valueOf(token.trim()));
                        } catch (NumberFormatException ignored) {
                            // skip non-numeric tokens
                        }
                    }
                }
            }
            if (cache.isEmpty()) {
                // Keep the SQL `NOT IN (:gradinsIds)` valid and inert (matches no
                // real model), so with no Gradins configured every model simply
                // keeps counting generated codes.
                cache.add(-1);
            }
            this.gradinsCache = cache;
        }
        return cache;
    }
}

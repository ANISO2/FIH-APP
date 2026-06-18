package com.fih.companion.badge;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

/**
 * Everything the PDF service needs to render one badge. Photo may be null.
 *
 * affecteeA is the name from the app-owned badge_affectation table (looked up by
 * numeroserie). It is what we print on the badge when present; holderName (from
 * the legacy holder table) is kept as a fallback so existing behaviour is
 * preserved when no name has been assigned yet.
 */
public record BadgeRecord(
        String type, String numeroserie, String codebarre, String holderName, String affecteeA,
        String eventTitle, LocalDate eventDate, String modelName,
        List<String> zones, Path photo
) {
}

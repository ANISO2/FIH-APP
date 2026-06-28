package com.fih.companion.badge;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;


public record BadgeRecord(
        String type, String numeroserie, String codebarre, String holderName, String affecteeA,
        String eventTitle, LocalDate eventDate, String modelName,
        List<String> zones, Path photo
) {
}

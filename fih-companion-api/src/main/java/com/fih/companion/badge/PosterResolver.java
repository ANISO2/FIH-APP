package com.fih.companion.badge;

import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.Normalizer;
import java.util.Locale;
import java.util.Optional;

/**
 * Backoffice poster-coverage check for an event.
 *
 * It mirrors EXACTLY how BadgePdfService finds a poster — slug(eventTitle).{ext}
 * in the configured poster dir — so the "Affiche présente ?" indicator in the UI
 * agrees with what actually prints. It deliberately does NOT count the default
 * poster (poster-default.jpg): the indicator is about whether THIS event has its
 * own artwork, while the default is only a print-time fallback. Reads files only,
 * never the DB.
 */
@Component
public class PosterResolver {

    private final BadgeProperties props;

    public PosterResolver(BadgeProperties props) {
        this.props = props;
    }

    /** The event-specific poster file (slug(title).ext), if present. Ignores the default. */
    public Optional<Path> resolve(String eventTitle) {
        String dir = props.getPosterDir();
        if (dir == null || dir.isBlank() || eventTitle == null || eventTitle.isBlank()) {
            return Optional.empty();
        }
        Path base = Paths.get(dir);
        String slug = slugify(eventTitle);
        for (String ext : props.getPosterExtensions()) {
            Path p = base.resolve(slug + "." + ext);
            if (Files.isReadable(p)) {
                return Optional.of(p);
            }
        }
        return Optional.empty();
    }

    /** True when the event has its own poster artwork on disk. */
    public boolean exists(String eventTitle) {
        return resolve(eventTitle).isPresent();
    }

    /** "Salif Keïta" -> "salif-keita" (accent-folded, lower-case, dash-joined). Same as BadgePdfService. */
    private String slugify(String s) {
        return Normalizer.normalize(s, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-+|-+$)", "");
    }
}

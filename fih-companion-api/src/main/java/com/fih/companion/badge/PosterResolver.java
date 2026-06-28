package com.fih.companion.badge;

import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.Normalizer;
import java.util.Locale;
import java.util.Optional;


@Component
public class PosterResolver {

    private final BadgeProperties props;

    public PosterResolver(BadgeProperties props) {
        this.props = props;
    }

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
     public boolean exists(String eventTitle) {
        return resolve(eventTitle).isPresent();
    }

     private String slugify(String s) {
        return Normalizer.normalize(s, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-+|-+$)", "");
    }
}

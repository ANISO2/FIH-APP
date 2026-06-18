package com.fih.companion.badge;

import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Finds a photo on the local disk for a record. Reads files only — never the DB.
 * Tries ${photo-dir}/${photo-key}.${ext} for each configured extension, then
 * falls back to the numeroserie as the filename.
 */
@Component
public class PhotoResolver {

    private final BadgeProperties props;

    public PhotoResolver(BadgeProperties props) {
        this.props = props;
    }

    public Optional<Path> resolve(String codebarre, String numeroserie) {
        for (String key : keyOrder(codebarre, numeroserie)) {
            if (key == null || key.isBlank()) continue;
            for (String ext : props.getPhotoExtensions()) {
                Path candidate = Path.of(props.getPhotoDir(), key + "." + ext);
                if (Files.isRegularFile(candidate)) {
                    return Optional.of(candidate);
                }
            }
        }
        return Optional.empty();
    }

    public boolean exists(String codebarre, String numeroserie) {
        return resolve(codebarre, numeroserie).isPresent();
    }

    private List<String> keyOrder(String codebarre, String numeroserie) {
        List<String> order = new ArrayList<>();
        if ("numeroserie".equalsIgnoreCase(props.getPhotoKey())) {
            order.add(numeroserie);
            order.add(codebarre);
        } else {
            order.add(codebarre);
            order.add(numeroserie);
        }
        return order;
    }
}

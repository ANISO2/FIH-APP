package com.fih.companion.badge;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Binds fih.badge.* from application.yml. */
@ConfigurationProperties(prefix = "fih.badge")
public class BadgeProperties {

    /** Folder holding the photos, named by the photo-key field. */
    private String photoDir = "D:/Bitaka/bitaka/fih-companion/photos";
    /** Which record field is used as the filename: "codebarre" (fallback numeroserie). */
    private String photoKey = "codebarre";
    private List<String> photoExtensions = List.of("jpg", "jpeg", "png");

    /** Default badge size in millimetres (portrait lanyard badge). */
    private double widthMm = 100;
    private double heightMm = 150;
    /** Above this many badges, /batch returns a ZIP of individual PDFs instead of one big PDF. */
    private int zipThreshold = 150;

    /**
     * modelebillet.reference values that are "invitation" models, as a COMMA-
     * SEPARATED STRING. PDF/badge generation is restricted to THESE models only:
     * the availability list, the item lists, the photo-check and every
     * generation endpoint refuse anything not in this set. Edit in
     * application.yml with no recompile. Seeded with the Invitation family
     * (3, 36, 38, 39, 40, 41).
     *
     * NOTE: this is a scalar String, NOT a List. Binding a List directly from a
     * placeholder default that contains commas (e.g.
     * {@code ${FIH_INVITATION_MODELS:3,36,38,39,40,41}}) breaks, because the
     * collection binder splits on commas BEFORE resolving the placeholder and
     * the last fragment keeps a stray "}". A scalar String resolves the
     * placeholder first, then we parse it ourselves below.
     */
    private String invitationModels = "3,36,38,39,40,41";

    /** Parsed + cached view of {@link #invitationModels}. */
    private Set<Integer> invitationModelCache;

    public String getPhotoDir() { return photoDir; }
    public void setPhotoDir(String photoDir) { this.photoDir = photoDir; }
    public String getPhotoKey() { return photoKey; }
    public void setPhotoKey(String photoKey) { this.photoKey = photoKey; }
    public List<String> getPhotoExtensions() { return photoExtensions; }
    public void setPhotoExtensions(List<String> photoExtensions) { this.photoExtensions = photoExtensions; }
    public double getWidthMm() { return widthMm; }
    public void setWidthMm(double widthMm) { this.widthMm = widthMm; }
    public double getHeightMm() { return heightMm; }
    public void setHeightMm(double heightMm) { this.heightMm = heightMm; }
    public int getZipThreshold() { return zipThreshold; }
    public void setZipThreshold(int zipThreshold) { this.zipThreshold = zipThreshold; }

    public String getInvitationModels() { return invitationModels; }
    public void setInvitationModels(String invitationModels) {
        this.invitationModels = invitationModels;
        this.invitationModelCache = null; // re-parse on next access
    }

    /** True when the given modelebillet.reference is an allowed invitation model. */
    public boolean isInvitationModel(Integer modelId) {
        return modelId != null && invitationModelSet().contains(modelId);
    }

    /** Parsed set of allowed invitation model references (ignores blanks/garbage). */
    public Set<Integer> invitationModelSet() {
        Set<Integer> cache = this.invitationModelCache;
        if (cache == null) {
            cache = new LinkedHashSet<>();
            if (invitationModels != null && !invitationModels.isBlank()) {
                for (String token : invitationModels.split("[,;\\s]+")) {
                    String t = token.trim();
                    if (t.isEmpty()) continue;
                    try {
                        cache.add(Integer.valueOf(t));
                    } catch (NumberFormatException ignored) {
                        // Skip anything that isn't an integer rather than failing startup.
                    }
                }
            }
            this.invitationModelCache = cache;
        }
        return cache;
    }

    /** Allowed invitation model references as an ordered List (convenience). */
    public List<Integer> invitationModelList() {
        return Arrays.asList(invitationModelSet().toArray(new Integer[0]));
    }
}

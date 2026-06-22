package com.fih.companion.badge;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Binds fih.badge.* from application.yml. */
@ConfigurationProperties(prefix = "fih.badge")
public class BadgeProperties {

    /** Folder holding the photos, named by the photo-key field. (Legacy; kept for binding.) */
    private String photoDir = "D:/Bitaka/bitaka/fih-companion/photos";
    /** Which record field is used as the filename: "codebarre" (fallback numeroserie). */
    private String photoKey = "codebarre";
    private List<String> photoExtensions = List.of("jpg", "jpeg", "png");

    /** Legacy portrait badge size in millimetres. (Kept for binding; e-ticket uses ticket-*-mm.) */
    private double widthMm = 100;
    private double heightMm = 150;
    /** Above this many badges, /batch returns a ZIP of individual PDFs instead of one big PDF. */
    private int zipThreshold = 150;

    // ===================== e-ticket layout =====================
    /** Landscape e-ticket page size in millimetres (matches the_format.jpeg, ratio 2.2). */
    private double ticketWidthMm = 220;
    private double ticketHeightMm = 100;

    /**
     * Event posters. Resolved per-ticket as slug(eventTitle).{ext}, e.g.
     * "Salif Keita" -> salif-keita.jpg, falling back to poster-default.
     */
    private String posterDir = "D:/Bitaka/bitaka/fih-companion/posters";
    private String posterDefault = "poster-default.jpg";
    private List<String> posterExtensions = List.of("jpg", "jpeg", "png");

    /**
     * Show time. The evenement table has no time column (only ddate), so the time
     * printed on the ticket is config-driven: a default plus an optional per-date
     * override map keyed by ddate in yyyy-MM-dd form. Edit without recompiling.
     */
    private String showTime = "22:00";
    private Map<String, String> showTimes = new LinkedHashMap<>();
    // ===========================================================

    /**
     * modelebillet.reference values that are "invitation" models, as a COMMA-
     * SEPARATED STRING. PDF/badge generation is restricted to THESE models only.
     * Scalar String (not List) so a placeholder default with commas resolves
     * before we parse it ourselves. Seeded with the Invitation family.
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

    public double getTicketWidthMm() { return ticketWidthMm; }
    public void setTicketWidthMm(double ticketWidthMm) { this.ticketWidthMm = ticketWidthMm; }
    public double getTicketHeightMm() { return ticketHeightMm; }
    public void setTicketHeightMm(double ticketHeightMm) { this.ticketHeightMm = ticketHeightMm; }

    public String getPosterDir() { return posterDir; }
    public void setPosterDir(String posterDir) { this.posterDir = posterDir; }
    public String getPosterDefault() { return posterDefault; }
    public void setPosterDefault(String posterDefault) { this.posterDefault = posterDefault; }
    public List<String> getPosterExtensions() { return posterExtensions; }
    public void setPosterExtensions(List<String> posterExtensions) { this.posterExtensions = posterExtensions; }

    public String getShowTime() { return showTime; }
    public void setShowTime(String showTime) { this.showTime = showTime; }
    public Map<String, String> getShowTimes() { return showTimes; }
    public void setShowTimes(Map<String, String> showTimes) { this.showTimes = showTimes; }

    /** Time to print for an event date: the per-date override if present, else the default. */
    public String resolveShowTime(LocalDate date) {
        if (date != null && showTimes != null) {
            String v = showTimes.get(date.toString()); // yyyy-MM-dd
            if (v != null && !v.isBlank()) return v;
        }
        return showTime;
    }

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

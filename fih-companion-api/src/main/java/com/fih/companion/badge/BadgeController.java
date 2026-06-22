package com.fih.companion.badge;

import com.fih.companion.badge.dto.AvailabilityDto;
import com.fih.companion.badge.dto.BadgeItemDto;
import com.fih.companion.badge.dto.BatchRequest;
import com.fih.companion.badge.dto.MissingPosterDto;
import com.fih.companion.badge.dto.PageDto;
import com.fih.companion.invitation.AffecteeService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;

/**
 * Badge endpoints. All under /api/badges/** which SecurityConfig locks to the
 * admin JWT. PDFs/ZIPs are returned as byte[] with a Content-Disposition header
 * so the browser downloads them with a sensible filename.
 *
 * GENERATION GUARD (Change D) — a PDF can only be produced for an invitation that
 * has been affected to a name. {@link #single} returns 422 for an unaffected
 * serial; {@link #batch} prints the affected ones and reports the skipped serials
 * back in the X-Skipped-* response headers (or 422 if NONE are affected).
 *
 * §6 — after a PDF is produced we stamp printed_at on the serials involved (via
 * {@link AffecteeService#markPrinted}). Only serials that already have a name are
 * stamped; printing never creates a name, so the one-time rule is untouched.
 */
@RestController
@RequestMapping("/api/badges")
public class BadgeController {

    private final BadgeQueryService query;
    private final BadgePdfService pdf;
    private final AffecteeService affectee;

    public BadgeController(BadgeQueryService query, BadgePdfService pdf, AffecteeService affectee) {
        this.query = query;
        this.pdf = pdf;
        this.affectee = affectee;
    }

    @GetMapping("/availability")
    public List<AvailabilityDto> availability(@RequestParam(required = false) Integer eventId) {
        return query.availability(eventId);
    }

    /** §6 — events that have invitations but no poster file yet. */
    @GetMapping("/posters/missing")
    public List<MissingPosterDto> missingPosters() {
        return query.missingPosters();
    }

    @GetMapping("/items")
    public PageDto<BadgeItemDto> items(@RequestParam int eventId,
                                       @RequestParam int modelId,
                                       @RequestParam(defaultValue = "0") int page,
                                       @RequestParam(defaultValue = "25") int size,
                                       @RequestParam(required = false) String search) {
        return query.items(eventId, modelId, page, size, search);
    }

    @GetMapping("/single")
    public ResponseEntity<byte[]> single(@RequestParam String type, @RequestParam String code) {
        BadgeRecord rec = query.single(type, code);

        // Change D — block a single PDF for an invitation that has no name yet.
        if (isUnaffected(rec)) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Cette invitation doit d'abord \u00eatre affect\u00e9e \u00e0 un nom avant de g\u00e9n\u00e9rer le PDF.");
        }

        byte[] body = pdf.single(rec);
        affectee.markPrinted(List.of(rec.numeroserie()));
        return pdfResponse(body, "badge_" + sanitize(rec.codebarre()) + ".pdf",
                MediaType.APPLICATION_PDF, null);
    }

    @PostMapping("/batch")
    public ResponseEntity<byte[]> batch(@RequestBody BatchRequest req) {
        if (req.eventId() == null || req.modelId() == null) {
            return ResponseEntity.badRequest().build();
        }
        List<BadgeRecord> all = query.batch(req.eventId(), req.modelId(), req.codes());

        // Change D — print only the affected invitations; remember the rest so the
        // UI can tell the admin which serials still need a name.
        List<BadgeRecord> printable = new ArrayList<>();
        List<String> skipped = new ArrayList<>();
        for (BadgeRecord r : all) {
            if (isUnaffected(r)) skipped.add(r.numeroserie());
            else printable.add(r);
        }
        if (printable.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Aucune invitation affect\u00e9e dans la s\u00e9lection. "
                            + "Affectez un nom avant de g\u00e9n\u00e9rer le PDF.");
        }

        String base = "badges_" + sanitize(printable.get(0).eventTitle())
                + "_" + sanitize(printable.get(0).modelName());

        ResponseEntity<byte[]> response;
        if (printable.size() > pdf.zipThreshold()) {
            response = pdfResponse(pdf.batchZip(printable), base + ".zip",
                    MediaType.parseMediaType("application/zip"), skipped);
        } else {
            response = pdfResponse(pdf.batchSingle(printable), base + ".pdf",
                    MediaType.APPLICATION_PDF, skipped);
        }
        affectee.markPrinted(printable.stream().map(BadgeRecord::numeroserie).toList());
        return response;
    }

    /** True when the invitation has no "Affectée à" name (Change D). */
    private boolean isUnaffected(BadgeRecord rec) {
        return rec.affecteeA() == null || rec.affecteeA().isBlank();
    }

    /**
     * Build the download response. When {@code skipped} is non-empty (batch only)
     * we add X-Skipped-Count and X-Skipped-Serials so the front-end can show which
     * unaffected invitations were left out. The list is capped to keep the header
     * small.
     */
    private ResponseEntity<byte[]> pdfResponse(byte[] body, String filename, MediaType type, List<String> skipped) {
        ResponseEntity.BodyBuilder b = ResponseEntity.ok()
                .contentType(type)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"");
        if (skipped != null && !skipped.isEmpty()) {
            List<String> sample = skipped.size() > 50 ? skipped.subList(0, 50) : skipped;
            b.header("X-Skipped-Count", String.valueOf(skipped.size()));
            b.header("X-Skipped-Serials", String.join(",", sample));
        }
        return b.body(body);
    }

    private String sanitize(String s) {
        if (s == null || s.isBlank()) return "badge";
        return s.replaceAll("[^a-zA-Z0-9._-]+", "_").replaceAll("_+", "_");
    }
}

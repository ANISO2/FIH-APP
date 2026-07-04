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

import java.util.List;


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

    @GetMapping("/posters/missing")
    public List<MissingPosterDto> missingPosters() {
        return query.missingPosters();
    }

    @GetMapping("/items")
    public PageDto<BadgeItemDto> items(@RequestParam int eventId,
                                       @RequestParam int modelId,
                                       @RequestParam(defaultValue = "0") int page,
                                       @RequestParam(defaultValue = "25") int size,
                                       @RequestParam(required = false) String search,
                                       // Feature 2 — default shows only NOT-yet-affected entries.
                                       @RequestParam(defaultValue = "pending") String status) {
        return query.items(eventId, modelId, page, size, search, status);
    }

    @GetMapping("/counts")
    public com.fih.companion.badge.dto.CountsDto counts(@RequestParam int eventId,
                                                        @RequestParam int modelId) {
        return query.counts(eventId, modelId);
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
         // invitation (each keeping the existing single-ticket design), bundled
        // into a single ZIP. Every PDF is named after that invitation's
        // "Affect\u00e9 \u00e0" value; invitations without a name are NOT skipped —
        // they fall back to a clearly-marked "SANS-NOM_<code>" file name.
        List<BadgeRecord> all = query.batch(req.eventId(), req.modelId(), req.codes());
        // query.batch(...) already throws 404 when the selection resolves to no
        // records, so `all` is non-empty here.

        String base = "badges_" + sanitize(all.get(0).eventTitle())
                + "_" + sanitize(all.get(0).modelName());

        byte[] zip = pdf.batchZipPerAffectee(all);
        // Only affected invitations own a badge_affectation row, so markPrinted
        // stamps printed_at for those; unaffected serials are a no-op update.
        affectee.markPrinted(all.stream().map(BadgeRecord::numeroserie).toList());
        return pdfResponse(zip, base + ".zip", MediaType.parseMediaType("application/zip"), null);
    }

    private boolean isUnaffected(BadgeRecord rec) {
        return rec.affecteeA() == null || rec.affecteeA().isBlank();
    }

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

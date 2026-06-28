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

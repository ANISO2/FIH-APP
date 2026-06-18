package com.fih.companion.badge;

import com.fih.companion.badge.dto.AvailabilityDto;
import com.fih.companion.badge.dto.BadgeItemDto;
import com.fih.companion.badge.dto.BatchRequest;
import com.fih.companion.badge.dto.PageDto;
import com.fih.companion.badge.dto.PhotoCheckDto;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Badge endpoints. All under /api/badges/** which SecurityConfig locks to the
 * admin JWT. PDFs/ZIPs are returned as byte[] with a Content-Disposition header
 * so the browser downloads them with a sensible filename.
 */
@RestController
@RequestMapping("/api/badges")
public class BadgeController {

    private final BadgeQueryService query;
    private final BadgePdfService pdf;

    public BadgeController(BadgeQueryService query, BadgePdfService pdf) {
        this.query = query;
        this.pdf = pdf;
    }

    @GetMapping("/availability")
    public List<AvailabilityDto> availability(@RequestParam(required = false) Integer eventId,
                                              @RequestParam(defaultValue = "false") boolean withPhotoCheck) {
        return query.availability(eventId, withPhotoCheck);
    }

    @GetMapping("/items")
    public PageDto<BadgeItemDto> items(@RequestParam int eventId,
                                       @RequestParam int modelId,
                                       @RequestParam(defaultValue = "0") int page,
                                       @RequestParam(defaultValue = "25") int size,
                                       @RequestParam(required = false) String search) {
        return query.items(eventId, modelId, page, size, search);
    }

    @GetMapping("/photo-check")
    public PhotoCheckDto photoCheck(@RequestParam int eventId, @RequestParam int modelId) {
        return query.photoCheck(eventId, modelId);
    }

    @GetMapping("/single")
    public ResponseEntity<byte[]> single(@RequestParam String type, @RequestParam String code) {
        BadgeRecord rec = query.single(type, code);
        byte[] body = pdf.single(rec);
        return pdfResponse(body, "badge_" + sanitize(rec.codebarre()) + ".pdf", MediaType.APPLICATION_PDF);
    }

    @PostMapping("/batch")
    public ResponseEntity<byte[]> batch(@RequestBody BatchRequest req,
                                        @RequestParam(defaultValue = "single") String layout) {
        if (req.eventId() == null || req.modelId() == null) {
            return ResponseEntity.badRequest().build();
        }
        List<BadgeRecord> recs = query.batch(req.eventId(), req.modelId(), req.codes());
        String base = "badges_" + sanitize(recs.get(0).eventTitle()) + "_" + sanitize(recs.get(0).modelName());

        if ("sheet".equalsIgnoreCase(layout)) {
            return pdfResponse(pdf.batchSheet(recs), base + "_sheet.pdf", MediaType.APPLICATION_PDF);
        }
        if (recs.size() > pdf.zipThreshold()) {
            return pdfResponse(pdf.batchZip(recs), base + ".zip",
                    MediaType.parseMediaType("application/zip"));
        }
        return pdfResponse(pdf.batchSingle(recs), base + ".pdf", MediaType.APPLICATION_PDF);
    }

    private ResponseEntity<byte[]> pdfResponse(byte[] body, String filename, MediaType type) {
        return ResponseEntity.ok()
                .contentType(type)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .body(body);
    }

    private String sanitize(String s) {
        if (s == null || s.isBlank()) return "badge";
        return s.replaceAll("[^a-zA-Z0-9._-]+", "_").replaceAll("_+", "_");
    }
}

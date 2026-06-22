package com.fih.companion.invitation;

import com.fih.companion.invitation.dto.AffecteeDto;
import com.fih.companion.invitation.dto.AffecteeRequest;
import com.fih.companion.invitation.dto.LotPreviewDto;
import com.fih.companion.invitation.dto.LotRequest;
import com.fih.companion.invitation.dto.LotResultDto;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.Principal;

/**
 * Admin-only endpoints for the invitation badge name ("Affectée à").
 *
 * Locked to ROLE_ADMIN by SecurityConfig (/api/invitations/**). These are the
 * only write endpoints in the application; they write to the app-owned
 * badge_affectation table via {@link AffecteeService} and never to a legacy
 * table.
 */
@RestController
@RequestMapping("/api/invitations")
public class InvitationController {

    private final AffecteeService service;

    public InvitationController(AffecteeService service) {
        this.service = service;
    }

    /** Current name for a serial. 404 if no name has been set yet. */
    @GetMapping("/{numeroserie}/affectee")
    public AffecteeDto get(@PathVariable String numeroserie) {
        return service.get(numeroserie)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Aucun nom enregistré pour ce billet."));
    }

    /**
     * Assign the name ONCE (Change B). The admin username comes from the JWT:
     * Spring injects the authenticated user as {@link Principal}, and
     * principal.getName() is the token subject (the username). If the serial is
     * already assigned the service returns 409 Conflict.
     */
    @PutMapping("/{numeroserie}/affectee")
    public AffecteeDto set(@PathVariable String numeroserie,
                           @Valid @RequestBody AffecteeRequest request,
                           Principal principal) {
        String updatedBy = principal == null ? null : principal.getName();
        return service.set(numeroserie, request.name(), updatedBy);
    }

    // ---------------------------------------------------------------- lot (C)

    /**
     * Dry-run a lot before assigning: returns the matched invitations, the names
     * they would receive (baseName-01 …), and any conflicts/warnings. Read-only.
     */
    @PostMapping("/affectation/lot/preview")
    public LotPreviewDto previewLot(@Valid @RequestBody LotRequest request) {
        return service.previewLot(request);
    }

    /**
     * Assign a whole lot immutably. Returns 409 Conflict (with the list) if ANY
     * serial in the range is already assigned, so the lot is all-or-nothing.
     */
    @PostMapping("/affectation/lot")
    public LotResultDto assignLot(@Valid @RequestBody LotRequest request, Principal principal) {
        String updatedBy = principal == null ? null : principal.getName();
        return service.assignLot(request, updatedBy);
    }

    /** CSV manifest (nom, numeroserie, codebarre, evenement) for an assigned range. */
    @GetMapping("/affectation/lot/manifest")
    public ResponseEntity<byte[]> manifest(@RequestParam String startSerie,
                                           @RequestParam String endSerie) {
        byte[] body = service.manifestCsv(startSerie, endSerie).getBytes(StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"manifest_lot.csv\"")
                .body(body);
    }
}

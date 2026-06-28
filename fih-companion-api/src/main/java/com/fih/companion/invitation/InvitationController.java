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


@RestController
@RequestMapping("/api/invitations")
public class InvitationController {

    private final AffecteeService service;

    public InvitationController(AffecteeService service) {
        this.service = service;
    }

     @GetMapping("/{numeroserie}/affectee")
    public AffecteeDto get(@PathVariable String numeroserie) {
        return service.get(numeroserie)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Aucun nom enregistré pour ce billet."));
    }


    @PutMapping("/{numeroserie}/affectee")
    public AffecteeDto set(@PathVariable String numeroserie,
                           @Valid @RequestBody AffecteeRequest request,
                           Principal principal) {
        String updatedBy = principal == null ? null : principal.getName();
        return service.set(numeroserie, request.name(), updatedBy);
    }


    @PostMapping("/affectation/lot/preview")
    public LotPreviewDto previewLot(@Valid @RequestBody LotRequest request) {
        return service.previewLot(request);
    }


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

package com.fih.companion.invitation;

import com.fih.companion.invitation.dto.AffecteeDto;
import com.fih.companion.invitation.dto.AffecteeRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

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
     * Set or update the name. The admin username comes from the JWT: Spring
     * injects the authenticated user as {@link Principal}, and principal.getName()
     * is the token subject (the username) set in JwtAuthFilter.
     */
    @PutMapping("/{numeroserie}/affectee")
    public AffecteeDto set(@PathVariable String numeroserie,
                           @Valid @RequestBody AffecteeRequest request,
                           Principal principal) {
        String updatedBy = principal == null ? null : principal.getName();
        return service.set(numeroserie, request.name(), updatedBy);
    }
}

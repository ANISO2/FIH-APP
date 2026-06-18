package com.fih.companion.invitation;

import com.fih.companion.badge.BadgeProperties;
import com.fih.companion.domain.BadgeAffectation;
import com.fih.companion.domain.Billet;
import com.fih.companion.invitation.dto.AffecteeDto;
import com.fih.companion.repository.BadgeAffectationRepository;
import com.fih.companion.repository.BilletRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Reads and upserts the "Affectée à" name for an invitation billet.
 *
 * This is the ONLY service in the app that writes. It writes a single row in the
 * app-owned badge_affectation table and never touches a legacy table.
 *
 * Before writing, it validates that the serial really is an INVITATION-model
 * billet, using the same configurable set as badge generation
 * (fih.badge.invitation-models). That keeps the name feature scoped to
 * invitations and rejects, e.g., a Billet Gradins serial. The set is config, not
 * hardcoded data, so new invitation models in future editions are handled by
 * editing application.yml — no code change.
 */
@Service
public class AffecteeService {

    private final BadgeAffectationRepository affectationRepository;
    private final BilletRepository billetRepository;
    private final BadgeProperties badgeProperties;

    public AffecteeService(BadgeAffectationRepository affectationRepository,
                           BilletRepository billetRepository,
                           BadgeProperties badgeProperties) {
        this.affectationRepository = affectationRepository;
        this.billetRepository = billetRepository;
        this.badgeProperties = badgeProperties;
    }

    /** Current name for a serial, if one has been set. Read-only. */
    @Transactional(readOnly = true)
    public Optional<AffecteeDto> get(String numeroserie) {
        return affectationRepository.findById(numeroserie).map(this::toDto);
    }

    /**
     * Set (or update) the name for an invitation serial. Upsert: when the serial
     * is new we INSERT, when it already exists we UPDATE — because the primary
     * key is the serial itself, a single save() does both.
     */
    @Transactional
    public AffecteeDto set(String numeroserie, String name, String updatedBy) {
        Billet billet = billetRepository.findByNumeroserie(numeroserie)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Aucun billet trouvé pour le numéro de série : " + numeroserie));

        if (!badgeProperties.isInvitationModel(billet.getModelebillet())) {
            // The billet exists but is not an invitation, so the request is
            // unprocessable (422) rather than "not found".
            throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "Le numéro de série ne correspond pas à un billet d'invitation.");
        }

        String clean = name == null ? "" : name.trim();
        if (clean.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le nom est obligatoire.");
        }

        BadgeAffectation entity = affectationRepository.findById(numeroserie).orElse(null);
        if (entity == null) {
            entity = new BadgeAffectation(numeroserie, clean, updatedBy);
        } else {
            entity.setAffecteeA(clean);
            entity.setUpdatedBy(updatedBy);
            entity.setUpdatedAt(LocalDateTime.now());
        }
        return toDto(affectationRepository.save(entity));
    }

    private AffecteeDto toDto(BadgeAffectation e) {
        return new AffecteeDto(e.getNumeroserie(), e.getAffecteeA(), e.getUpdatedAt(), e.getUpdatedBy());
    }
}

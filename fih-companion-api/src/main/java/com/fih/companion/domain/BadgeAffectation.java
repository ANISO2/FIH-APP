package com.fih.companion.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * The "Affectée à" name printed on an invitation badge.
 *
 * THIS IS THE ONLY MUTABLE ENTITY IN THE APPLICATION. Every other entity maps a
 * legacy table and is @Immutable / read-only. This one maps a brand-new table
 * (badge_affectation) that the companion app owns, so it is safe to write here —
 * and nowhere else.
 *
 * The primary key is the invitation billet's serial (numeroserie). There is NO
 * foreign key to billet, on purpose: a FK would couple this table to the legacy
 * billet table (locks, cascade rules, the other team's sign-off). The link is
 * purely logical — we store the same serial and join on it only when we need the
 * name. Because the id is the serial itself, JpaRepository.save() naturally
 * "upserts": it INSERTs a new serial and UPDATEs an existing one.
 */
@Entity
@Table(name = "badge_affectation")
@Getter
@Setter
public class BadgeAffectation {

    /** Invitation billet serial. Assigned by us (not generated). */
    @Id
    @Column(name = "numeroserie", nullable = false, length = 255)
    private String numeroserie;

    /** The name to print on the badge. */
    @Column(name = "affectee_a", nullable = false, length = 255)
    private String affecteeA;

    /** When the name was last set/changed. We set this explicitly on every write. */
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /** Which admin set it (the JWT username). */
    @Column(name = "updated_by", length = 255)
    private String updatedBy;

    /** Required by JPA. */
    protected BadgeAffectation() {
    }

    public BadgeAffectation(String numeroserie, String affecteeA, String updatedBy) {
        this.numeroserie = numeroserie;
        this.affecteeA = affecteeA;
        this.updatedBy = updatedBy;
        this.updatedAt = LocalDateTime.now();
    }
}

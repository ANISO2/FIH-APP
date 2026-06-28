package com.fih.companion.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;


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

    /** §6 — when a PDF for this serial was last generated (null = never printed). */
    @Column(name = "printed_at")
    private LocalDateTime printedAt;

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

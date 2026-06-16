package com.fih.companion.evenement;

import com.fih.companion.domain.Location;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import org.hibernate.annotations.Immutable;

import java.time.LocalDate;

/**
 * Maps the existing "evenement" table EXACTLY (34 rows). We do not own this
 * table, so every field is marked read-only and the whole entity is @Immutable.
 *
 * Annotations explained:
 *  - @Entity / @Table(name="evenement") : this Java class corresponds to that
 *    existing table; French names are kept as-is, no renaming.
 *  - @Immutable (Hibernate) : tells Hibernate this entity is never modified, so
 *    it never generates UPDATE statements and can optimize reads. Any attempt to
 *    change a loaded instance is silently ignored at flush time.
 *  - @Column(insertable = false, updatable = false) : even if some code tried to
 *    persist or merge this entity, Hibernate would exclude these columns from
 *    INSERT/UPDATE. Combined with @Immutable, the entity is purely a read view.
 *
 * The hard guarantee is still the fih_ro database role; these annotations are
 * the application-layer expression of the same "read-only" intent.
 */
@Entity
@Table(name = "evenement")
@Immutable
@Getter
public class Evenement {

    @Id
    @Column(name = "reference", insertable = false, updatable = false)
    private Integer reference;

    @Column(name = "titre", insertable = false, updatable = false)
    private String titre;

    @Column(name = "ddate", insertable = false, updatable = false)
    private LocalDate ddate;

    @Column(name = "billet", insertable = false, updatable = false)
    private boolean billet;

    @Column(name = "voucher", insertable = false, updatable = false)
    private boolean voucher;

    /**
     * Venue. Now modeled as a proper read-only @ManyToOne to Location
     * (Phase 1 had this as a plain Integer). The join column stays read-only.
     */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "location", insertable = false, updatable = false)
    private Location location;

    /** JPA requires a no-arg constructor; it stays protected so app code does not use it. */
    protected Evenement() {
    }
}

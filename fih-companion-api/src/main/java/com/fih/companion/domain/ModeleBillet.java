package com.fih.companion.domain;

import jakarta.persistence.*;
import lombok.Getter;
import org.hibernate.annotations.Immutable;

/**
 * Ticket type. Read-only mapping of "modelebillet".
 *
 * NOTE on "access": it is a serialized Java ArrayList<String> stored as bytea.
 * We map it as an opaque byte[] and NEVER deserialize it here. Access-zone logic
 * lives in AccessZoneResolver, driven by configuration, so it is plain Java we
 * control and can query.
 */
@Entity
@Table(name = "modelebillet")
@Immutable
@Getter
public class ModeleBillet {

    @Id
    @Column(name = "reference", insertable = false, updatable = false)
    private Integer reference;

    /** Opaque legacy blob — do not deserialize. */
    @Column(name = "access", insertable = false, updatable = false)
    private byte[] access;

    @Column(name = "modele", insertable = false, updatable = false)
    private String modele;

    @Column(name = "maxaccess", insertable = false, updatable = false)
    private int maxaccess;

    @Column(name = "billet", insertable = false, updatable = false)
    private boolean billet;

    @Column(name = "voucher", insertable = false, updatable = false)
    private boolean voucher;

    @Column(name = "kit", insertable = false, updatable = false)
    private boolean kit;

    @Column(name = "vente", insertable = false, updatable = false)
    private boolean vente;

    @Column(name = "ventekit", insertable = false, updatable = false)
    private boolean ventekit;

    @Column(name = "utilitaire", insertable = false, updatable = false)
    private boolean utilitaire;

    protected ModeleBillet() {
    }
}

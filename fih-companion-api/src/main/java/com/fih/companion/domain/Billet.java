package com.fih.companion.domain;

import jakarta.persistence.*;
import lombok.Getter;
import org.hibernate.annotations.Immutable;


@Entity
@Table(name = "billet")
@Immutable
@Getter
public class Billet {

    @Id
    @Column(name = "numeroserie", insertable = false, updatable = false)
    private String numeroserie;

    @Column(name = "codebarre", insertable = false, updatable = false)
    private String codebarre;

    @Column(name = "activation", insertable = false, updatable = false)
    private boolean activation;

    @Column(name = "reservation", insertable = false, updatable = false)
    private boolean reservation;

    @Column(name = "utilisation", insertable = false, updatable = false)
    private boolean utilisation;

    @Column(name = "vendu", insertable = false, updatable = false)
    private boolean vendu;

    @Column(name = "etatlivraison", insertable = false, updatable = false)
    private boolean etatlivraison;

    @Column(name = "nombreacces", insertable = false, updatable = false)
    private int nombreacces;

    @Column(name = "evenement", insertable = false, updatable = false)
    private Integer evenement;

    @Column(name = "modelebillet", insertable = false, updatable = false)
    private Integer modelebillet;

    @Column(name = "livraison", insertable = false, updatable = false)
    private Integer livraison;

    @Column(name = "vente", insertable = false, updatable = false)
    private Integer vente;

    @Column(name = "kit", insertable = false, updatable = false)
    private Integer kit;

    protected Billet() {
    }
}

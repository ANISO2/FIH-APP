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


    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "location", insertable = false, updatable = false)
    private Location location;

     protected Evenement() {
    }
}

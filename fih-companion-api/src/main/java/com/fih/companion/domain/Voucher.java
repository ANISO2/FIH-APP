package com.fih.companion.domain;

import jakarta.persistence.*;
import lombok.Getter;
import org.hibernate.annotations.Immutable;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;


@Entity
@Table(name = "voucher")
@Immutable
@Getter
public class Voucher {

    @Id
    @Column(name = "numeroserie", insertable = false, updatable = false)
    private String numeroserie;

    @Column(name = "codebarre", insertable = false, updatable = false)
    private String codebarre;

    @Column(name = "activation", insertable = false, updatable = false)
    private Boolean activation;

    @Column(name = "reservation", insertable = false, updatable = false)
    private Boolean reservation;

    @Column(name = "utilisation", insertable = false, updatable = false)
    private Boolean utilisation;

    @Column(name = "vendu", insertable = false, updatable = false)
    private Boolean vendu;

    @Column(name = "accesscounter", insertable = false, updatable = false)
    private Integer accesscounter;

    @Column(name = "datevente", insertable = false, updatable = false)
    private LocalDate datevente;

    @Column(name = "heurevente", insertable = false, updatable = false)
    private LocalTime heurevente;

    @Column(name = "dateannulation", insertable = false, updatable = false)
    private LocalDateTime dateannulation;

    @Column(name = "evenement", insertable = false, updatable = false)
    private Integer evenement;

    @Column(name = "modelebillet", insertable = false, updatable = false)
    private Integer modelebillet;

    @Column(name = "voucherorder", insertable = false, updatable = false)
    private Integer voucherorder;

    protected Voucher() {
    }
}

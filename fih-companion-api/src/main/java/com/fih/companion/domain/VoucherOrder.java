package com.fih.companion.domain;

import jakarta.persistence.*;
import lombok.Getter;
import org.hibernate.annotations.Immutable;

import java.time.LocalDate;
import java.time.LocalTime;

 @Entity
@Table(name = "voucherorder")
@Immutable
@Getter
public class VoucherOrder {

    @Id
    @Column(name = "reference", insertable = false, updatable = false)
    private Integer reference;

    @Column(name = "code", insertable = false, updatable = false)
    private int code;

    @Column(name = "nombre", insertable = false, updatable = false)
    private int nombre;

    @Column(name = "ddate", insertable = false, updatable = false)
    private LocalDate ddate;

    @Column(name = "heure", insertable = false, updatable = false)
    private LocalTime heure;

    @Column(name = "status", insertable = false, updatable = false)
    private boolean status;

    @Column(name = "annulation", insertable = false, updatable = false)
    private boolean annulation;

    @Column(name = "infos", insertable = false, updatable = false)
    private String infos;

    @Column(name = "web", insertable = false, updatable = false)
    private String web;

    @Column(name = "client", insertable = false, updatable = false)
    private Integer client;

    @Column(name = "evenement", insertable = false, updatable = false)
    private Integer evenement;

    @Column(name = "modelebillet", insertable = false, updatable = false)
    private Integer modelebillet;

    protected VoucherOrder() {
    }
}

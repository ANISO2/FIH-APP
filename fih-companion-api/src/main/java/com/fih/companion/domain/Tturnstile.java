package com.fih.companion.domain;

import jakarta.persistence.*;
import lombok.Getter;
import org.hibernate.annotations.Immutable;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**   Read-only mapping of "tturnstile". */
@Entity
@Table(name = "tturnstile")
@Immutable
@Getter
public class Tturnstile {

    @Id
    @Column(name = "reference", insertable = false, updatable = false)
    private Integer reference;

    @Column(name = "codebarre", insertable = false, updatable = false)
    private String codebarre;

    @Column(name = "porte", insertable = false, updatable = false)
    private String porte;

    @Column(name = "datetransaction", insertable = false, updatable = false)
    private LocalDate datetransaction;

    @Column(name = "heuretransaction", insertable = false, updatable = false)
    private LocalDateTime heuretransaction;

    @Column(name = "transactionstate", insertable = false, updatable = false)
    private Boolean transactionstate;

    @Column(name = "description", insertable = false, updatable = false)
    private String description;

    @Column(name = "billet", insertable = false, updatable = false)
    private String billet;

    @Column(name = "voucher", insertable = false, updatable = false)
    private String voucher;

    @Column(name = "location", insertable = false, updatable = false)
    private int location;

    protected Tturnstile() {
    }
}

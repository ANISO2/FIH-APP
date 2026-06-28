package com.fih.companion.domain;

import jakarta.persistence.*;
import lombok.Getter;
import org.hibernate.annotations.Immutable;

 @Entity
@Table(name = "generation")
@Immutable
@Getter
public class Generation {

    @EmbeddedId
    private GenerationId id;

    @Column(name = "prefixe", insertable = false, updatable = false)
    private String prefixe;

    @Column(name = "prix", insertable = false, updatable = false)
    private double prix;

    @Column(name = "activation", insertable = false, updatable = false)
    private boolean activation;

    @Column(name = "web", insertable = false, updatable = false)
    private String web;

    @Column(name = "stockbillet", insertable = false, updatable = false)
    private int stockbillet;

    @Column(name = "stockvoucher", insertable = false, updatable = false)
    private int stockvoucher;

    @Column(name = "counterbillet", insertable = false, updatable = false)
    private int counterbillet;

    @Column(name = "countervoucher", insertable = false, updatable = false)
    private int countervoucher;

    @Column(name = "counterkit", insertable = false, updatable = false)
    private int counterkit;

    @Column(name = "client_reference", insertable = false, updatable = false)
    private Integer clientReference;

    protected Generation() {
    }
}

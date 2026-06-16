package com.fih.companion.domain;

import jakarta.persistence.*;
import lombok.Getter;
import org.hibernate.annotations.Immutable;

/** Ticket holder. Read-only mapping of "holder". FK "billet" -> billet.numeroserie. */
@Entity
@Table(name = "holder")
@Immutable
@Getter
public class Holder {

    @Id
    @Column(name = "id", insertable = false, updatable = false)
    private Integer id;

    @Column(name = "firstname", insertable = false, updatable = false)
    private String firstname;

    @Column(name = "lastname", insertable = false, updatable = false)
    private String lastname;

    @Column(name = "administration", insertable = false, updatable = false)
    private boolean administration;

    @Column(name = "sortie", insertable = false, updatable = false)
    private boolean sortie;

    @Column(name = "billet", insertable = false, updatable = false)
    private String billet;

    protected Holder() {
    }
}

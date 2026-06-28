package com.fih.companion.domain;

import jakarta.persistence.*;
import lombok.Getter;
import org.hibernate.annotations.Immutable;

 @Entity
@Table(name = "location")
@Immutable
@Getter
public class Location {

    @Id
    @Column(name = "reference", insertable = false, updatable = false)
    private Integer reference;

    @Column(name = "name", insertable = false, updatable = false)
    private String name;

    @Column(name = "turnstile", insertable = false, updatable = false)
    private boolean turnstile;

    protected Location() {
    }
}

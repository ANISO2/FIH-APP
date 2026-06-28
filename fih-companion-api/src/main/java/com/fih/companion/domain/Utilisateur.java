package com.fih.companion.domain;

import jakarta.persistence.*;
import lombok.Getter;
import org.hibernate.annotations.Immutable;

 @Entity
@Table(name = "utilisateur")
@Immutable
@Getter
public class Utilisateur {

    @Id
    @Column(name = "reference", insertable = false, updatable = false)
    private Integer reference;

    @Column(name = "username", insertable = false, updatable = false)
    private String username;

     @Column(name = "password", insertable = false, updatable = false)
    private String password;

    @Column(name = "role", insertable = false, updatable = false)
    private String role;

    @Column(name = "admin", insertable = false, updatable = false)
    private Boolean admin;

    @Column(name = "email", insertable = false, updatable = false)
    private String email;

    @Column(name = "firstname", insertable = false, updatable = false)
    private String firstname;

    @Column(name = "lastname", insertable = false, updatable = false)
    private String lastname;

    @Column(name = "mobile", insertable = false, updatable = false)
    private String mobile;

    protected Utilisateur() {
    }
}

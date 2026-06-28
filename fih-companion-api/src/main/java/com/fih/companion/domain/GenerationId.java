package com.fih.companion.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.io.Serializable;


@Embeddable
@Getter
@EqualsAndHashCode
public class GenerationId implements Serializable {

    @Column(name = "evenement")
    private Integer evenement;

    @Column(name = "modelebillet")
    private Integer modelebillet;

    protected GenerationId() {
    }
}

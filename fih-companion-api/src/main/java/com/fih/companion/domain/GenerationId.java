package com.fih.companion.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.io.Serializable;

/**
 * Composite primary key of "generation" = (evenement, modelebillet).
 *
 * We use @EmbeddedId (this class) rather than @IdClass. Trade-off in two lines:
 *  - @EmbeddedId groups the key fields into one object you access as gen.getId()
 *    .getEvenement(); it is the most explicit and beginner-friendly option.
 *  - @IdClass keeps the two ids as flat fields on the entity but requires a
 *    duplicate id class; it reads more naturally but duplicates field declarations.
 * For a read-only model, @EmbeddedId is the cleaner choice.
 */
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

package com.fih.companion.repository;

import com.fih.companion.domain.Billet;
import com.fih.companion.invitation.projection.LotRowProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BilletRepository extends JpaRepository<Billet, String> {

    /** Uses the existing DB index on codebarre. */
    Optional<Billet> findByCodebarre(String codebarre);

    Optional<Billet> findByNumeroserie(String numeroserie);

    /**
     * Change C — every billet whose numeroserie falls in [start, end], with its
     * model/event and any existing assigned name. Read-only.
     *
     * The range is a plain SQL BETWEEN on numeroserie. In this database the
     * numeroserie is a fixed-width 10-digit numeric string, so a text BETWEEN is
     * the same as a numeric one. We join modelebillet/evenement for labels and
     * LEFT JOIN our own badge_affectation so a row with no name simply returns
     * NULL. Invitation-vs-not filtering happens in Java (config-driven), so this
     * query stays generic.
     */
    @Query(value = """
            SELECT b.numeroserie  AS "numeroserie",
                   b.codebarre    AS "codebarre",
                   b.evenement    AS "eventId",
                   e.titre        AS "eventTitle",
                   b.modelebillet AS "modelId",
                   m.modele       AS "modelName",
                   ba.affectee_a  AS "affecteeA"
            FROM billet b
            LEFT JOIN evenement e    ON e.reference = b.evenement
            LEFT JOIN modelebillet m ON m.reference = b.modelebillet
            LEFT JOIN badge_affectation ba ON ba.numeroserie = b.numeroserie
            WHERE b.numeroserie BETWEEN :start AND :end
            ORDER BY b.numeroserie
            """, nativeQuery = true)
    List<LotRowProjection> findRange(@Param("start") String start, @Param("end") String end);
}

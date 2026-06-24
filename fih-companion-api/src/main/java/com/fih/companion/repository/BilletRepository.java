package com.fih.companion.repository;

import com.fih.companion.domain.Billet;
import com.fih.companion.invitation.projection.LotRowProjection;
import com.fih.companion.verification.projection.BilletVerifyProjection;
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
     * SPIKE FIX — one query for a full billet verification (replaces 5–6 round
     * trips). Looks up by codebarre OR numeroserie (both indexed: codebarre has a
     * unique index, numeroserie is the PK) and joins everything the verdict and
     * the result card need. ORDER BY (codebarre match) DESC keeps the old
     * precedence: a codebarre hit wins over a numeroserie hit. holder /
     * badge_affectation are LEFT JOINs (indexed: holder.billet, ba.numeroserie),
     * so a missing name simply returns NULL. We select the live counters
     * (utilisation, nombreacces) raw — the verdict is decided in Java, never
     * cached — so the answer always reflects the current row.
     */
    @Query(value = """
            SELECT b.numeroserie  AS "numeroserie",
                   b.codebarre    AS "codebarre",
                   b.modelebillet AS "modelId",
                   e.titre        AS "eventTitle",
                   e.ddate        AS "eventDate",
                   m.modele       AS "modelName",
                   m.maxaccess    AS "maxAccess",
                   b.activation   AS "activation",
                   b.utilisation  AS "utilisation",
                   b.vendu        AS "vendu",
                   b.reservation  AS "reservation",
                   b.nombreacces  AS "nombreacces",
                   NULLIF(trim(concat(coalesce(h.firstname, ''), ' ', coalesce(h.lastname, ''))), '') AS "holderName",
                   ba.affectee_a  AS "affecteeA"
            FROM billet b
            LEFT JOIN modelebillet m      ON m.reference   = b.modelebillet
            LEFT JOIN evenement e         ON e.reference   = b.evenement
            LEFT JOIN holder h            ON h.billet      = b.numeroserie
            LEFT JOIN badge_affectation ba ON ba.numeroserie = b.numeroserie
            WHERE b.codebarre = :code OR b.numeroserie = :code
            ORDER BY (b.codebarre = :code) DESC
            LIMIT 1
            """, nativeQuery = true)
    Optional<BilletVerifyProjection> findForVerification(@Param("code") String code);

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

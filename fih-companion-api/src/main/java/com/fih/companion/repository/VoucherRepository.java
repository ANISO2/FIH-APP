package com.fih.companion.repository;

import com.fih.companion.domain.Voucher;
import com.fih.companion.verification.projection.VoucherVerifyProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface VoucherRepository extends JpaRepository<Voucher, String> {

    /** Uses the existing DB index on codebarre. */
    Optional<Voucher> findByCodebarre(String codebarre);

    Optional<Voucher> findByNumeroserie(String numeroserie);

    /**
     * SPIKE FIX — one query for a full voucher verification (replaces several
     * round trips). Same shape as the billet finder: lookup by codebarre OR
     * numeroserie (both indexed), join model/event for labels and our
     * badge_affectation for the assigned name. Live state (utilisation,
     * accesscounter, dateannulation) is selected raw; the verdict is decided in
     * Java on every call, never cached.
     */
    @Query(value = """
            SELECT v.numeroserie    AS "numeroserie",
                   v.codebarre      AS "codebarre",
                   v.modelebillet   AS "modelId",
                   e.titre          AS "eventTitle",
                   e.ddate          AS "eventDate",
                   m.modele         AS "modelName",
                   m.maxaccess      AS "maxAccess",
                   v.activation     AS "activation",
                   v.utilisation    AS "utilisation",
                   v.vendu          AS "vendu",
                   v.reservation    AS "reservation",
                   v.accesscounter  AS "accesscounter",
                   v.dateannulation AS "dateannulation",
                   ba.affectee_a    AS "affecteeA"
            FROM voucher v
            LEFT JOIN modelebillet m      ON m.reference   = v.modelebillet
            LEFT JOIN evenement e         ON e.reference   = v.evenement
            LEFT JOIN badge_affectation ba ON ba.numeroserie = v.numeroserie
            WHERE v.codebarre = :code OR v.numeroserie = :code
            ORDER BY (v.codebarre = :code) DESC
            LIMIT 1
            """, nativeQuery = true)
    Optional<VoucherVerifyProjection> findForVerification(@Param("code") String code);
}

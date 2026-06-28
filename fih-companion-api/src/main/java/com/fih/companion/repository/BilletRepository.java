package com.fih.companion.repository;

import com.fih.companion.domain.Billet;
import com.fih.companion.invitation.projection.LotRowProjection;
import com.fih.companion.verification.projection.AccessLogProjection;
import com.fih.companion.verification.projection.BilletDetailsProjection;
import com.fih.companion.verification.projection.BilletVerifyProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BilletRepository extends JpaRepository<Billet, String> {

     Optional<Billet> findByCodebarre(String codebarre);

    Optional<Billet> findByNumeroserie(String numeroserie);


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


    @Query(value = """
            SELECT b.numeroserie    AS "numeroserie",
                   b.codebarre      AS "codebarre",
                   b.etatlivraison  AS "livre",
                   l.datelivraison  AS "dateLivraison",
                   v.datevente      AS "dateVente"
            FROM billet b
            LEFT JOIN livraison l ON l.id = b.livraison
            LEFT JOIN vente v     ON v.id = b.vente
            WHERE b.numeroserie = :numeroserie
            LIMIT 1
            """, nativeQuery = true)
    Optional<BilletDetailsProjection> findBilletDetails(@Param("numeroserie") String numeroserie);


    @Query(value = """
            SELECT t.reference        AS "reference",
                   t.codebarre        AS "codebarre",
                   t.datetransaction  AS "datetransaction",
                   t.heuretransaction AS "heuretransaction",
                   t.porte            AS "porte",
                   t.transactionstate AS "transactionstate"
            FROM tturnstile t
            WHERE t.billet = :numeroserie
            ORDER BY t.heuretransaction DESC NULLS LAST
            LIMIT 200
            """, nativeQuery = true)
    List<AccessLogProjection> findPublicAccessLog(@Param("numeroserie") String numeroserie);

     @Query(value = """
            SELECT t.reference        AS "reference",
                   t.codebarre        AS "codebarre",
                   t.datetransaction  AS "datetransaction",
                   t.heuretransaction AS "heuretransaction",
                   t.porte            AS "porte",
                   t.transactionstate AS "transactionstate"
            FROM vipaccess t
            WHERE t.billet = :numeroserie
            ORDER BY t.heuretransaction DESC NULLS LAST
            LIMIT 200
            """, nativeQuery = true)
    List<AccessLogProjection> findVipAccessLog(@Param("numeroserie") String numeroserie);


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

package com.fih.companion.verification.admin;

import com.fih.companion.domain.Billet;
import com.fih.companion.verification.projection.BilletSearchProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;


public interface AdminBilletSearchRepository extends Repository<Billet, String> {

    String SELECT = """
            SELECT b.numeroserie  AS "numeroserie",
                   b.codebarre    AS "codebarre",
                   b.activation   AS "activation",
                   b.etatlivraison AS "livre",
                   b.vendu        AS "vendu",
                   b.utilisation  AS "utilise",
                   e.titre        AS "eventTitle",
                   m.modele       AS "modelName",
                   v.datevente    AS "dateVente",
                   lv.rolecontroleur AS "livreur",
                   l.datelivraison AS "dateLivraison"
            FROM billet b
            LEFT JOIN evenement e    ON e.reference = b.evenement
            LEFT JOIN modelebillet m ON m.reference = b.modelebillet
            LEFT JOIN vente v        ON v.id = b.vente
            LEFT JOIN livraison l    ON l.id = b.livraison
            LEFT JOIN livreur lv     ON lv.reference = l.controlleur
            """;

    @Query(value = SELECT + " WHERE b.codebarre = :value ORDER BY b.numeroserie",
            countQuery = "SELECT count(*) FROM billet b WHERE b.codebarre = :value",
            nativeQuery = true)
    Page<BilletSearchProjection> searchByCodebarre(@Param("value") String value, Pageable pageable);

    @Query(value = SELECT + " WHERE b.codebarre LIKE :prefix ESCAPE '\\' ORDER BY b.codebarre, b.numeroserie",
            countQuery = "SELECT count(*) FROM billet b WHERE b.codebarre LIKE :prefix ESCAPE '\\'",
            nativeQuery = true)
    Page<BilletSearchProjection> searchByCodebarrePrefix(@Param("prefix") String prefix, Pageable pageable);

    @Query(value = SELECT + " WHERE b.numeroserie = :value ORDER BY b.numeroserie",
            countQuery = "SELECT count(*) FROM billet b WHERE b.numeroserie = :value",
            nativeQuery = true)
    Page<BilletSearchProjection> searchByNumeroserie(@Param("value") String value, Pageable pageable);

    @Query(value = SELECT + " WHERE b.numeroserie LIKE :prefix ESCAPE '\\' ORDER BY b.numeroserie",
            countQuery = "SELECT count(*) FROM billet b WHERE b.numeroserie LIKE :prefix ESCAPE '\\'",
            nativeQuery = true)
    Page<BilletSearchProjection> searchByNumeroseriePrefix(@Param("prefix") String prefix, Pageable pageable);
}

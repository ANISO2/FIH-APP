package com.fih.companion.verification.admin;

import com.fih.companion.domain.Billet;
import com.fih.companion.verification.projection.BilletSearchProjection;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.util.List;


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

    /** Count base with the same evenement join as the select. */
    String COUNT_FROM = "SELECT count(*) FROM billet b LEFT JOIN evenement e ON e.reference = b.evenement ";


    @Query(value = SELECT + " WHERE b.codebarre = :value " + " ORDER BY b.numeroserie LIMIT :size OFFSET :offset",
            nativeQuery = true)
    List<BilletSearchProjection> searchByCodebarre(@Param("value") String value,
                                                   @Param("size") int size,
                                                   @Param("offset") int offset);

    @Query(value = COUNT_FROM + " WHERE b.codebarre = :value ", nativeQuery = true)
    long countByCodebarre(@Param("value") String value);

    @Query(value = SELECT + " WHERE b.codebarre LIKE :prefix ESCAPE '\\' " + " ORDER BY b.codebarre, b.numeroserie LIMIT :size OFFSET :offset",
            nativeQuery = true)
    List<BilletSearchProjection> searchByCodebarrePrefix(@Param("prefix") String prefix,
                                                         @Param("size") int size,
                                                         @Param("offset") int offset);

    @Query(value = COUNT_FROM + " WHERE b.codebarre LIKE :prefix ESCAPE '\\' ", nativeQuery = true)
    long countByCodebarrePrefix(@Param("prefix") String prefix);

    @Query(value = SELECT + " WHERE b.numeroserie = :value " + " ORDER BY b.numeroserie LIMIT :size OFFSET :offset",
            nativeQuery = true)
    List<BilletSearchProjection> searchByNumeroserie(@Param("value") String value,
                                                     @Param("size") int size,
                                                     @Param("offset") int offset);

    @Query(value = COUNT_FROM + " WHERE b.numeroserie = :value ", nativeQuery = true)
    long countByNumeroserie(@Param("value") String value);

    @Query(value = SELECT + " WHERE b.numeroserie LIKE :prefix ESCAPE '\\' " + " ORDER BY b.numeroserie LIMIT :size OFFSET :offset",
            nativeQuery = true)
    List<BilletSearchProjection> searchByNumeroseriePrefix(@Param("prefix") String prefix,
                                                           @Param("size") int size,
                                                           @Param("offset") int offset);

    @Query(value = COUNT_FROM + " WHERE b.numeroserie LIKE :prefix ESCAPE '\\' ", nativeQuery = true)
    long countByNumeroseriePrefix(@Param("prefix") String prefix);
}

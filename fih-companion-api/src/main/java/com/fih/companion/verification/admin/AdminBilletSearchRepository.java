package com.fih.companion.verification.admin;

import com.fih.companion.domain.Billet;
import com.fih.companion.verification.projection.BilletSearchProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

/**
 * Backoffice "Vérification Billet" search (3.2). Separate, read-only repository
 * (extends the bare Repository marker — no save/delete exposed) so the existing
 * BilletRepository is left untouched.
 *
 * SCALE (built for ~100 000 rows):
 *  - Every lookup is on an INDEXED column only: codebarre (unique index
 *    billet_codebarre_key + index_billet_codebarre) or numeroserie (PK). Under
 *    the DB's C.UTF-8 collation an equality match is an index seek, and a
 *    `LIKE 'prefix%'` is index-eligible as well; we never offer "contains".
 *  - Interface projection returns ONLY the displayed columns — no entity or
 *    relation is materialised. The LEFT JOINs (event/model/vente/livraison/
 *    livreur) only enrich the row, never filter, so the COUNT query runs on
 *    `billet` alone and stays cheap.
 *  - Server-side pagination via Pageable; rows are never all loaded.
 *
 * The four methods are split by column and by exact/prefix so each binds to a
 * single index path (no OR across columns, which would defeat the index).
 */
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

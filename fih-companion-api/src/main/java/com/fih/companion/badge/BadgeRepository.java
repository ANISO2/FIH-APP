package com.fih.companion.badge;

import com.fih.companion.badge.projection.AvailabilityProjection;
import com.fih.companion.badge.projection.BadgeItemProjection;
import com.fih.companion.badge.projection.CodeRowProjection;
import com.fih.companion.domain.Tturnstile;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * Read-only queries for the badge feature. "Injected" records are billet + voucher
 * rows the legacy team created. Aliases are quoted so they bind to the projection
 * getters (PostgreSQL lowercases unquoted aliases).
 *
 * AFFECTÉE À (3.3)
 * ----------------
 * The item queries LEFT JOIN our app-owned badge_affectation table on numeroserie
 * to surface the assigned name ("affecteeA"). It is a LEFT JOIN, so records with
 * no name set simply return NULL — nothing is hidden. The name is also added to
 * the free-text search so admins can find a record by the name they typed. This
 * is the only place these read queries touch badge_affectation; the legacy tables
 * stay strictly read-only.
 */
public interface BadgeRepository extends Repository<Tturnstile, Integer> {

    @Query(value = """
            SELECT e.reference AS "eventId", e.titre AS "eventTitle", e.ddate AS "eventDate",
                   m.reference AS "modelId", m.modele AS "modelName",
                   coalesce(b.cnt, 0) AS "billetCount",
                   coalesce(v.cnt, 0) AS "voucherCount",
                   coalesce(b.cnt, 0) + coalesce(v.cnt, 0) AS "injectedCount"
            FROM (SELECT DISTINCT evenement, modelebillet FROM billet
                  UNION SELECT DISTINCT evenement, modelebillet FROM voucher) k
            JOIN evenement e ON e.reference = k.evenement
            JOIN modelebillet m ON m.reference = k.modelebillet
            LEFT JOIN (SELECT evenement, modelebillet, count(*) cnt FROM billet GROUP BY 1, 2) b
                   ON b.evenement = k.evenement AND b.modelebillet = k.modelebillet
            LEFT JOIN (SELECT evenement, modelebillet, count(*) cnt FROM voucher GROUP BY 1, 2) v
                   ON v.evenement = k.evenement AND v.modelebillet = k.modelebillet
            WHERE (:eventId IS NULL OR e.reference = :eventId)
            ORDER BY e.ddate, m.modele
            """, nativeQuery = true)
    List<AvailabilityProjection> availability(@Param("eventId") Integer eventId);

    @Query(value = """
            SELECT type AS "type", numeroserie AS "numeroserie", codebarre AS "codebarre",
                   holderName AS "holderName", affecteeA AS "affecteeA"
            FROM (
              SELECT 'BILLET' AS type, b.numeroserie, b.codebarre,
                     NULLIF(trim(coalesce(h.firstname, '') || ' ' || coalesce(h.lastname, '')), '') AS holderName,
                     ba.affectee_a AS affecteeA
              FROM billet b
              LEFT JOIN holder h ON h.billet = b.numeroserie
              LEFT JOIN badge_affectation ba ON ba.numeroserie = b.numeroserie
              WHERE b.evenement = :eventId AND b.modelebillet = :modelId
              UNION ALL
              SELECT 'VOUCHER', v.numeroserie, v.codebarre, NULL, ba.affectee_a
              FROM voucher v
              LEFT JOIN badge_affectation ba ON ba.numeroserie = v.numeroserie
              WHERE v.evenement = :eventId AND v.modelebillet = :modelId
            ) x
            WHERE (:search IS NULL
                   OR x.numeroserie ILIKE concat('%', :search, '%')
                   OR x.codebarre ILIKE concat('%', :search, '%')
                   OR x.holderName ILIKE concat('%', :search, '%')
                   OR x.affecteeA ILIKE concat('%', :search, '%'))
            ORDER BY x.numeroserie
            LIMIT :size OFFSET :offset
            """, nativeQuery = true)
    List<BadgeItemProjection> items(@Param("eventId") int eventId,
                                    @Param("modelId") int modelId,
                                    @Param("search") String search,
                                    @Param("size") int size,
                                    @Param("offset") int offset);

    @Query(value = """
            SELECT count(*) FROM (
              SELECT b.numeroserie, b.codebarre,
                     NULLIF(trim(coalesce(h.firstname, '') || ' ' || coalesce(h.lastname, '')), '') AS holderName,
                     ba.affectee_a AS affecteeA
              FROM billet b
              LEFT JOIN holder h ON h.billet = b.numeroserie
              LEFT JOIN badge_affectation ba ON ba.numeroserie = b.numeroserie
              WHERE b.evenement = :eventId AND b.modelebillet = :modelId
              UNION ALL
              SELECT v.numeroserie, v.codebarre, NULL, ba.affectee_a
              FROM voucher v
              LEFT JOIN badge_affectation ba ON ba.numeroserie = v.numeroserie
              WHERE v.evenement = :eventId AND v.modelebillet = :modelId
            ) x
            WHERE (:search IS NULL
                   OR x.numeroserie ILIKE concat('%', :search, '%')
                   OR x.codebarre ILIKE concat('%', :search, '%')
                   OR x.holderName ILIKE concat('%', :search, '%')
                   OR x.affecteeA ILIKE concat('%', :search, '%'))
            """, nativeQuery = true)
    long itemsCount(@Param("eventId") int eventId,
                    @Param("modelId") int modelId,
                    @Param("search") String search);

    /** All records for a model (no paging) — used for "generate all" and photo-check. */
    @Query(value = """
            SELECT 'BILLET' AS "type", b.numeroserie AS "numeroserie", b.codebarre AS "codebarre",
                   NULLIF(trim(coalesce(h.firstname, '') || ' ' || coalesce(h.lastname, '')), '') AS "holderName",
                   ba.affectee_a AS "affecteeA"
            FROM billet b
            LEFT JOIN holder h ON h.billet = b.numeroserie
            LEFT JOIN badge_affectation ba ON ba.numeroserie = b.numeroserie
            WHERE b.evenement = :eventId AND b.modelebillet = :modelId
            UNION ALL
            SELECT 'VOUCHER', v.numeroserie, v.codebarre, NULL, ba.affectee_a
            FROM voucher v
            LEFT JOIN badge_affectation ba ON ba.numeroserie = v.numeroserie
            WHERE v.evenement = :eventId AND v.modelebillet = :modelId
            ORDER BY 2
            """, nativeQuery = true)
    List<BadgeItemProjection> allItems(@Param("eventId") int eventId, @Param("modelId") int modelId);

    /** Light rows for festival-wide photo coverage counting. */
    @Query(value = """
            SELECT evenement AS "eventId", modelebillet AS "modelId", codebarre AS "codebarre", numeroserie AS "numeroserie"
            FROM billet WHERE (:eventId IS NULL OR evenement = :eventId)
            UNION ALL
            SELECT evenement, modelebillet, codebarre, numeroserie
            FROM voucher WHERE (:eventId IS NULL OR evenement = :eventId)
            """, nativeQuery = true)
    List<CodeRowProjection> codeRows(@Param("eventId") Integer eventId);
}

package com.fih.companion.badge;

import com.fih.companion.badge.projection.AvailabilityProjection;
import com.fih.companion.badge.projection.BadgeItemProjection;
import com.fih.companion.badge.projection.CountsProjection;
import com.fih.companion.domain.Tturnstile;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.util.List;


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
            WHERE (CAST(:eventId AS integer) IS NULL OR e.reference = CAST(:eventId AS integer))
            ORDER BY e.ddate, m.modele
            """, nativeQuery = true)
    List<AvailabilityProjection> availability(@Param("eventId") Integer eventId);


    @Query(value = """
            SELECT type AS "type", numeroserie AS "numeroserie", codebarre AS "codebarre",
                   holderName AS "holderName", affecteeA AS "affecteeA", printedAt AS "printedAt"
            FROM (
              SELECT 'BILLET' AS type, b.numeroserie, b.codebarre,
                     NULLIF(trim(coalesce(h.firstname, '') || ' ' || coalesce(h.lastname, '')), '') AS holderName,
                     ba.affectee_a AS affecteeA, ba.printed_at AS printedAt
              FROM billet b
              LEFT JOIN holder h ON h.billet = b.numeroserie
              LEFT JOIN badge_affectation ba ON ba.numeroserie = b.numeroserie
              WHERE b.evenement = CAST(:eventId AS integer) AND b.modelebillet = :modelId
              UNION ALL
              SELECT 'VOUCHER', v.numeroserie, v.codebarre, NULL, ba.affectee_a, ba.printed_at
              FROM voucher v
              LEFT JOIN badge_affectation ba ON ba.numeroserie = v.numeroserie
              WHERE v.evenement = CAST(:eventId AS integer) AND v.modelebillet = :modelId
            ) x
            WHERE (CAST(:search AS text) IS NULL
                   OR x.numeroserie ILIKE concat('%', CAST(:search AS text), '%')
                   OR x.codebarre ILIKE concat('%', CAST(:search AS text), '%')
                   OR x.holderName ILIKE concat('%', CAST(:search AS text), '%')
                   OR x.affecteeA ILIKE concat('%', CAST(:search AS text), '%'))
              AND (:status = 'all'
                   OR (:status = 'affected' AND x.affecteeA IS NOT NULL)
                   OR (:status = 'pending'  AND x.affecteeA IS NULL))
            ORDER BY x.numeroserie
            LIMIT :size OFFSET :offset
            """, nativeQuery = true)
    List<BadgeItemProjection> items(@Param("eventId") int eventId,
                                    @Param("modelId") int modelId,
                                    @Param("search") String search,
                                    @Param("status") String status,
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
              WHERE b.evenement = CAST(:eventId AS integer) AND b.modelebillet = :modelId
              UNION ALL
              SELECT v.numeroserie, v.codebarre, NULL, ba.affectee_a
              FROM voucher v
              LEFT JOIN badge_affectation ba ON ba.numeroserie = v.numeroserie
              WHERE v.evenement = CAST(:eventId AS integer) AND v.modelebillet = :modelId
            ) x
            WHERE (CAST(:search AS text) IS NULL
                   OR x.numeroserie ILIKE concat('%', CAST(:search AS text), '%')
                   OR x.codebarre ILIKE concat('%', CAST(:search AS text), '%')
                   OR x.holderName ILIKE concat('%', CAST(:search AS text), '%')
                   OR x.affecteeA ILIKE concat('%', CAST(:search AS text), '%'))
              AND (:status = 'all'
                   OR (:status = 'affected' AND x.affecteeA IS NOT NULL)
                   OR (:status = 'pending'  AND x.affecteeA IS NULL))
            """, nativeQuery = true)
    long itemsCount(@Param("eventId") int eventId,
                    @Param("modelId") int modelId,
                    @Param("search") String search,
                    @Param("status") String status);


    @Query(value = """
            SELECT count(*) FILTER (WHERE x.affecteeA IS NOT NULL) AS "affected",
                   count(*) FILTER (WHERE x.affecteeA IS NULL)     AS "pending",
                   count(*)                                        AS "total"
            FROM (
              SELECT ba.affectee_a AS affecteeA
              FROM billet b
              LEFT JOIN badge_affectation ba ON ba.numeroserie = b.numeroserie
              WHERE b.evenement = CAST(:eventId AS integer) AND b.modelebillet = :modelId
              UNION ALL
              SELECT ba.affectee_a
              FROM voucher v
              LEFT JOIN badge_affectation ba ON ba.numeroserie = v.numeroserie
              WHERE v.evenement = CAST(:eventId AS integer) AND v.modelebillet = :modelId
            ) x
            """, nativeQuery = true)
    CountsProjection counts(@Param("eventId") int eventId, @Param("modelId") int modelId);

    @Query(value = """
            SELECT 'BILLET' AS "type", b.numeroserie AS "numeroserie", b.codebarre AS "codebarre",
                   NULLIF(trim(coalesce(h.firstname, '') || ' ' || coalesce(h.lastname, '')), '') AS "holderName",
                   ba.affectee_a AS "affecteeA", ba.printed_at AS "printedAt"
            FROM billet b
            LEFT JOIN holder h ON h.billet = b.numeroserie
            LEFT JOIN badge_affectation ba ON ba.numeroserie = b.numeroserie
            WHERE b.evenement = CAST(:eventId AS integer) AND b.modelebillet = :modelId
            UNION ALL
            SELECT 'VOUCHER', v.numeroserie, v.codebarre, NULL, ba.affectee_a, ba.printed_at
            FROM voucher v
            LEFT JOIN badge_affectation ba ON ba.numeroserie = v.numeroserie
            WHERE v.evenement = CAST(:eventId AS integer) AND v.modelebillet = :modelId
            ORDER BY 2
            """, nativeQuery = true)
    List<BadgeItemProjection> allItems(@Param("eventId") int eventId, @Param("modelId") int modelId);
}

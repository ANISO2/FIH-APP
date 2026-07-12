package com.fih.companion.stats;

import com.fih.companion.domain.Tturnstile;
import com.fih.companion.stats.projection.*;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface StatsRepository extends Repository<Tturnstile, Integer> {


    @Query(value = """
            SELECT
              (SELECT count(*) FROM evenement e)            AS "totalEvents",
              (SELECT count(*) FROM billet b
                 WHERE b.evenement IN (SELECT reference FROM evenement)) AS "totalBillets",
              (SELECT count(*) FROM voucher v
                 WHERE v.evenement IN (SELECT reference FROM evenement)) AS "totalVouchers",
              (SELECT count(*) FROM tturnstile t
                 WHERE t.datetransaction IN (SELECT ddate FROM evenement))     AS "totalScans",
              (SELECT count(*) FROM tturnstile t
                 WHERE transactionstate IS TRUE
                   AND t.datetransaction IN (SELECT ddate FROM evenement))     AS "acceptedScans",
              (SELECT count(*) FROM tturnstile t
                 WHERE transactionstate IS NOT TRUE
                   AND t.datetransaction IN (SELECT ddate FROM evenement))     AS "rejectedScans",
              (SELECT count(*) FROM tturnstile t
                 WHERE lower(porte) = 'public'
                   AND t.datetransaction IN (SELECT ddate FROM evenement))     AS "publicScans",
              (SELECT count(*) FROM tturnstile t
                 WHERE lower(porte) = 'vip'
                   AND t.datetransaction IN (SELECT ddate FROM evenement))     AS "vipScans"
            """, nativeQuery = true)
    OverviewCountsProjection overviewCounts();

    @Query(value = """
            SELECT e.titre AS "title", e.ddate AS "date",
                   count(t.reference) FILTER (WHERE t.transactionstate IS TRUE) AS "scans"
            FROM evenement e
            JOIN tturnstile t ON t.datetransaction = e.ddate
            GROUP BY e.reference, e.titre, e.ddate
            ORDER BY count(t.reference) FILTER (WHERE t.transactionstate IS TRUE) DESC
            LIMIT 1
            """, nativeQuery = true)
    BusiestEventProjection busiestEvent();

    @Query(value = """
            SELECT e.ddate AS "date",
                   count(t.reference) AS "scans",
                   count(t.reference) FILTER (WHERE t.transactionstate IS TRUE)     AS "accepted",
                   count(t.reference) FILTER (WHERE t.transactionstate IS NOT TRUE) AS "rejected"
            FROM evenement e
            JOIN tturnstile t ON t.datetransaction = e.ddate
            GROUP BY e.ddate
            ORDER BY e.ddate
            """, nativeQuery = true)
    List<EntryByDayProjection> entriesByDay();

    @Query(value = """
            SELECT lower(porte) AS "gate",
                   count(*) AS "scans",
                   count(*) FILTER (WHERE transactionstate IS TRUE)     AS "accepted",
                   count(*) FILTER (WHERE transactionstate IS NOT TRUE) AS "rejected"
            FROM tturnstile t
            WHERE porte IS NOT NULL
              AND t.datetransaction IN (SELECT ddate FROM evenement)
            GROUP BY lower(porte)
            """, nativeQuery = true)
    List<GateProjection> gateBreakdown();

    @Query(value = """
            SELECT
              (SELECT count(*) FROM billet b
                 WHERE b.evenement IN (SELECT reference FROM evenement))   AS "billetIssued",
              (SELECT count(DISTINCT t.billet) FROM tturnstile t
                 WHERE t.billet IS NOT NULL
                   AND t.datetransaction IN (SELECT ddate FROM evenement))       AS "billetScanned",
              (SELECT count(*) FROM voucher v
                 WHERE v.evenement IN (SELECT reference FROM evenement))   AS "voucherIssued",
              (SELECT count(DISTINCT t.voucher) FROM tturnstile t
                 WHERE t.voucher IS NOT NULL
                   AND t.datetransaction IN (SELECT ddate FROM evenement))       AS "voucherScanned"
            """, nativeQuery = true)
    TicketTypesProjection ticketTypes();

    @Query(value = """
            SELECT e.reference AS "eventId", e.titre AS "title", e.ddate AS "date",
                   count(t.reference) AS "scans",
                   count(t.reference) FILTER (WHERE t.transactionstate IS TRUE)        AS "accepted",
                   count(t.reference) FILTER (WHERE t.transactionstate IS NOT TRUE)    AS "rejected",
                   count(t.reference) FILTER (WHERE lower(t.porte) = 'public' AND t.transactionstate IS TRUE) AS "publicScans",
                   count(t.reference) FILTER (WHERE lower(t.porte) = 'vip'    AND t.transactionstate IS TRUE) AS "vipScans"
            FROM evenement e
            LEFT JOIN tturnstile t ON t.datetransaction = e.ddate
            GROUP BY e.reference, e.titre, e.ddate
            ORDER BY e.ddate
            """, nativeQuery = true)
    List<EventRollupProjection> eventRollups();

    @Query(value = """
            SELECT e.reference AS "eventId", e.titre AS "title", e.ddate AS "date",
                   count(t.reference) AS "scans",
                   count(t.reference) FILTER (WHERE t.transactionstate IS TRUE)        AS "accepted",
                   count(t.reference) FILTER (WHERE t.transactionstate IS NOT TRUE)    AS "rejected",
                   count(t.reference) FILTER (WHERE lower(t.porte) = 'public' AND t.transactionstate IS TRUE) AS "publicScans",
                   count(t.reference) FILTER (WHERE lower(t.porte) = 'vip'    AND t.transactionstate IS TRUE) AS "vipScans"
            FROM evenement e
            LEFT JOIN tturnstile t ON t.datetransaction = e.ddate
            WHERE e.reference = :id
            GROUP BY e.reference, e.titre, e.ddate
            """, nativeQuery = true)
    EventRollupProjection eventRollup(@Param("id") int id);

    @Query(value = """
            SELECT lower(t.porte) AS "gate",
                   count(*) AS "scans",
                   count(*) FILTER (WHERE t.transactionstate IS TRUE)     AS "accepted",
                   count(*) FILTER (WHERE t.transactionstate IS NOT TRUE) AS "rejected"
            FROM tturnstile t
            JOIN evenement e ON e.ddate = t.datetransaction
            WHERE e.reference = :id AND t.porte IS NOT NULL
            GROUP BY lower(t.porte)
            """, nativeQuery = true)
    List<GateProjection> gateForEvent(@Param("id") int id);

    @Query(value = """
            SELECT extract(hour FROM t.heuretransaction)::int AS "hour",
                   count(*) AS "scans"
            FROM tturnstile t
            JOIN evenement e ON e.ddate = t.datetransaction
            WHERE e.reference = :id AND t.heuretransaction IS NOT NULL
              AND t.transactionstate IS TRUE
            GROUP BY extract(hour FROM t.heuretransaction)::int
            ORDER BY 1
            """, nativeQuery = true)
    List<HourProjection> entriesByHour(@Param("id") int id);

    // ------------------------------------------------------------------ Recette

    @Query(value = """
            SELECT e.reference AS "eventId", e.titre AS "eventTitle", e.ddate AS "eventDate",
                   COALESCE(SUM(g.counterbillet  * g.prix), 0) AS "billet",
                   COALESCE(SUM(g.countervoucher * g.prix), 0) AS "voucher",
                   COALESCE(SUM((g.counterbillet + g.countervoucher) * g.prix), 0) AS "total"
            FROM evenement e
            JOIN generation g ON g.evenement = e.reference
            WHERE g.prix > 0
            GROUP BY e.reference, e.titre, e.ddate
            ORDER BY "total" DESC, e.ddate
            """, nativeQuery = true)
    List<RecetteSummaryProjection> recetteSummary();


    @Query(value = """
            SELECT e.reference AS "eventId", e.titre AS "eventTitle", e.ddate AS "eventDate",
                   COALESCE(SUM(g.stockbillet + g.stockvoucher), 0)                          AS "totalGenere",
                   COALESCE(SUM(g.counterbillet + g.countervoucher), 0)                        AS "totalVendu",
                   COALESCE(SUM((g.stockbillet  - g.counterbillet)
                              + (g.stockvoucher - g.countervoucher)), 0)                     AS "totalReste",
                   COALESCE(SUM((g.counterbillet + g.countervoucher) * g.prix), 0)           AS "recetteTotale"
            FROM evenement e
            JOIN generation g ON g.evenement = e.reference
            WHERE g.prix > 0
            GROUP BY e.reference, e.titre, e.ddate
            ORDER BY "recetteTotale" DESC, e.ddate
            """, nativeQuery = true)
    List<RecetteEventHeaderProjection> recetteDetailHeaders();


    @Query(value = """
            SELECT m.reference AS "modelId", m.modele AS "modelName",
                   g.prix AS "montant",
                   g.stockbillet                                              AS "billetGeneration",
                   g.counterbillet                                            AS "billetVente",
                   (g.stockbillet - g.counterbillet)                          AS "billetReste",
                   g.stockvoucher                                             AS "voucherGeneration",
                   g.countervoucher                                           AS "voucherVente",
                   (g.stockvoucher - g.countervoucher)                        AS "voucherReste",
                   (g.counterbillet + g.countervoucher)                       AS "totalVendu",
                   (g.counterbillet + g.countervoucher) * g.prix              AS "recetteTnd"
            FROM generation g
            JOIN modelebillet m ON m.reference = g.modelebillet
            WHERE g.evenement = :eventId AND g.prix > 0
            ORDER BY m.modele
            """, nativeQuery = true)
    List<RecetteModelRowProjection> recetteDetailRows(@Param("eventId") int eventId);

    // -------------------------------------------------------- Recette par guichet


    @Query(value = """
            SELECT e.reference AS "eventId", e.titre AS "eventTitle", e.ddate AS "eventDate",
                   COALESCE(vb.recette, 0) AS "billet",
                   COALESCE(kk.recette, 0) AS "kit",
                   COALESCE(vb.recette, 0) + COALESCE(kk.recette, 0) AS "total"
            FROM evenement e
            LEFT JOIN (SELECT evenement, SUM(montantnet) AS recette
                       FROM vente WHERE annulation IS NOT TRUE
                       GROUP BY evenement) vb ON vb.evenement = e.reference
            LEFT JOIN (SELECT dk.evenement, SUM(k.montantnet) AS recette
                       FROM detailkit dk JOIN kit k ON k.id = dk.kit
                       GROUP BY dk.evenement) kk ON kk.evenement = e.reference
            WHERE (vb.recette IS NOT NULL OR kk.recette IS NOT NULL)
            ORDER BY "total" DESC, e.ddate
            """, nativeQuery = true)
    List<RecetteGuichetSummaryProjection> recetteGuichetSummary();

    @Query(value = """
            SELECT e.reference AS "eventId", e.titre AS "eventTitle", e.ddate AS "eventDate",
                   m.reference AS "modelId", m.modele AS "modelName",
                   COALESCE(l.livraison, 0)    AS "billetLivraison",
                   COALESCE(s.vente, 0)        AS "billetVente",
                   COALESCE(s.prixunitaire, 0) AS "billetPrixUnitaire",
                   COALESCE(s.recette, 0)      AS "billetRecette",
                   COALESCE(l.livraison, 0) - COALESCE(s.vente, 0) AS "billetReste",
                   COALESCE(k.recette, 0)      AS "kit"
            FROM (SELECT evenement, modelebillet FROM livraison
                  UNION SELECT evenement, modelebillet FROM vente
                  UNION SELECT evenement, modelebillet FROM detailkit) km
            JOIN evenement e ON e.reference = km.evenement
            JOIN modelebillet m ON m.reference = km.modelebillet
            LEFT JOIN (SELECT evenement, modelebillet, SUM(nbrebillets) AS livraison
                       FROM livraison WHERE annulation IS NOT TRUE
                       GROUP BY evenement, modelebillet) l
                   ON l.evenement = km.evenement AND l.modelebillet = km.modelebillet
            LEFT JOIN (SELECT evenement, modelebillet, SUM(nombre) AS vente,
                              MAX(montantunitaire) AS prixunitaire, SUM(montantnet) AS recette
                       FROM vente WHERE annulation IS NOT TRUE
                       GROUP BY evenement, modelebillet) s
                   ON s.evenement = km.evenement AND s.modelebillet = km.modelebillet
            LEFT JOIN (SELECT dk.evenement, dk.modelebillet, SUM(k.montantnet) AS recette
                       FROM detailkit dk JOIN kit k ON k.id = dk.kit
                       GROUP BY dk.evenement, dk.modelebillet) k
                   ON k.evenement = km.evenement AND k.modelebillet = km.modelebillet
            ORDER BY e.ddate, e.titre, m.modele
            """, nativeQuery = true)
    List<RecetteGuichetDetailProjection> recetteGuichetDetail();

    // ----------------------------------------------------- Statistique des tourniquets

    @Query(value = """
            WITH codes AS (
              SELECT evenement, modelebillet,
                     count(*) FILTER (WHERE kind = 'BILLET')  AS billet_codes,
                     count(*) FILTER (WHERE kind = 'VOUCHER') AS voucher_codes
              FROM (SELECT evenement, modelebillet, 'BILLET' AS kind FROM billet
                    UNION ALL
                    SELECT evenement, modelebillet, 'VOUCHER' FROM voucher) ac
              GROUP BY evenement, modelebillet
            ),
            tx AS (
              SELECT COALESCE(b.evenement, v.evenement) AS evenement,
                     COALESCE(b.modelebillet, v.modelebillet) AS modelebillet,
                     count(*) FILTER (WHERE t.billet IS NOT NULL  AND t.transactionstate IS TRUE) AS billet_tx,
                     count(*) FILTER (WHERE t.voucher IS NOT NULL AND t.transactionstate IS TRUE) AS voucher_tx
              FROM tturnstile t
              LEFT JOIN billet b  ON b.numeroserie = t.billet
              LEFT JOIN voucher v ON v.numeroserie = t.voucher
              GROUP BY 1, 2
            )
            SELECT e.reference AS "eventId", e.titre AS "eventTitle", e.ddate AS "eventDate",
                   m.reference AS "modelId", m.modele AS "modelName",
                   COALESCE(c.billet_codes, 0)  AS "billetCodes",
                   COALESCE(c.voucher_codes, 0) AS "voucherCodes",
                   COALESCE(t.billet_tx, 0)     AS "billetTx",
                   COALESCE(t.voucher_tx, 0)    AS "voucherTx"
            FROM codes c
            FULL OUTER JOIN tx t ON t.evenement = c.evenement AND t.modelebillet = c.modelebillet
            JOIN evenement e ON e.reference = COALESCE(c.evenement, t.evenement)
            JOIN modelebillet m ON m.reference = COALESCE(c.modelebillet, t.modelebillet)
            ORDER BY e.ddate, e.titre, m.modele
            """, nativeQuery = true)
    List<TourniquetProjection> tourniquets();

    // ------------------------------------------------- Analyse des rejets (§5 / Part C)

    @Query(value = """
            SELECT count(*) FILTER (WHERE t.transactionstate = false) AS rejets,
                   count(*) AS total
            FROM tturnstile t
            LEFT JOIN billet b  ON b.numeroserie = t.billet
            LEFT JOIN voucher v ON v.numeroserie = t.voucher
            LEFT JOIN evenement e ON e.reference = COALESCE(b.evenement, v.evenement)
            """, nativeQuery = true)
    RejetKpiProjection rejetsKpi();

    @Query(value = """
            SELECT CASE
                     WHEN lower(t.description) LIKE '%utilis%' THEN 'Déjà utilisé'
                     WHEN lower(t.description) LIKE '%date%'   THEN 'Date incohérente'
                     WHEN lower(t.description) LIKE '%porte%'  THEN 'Porte inaccessible'
                     ELSE 'Autre'
                   END AS "label",
                   count(*) AS "valeur"
            FROM tturnstile t
            LEFT JOIN billet b  ON b.numeroserie = t.billet
            LEFT JOIN voucher v ON v.numeroserie = t.voucher
            LEFT JOIN evenement e ON e.reference = COALESCE(b.evenement, v.evenement)
            WHERE t.transactionstate = false
            GROUP BY 1 ORDER BY 2 DESC
            """, nativeQuery = true)
    List<RejetGroupProjection> rejetsParCategorie();

    @Query(value = """
            SELECT CASE
                     WHEN lower(t.porte) LIKE 'vip%'    THEN 'VIP'
                     WHEN lower(t.porte) LIKE 'public%' THEN 'Public'
                     ELSE COALESCE(t.porte, 'Inconnu')
                   END AS "label",
                   count(*) AS "valeur"
            FROM tturnstile t
            LEFT JOIN billet b  ON b.numeroserie = t.billet
            LEFT JOIN voucher v ON v.numeroserie = t.voucher
            LEFT JOIN evenement e ON e.reference = COALESCE(b.evenement, v.evenement)
            WHERE t.transactionstate = false
            GROUP BY 1 ORDER BY 2 DESC
            """, nativeQuery = true)
    List<RejetGroupProjection> rejetsParPorte();

    @Query(value = """
            SELECT e.reference AS "eventId", e.titre AS "eventTitle", e.ddate AS "eventDate", count(*) AS "rejets"
            FROM tturnstile t
            LEFT JOIN billet b  ON b.numeroserie = t.billet
            LEFT JOIN voucher v ON v.numeroserie = t.voucher
            JOIN evenement e ON e.reference = COALESCE(b.evenement, v.evenement)
            WHERE t.transactionstate = false
            GROUP BY e.reference, e.titre, e.ddate ORDER BY count(*) DESC
            """, nativeQuery = true)
    List<RejetEvenementProjection> rejetsParEvenement();

    @Query(value = """
            SELECT m.reference AS "modelId", m.modele AS "modelName", count(*) AS "rejets"
            FROM tturnstile t
            LEFT JOIN billet b  ON b.numeroserie = t.billet
            LEFT JOIN voucher v ON v.numeroserie = t.voucher
            JOIN evenement e ON e.reference = COALESCE(b.evenement, v.evenement)
            JOIN modelebillet m ON m.reference = COALESCE(b.modelebillet, v.modelebillet)
            WHERE t.transactionstate = false
            GROUP BY m.reference, m.modele ORDER BY count(*) DESC
            """, nativeQuery = true)
    List<RejetModeleProjection> rejetsParModele();

    @Query(value = """
            SELECT t.datetransaction AS "jour", count(*) AS "rejets"
            FROM tturnstile t
            LEFT JOIN billet b  ON b.numeroserie = t.billet
            LEFT JOIN voucher v ON v.numeroserie = t.voucher
            LEFT JOIN evenement e ON e.reference = COALESCE(b.evenement, v.evenement)
            WHERE t.transactionstate = false
              AND t.datetransaction IS NOT NULL
            GROUP BY t.datetransaction ORDER BY t.datetransaction
            """, nativeQuery = true)
    List<RejetJourProjection> rejetsParJour();

    @Query(value = """
            SELECT t.codebarre AS "codebarre", e.titre AS "eventTitle", t.porte AS "porte",
                   t.heuretransaction AS "dateTime", t.description AS "description"
            FROM tturnstile t
            LEFT JOIN billet b  ON b.numeroserie = t.billet
            LEFT JOIN voucher v ON v.numeroserie = t.voucher
            LEFT JOIN evenement e ON e.reference = COALESCE(b.evenement, v.evenement)
            WHERE t.transactionstate = false
            ORDER BY t.heuretransaction DESC NULLS LAST
            LIMIT 2000
            """, nativeQuery = true)
    List<RejetScanProjection> rejetsScans();
}
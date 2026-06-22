package com.fih.companion.stats;

import com.fih.companion.domain.Tturnstile;
import com.fih.companion.stats.projection.*;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * Read-only statistics queries. Extends the bare Repository marker (not
 * JpaRepository) so no save/delete methods are exposed at all.
 *
 * Every query is an aggregate (no full-table fetch). Aliases are QUOTED so the
 * result column labels keep their exact case and bind to the projection getters
 * (PostgreSQL lowercases unquoted aliases, which would break the mapping).
 *
 * Scan -> event attribution: a scan belongs to the event whose ddate equals the
 * scan's datetransaction. This avoids the "FIH" pass-container event (ref 58,
 * dated 1970) absorbing every pass scan. porte is normalized with lower().
 *
 * YEAR AWARENESS (added for multi-edition support)
 * ------------------------------------------------
 * The database may hold several festival editions across different years. Every
 * query now takes a nullable :year parameter, filtered by the YEAR of
 * evenement.ddate. The filter is written as `(:year IS NULL OR ...)` so that:
 *   - :year = NULL  -> "Toutes les années": reproduces the ORIGINAL numbers
 *                      byte-for-byte (verified against the dev backup).
 *   - :year = 2025  -> only that edition.
 *
 * For the flat count queries (overview, ticket-types, gate breakdown) the guard
 * uses an `IN (SELECT ddate/reference FROM evenement WHERE year = :year)`
 * subquery rather than a JOIN, precisely so the NULL case stays identical to the
 * pre-year behaviour (a JOIN would have dropped the ~86 scans whose date matches
 * no event). Nothing here writes; all methods are SELECT-only.
 */
public interface StatsRepository extends Repository<Tturnstile, Integer> {

    /** Distinct festival years present in evenement.ddate, most-recent first. */
    @Query(value = """
            SELECT DISTINCT extract(year FROM ddate)::int AS yr
            FROM evenement
            WHERE ddate IS NOT NULL
            ORDER BY yr DESC
            """, nativeQuery = true)
    List<Integer> availableYears();

    @Query(value = """
            SELECT
              (SELECT count(*) FROM evenement e
                 WHERE (:year IS NULL OR extract(year FROM e.ddate) = :year))            AS "totalEvents",
              (SELECT count(*) FROM billet b
                 WHERE (:year IS NULL OR b.evenement IN
                        (SELECT reference FROM evenement WHERE extract(year FROM ddate) = :year))) AS "totalBillets",
              (SELECT count(*) FROM voucher v
                 WHERE (:year IS NULL OR v.evenement IN
                        (SELECT reference FROM evenement WHERE extract(year FROM ddate) = :year))) AS "totalVouchers",
              (SELECT count(*) FROM tturnstile t
                 WHERE (:year IS NULL OR t.datetransaction IN
                        (SELECT ddate FROM evenement WHERE extract(year FROM ddate) = :year)))     AS "totalScans",
              (SELECT count(*) FROM tturnstile t
                 WHERE transactionstate IS TRUE
                   AND (:year IS NULL OR t.datetransaction IN
                        (SELECT ddate FROM evenement WHERE extract(year FROM ddate) = :year)))     AS "acceptedScans",
              (SELECT count(*) FROM tturnstile t
                 WHERE transactionstate IS NOT TRUE
                   AND (:year IS NULL OR t.datetransaction IN
                        (SELECT ddate FROM evenement WHERE extract(year FROM ddate) = :year)))     AS "rejectedScans",
              (SELECT count(*) FROM tturnstile t
                 WHERE lower(porte) = 'public'
                   AND (:year IS NULL OR t.datetransaction IN
                        (SELECT ddate FROM evenement WHERE extract(year FROM ddate) = :year)))     AS "publicScans",
              (SELECT count(*) FROM tturnstile t
                 WHERE lower(porte) = 'vip'
                   AND (:year IS NULL OR t.datetransaction IN
                        (SELECT ddate FROM evenement WHERE extract(year FROM ddate) = :year)))     AS "vipScans"
            """, nativeQuery = true)
    OverviewCountsProjection overviewCounts(@Param("year") Integer year);

    @Query(value = """
            SELECT e.titre AS "title", e.ddate AS "date", count(t.reference) AS "scans"
            FROM evenement e
            JOIN tturnstile t ON t.datetransaction = e.ddate
            WHERE (:year IS NULL OR extract(year FROM e.ddate) = :year)
            GROUP BY e.reference, e.titre, e.ddate
            ORDER BY count(t.reference) DESC
            LIMIT 1
            """, nativeQuery = true)
    BusiestEventProjection busiestEvent(@Param("year") Integer year);

    @Query(value = """
            SELECT e.ddate AS "date",
                   count(t.reference) AS "scans",
                   count(t.reference) FILTER (WHERE t.transactionstate IS TRUE)     AS "accepted",
                   count(t.reference) FILTER (WHERE t.transactionstate IS NOT TRUE) AS "rejected"
            FROM evenement e
            JOIN tturnstile t ON t.datetransaction = e.ddate
            WHERE (:year IS NULL OR extract(year FROM e.ddate) = :year)
            GROUP BY e.ddate
            ORDER BY e.ddate
            """, nativeQuery = true)
    List<EntryByDayProjection> entriesByDay(@Param("year") Integer year);

    @Query(value = """
            SELECT lower(porte) AS "gate",
                   count(*) AS "scans",
                   count(*) FILTER (WHERE transactionstate IS TRUE)     AS "accepted",
                   count(*) FILTER (WHERE transactionstate IS NOT TRUE) AS "rejected"
            FROM tturnstile t
            WHERE porte IS NOT NULL
              AND (:year IS NULL OR t.datetransaction IN
                   (SELECT ddate FROM evenement WHERE extract(year FROM ddate) = :year))
            GROUP BY lower(porte)
            """, nativeQuery = true)
    List<GateProjection> gateBreakdown(@Param("year") Integer year);

    @Query(value = """
            SELECT
              (SELECT count(*) FROM billet b
                 WHERE (:year IS NULL OR b.evenement IN
                        (SELECT reference FROM evenement WHERE extract(year FROM ddate) = :year)))   AS "billetIssued",
              (SELECT count(DISTINCT t.billet) FROM tturnstile t
                 WHERE t.billet IS NOT NULL
                   AND (:year IS NULL OR t.datetransaction IN
                        (SELECT ddate FROM evenement WHERE extract(year FROM ddate) = :year)))       AS "billetScanned",
              (SELECT count(*) FROM voucher v
                 WHERE (:year IS NULL OR v.evenement IN
                        (SELECT reference FROM evenement WHERE extract(year FROM ddate) = :year)))   AS "voucherIssued",
              (SELECT count(DISTINCT t.voucher) FROM tturnstile t
                 WHERE t.voucher IS NOT NULL
                   AND (:year IS NULL OR t.datetransaction IN
                        (SELECT ddate FROM evenement WHERE extract(year FROM ddate) = :year)))       AS "voucherScanned"
            """, nativeQuery = true)
    TicketTypesProjection ticketTypes(@Param("year") Integer year);

    @Query(value = """
            SELECT e.reference AS "eventId", e.titre AS "title", e.ddate AS "date",
                   count(t.reference) AS "scans",
                   count(t.reference) FILTER (WHERE t.transactionstate IS TRUE)        AS "accepted",
                   count(t.reference) FILTER (WHERE t.transactionstate IS NOT TRUE)    AS "rejected",
                   count(t.reference) FILTER (WHERE lower(t.porte) = 'public')         AS "publicScans",
                   count(t.reference) FILTER (WHERE lower(t.porte) = 'vip')            AS "vipScans"
            FROM evenement e
            LEFT JOIN tturnstile t ON t.datetransaction = e.ddate
            WHERE (:year IS NULL OR extract(year FROM e.ddate) = :year)
            GROUP BY e.reference, e.titre, e.ddate
            ORDER BY e.ddate
            """, nativeQuery = true)
    List<EventRollupProjection> eventRollups(@Param("year") Integer year);

    @Query(value = """
            SELECT e.reference AS "eventId", e.titre AS "title", e.ddate AS "date",
                   count(t.reference) AS "scans",
                   count(t.reference) FILTER (WHERE t.transactionstate IS TRUE)        AS "accepted",
                   count(t.reference) FILTER (WHERE t.transactionstate IS NOT TRUE)    AS "rejected",
                   count(t.reference) FILTER (WHERE lower(t.porte) = 'public')         AS "publicScans",
                   count(t.reference) FILTER (WHERE lower(t.porte) = 'vip')            AS "vipScans"
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
            GROUP BY extract(hour FROM t.heuretransaction)::int
            ORDER BY 1
            """, nativeQuery = true)
    List<HourProjection> entriesByHour(@Param("id") int id);

    // ------------------------------------------------------------------ Recette
    // Revenue is derived purely from the `generation` table (one row per
    // event x model): prix (unit price) and the counter/stock columns. Both
    // queries are year-filtered by evenement.ddate, exactly like the rest.

    /**
     * Recette résumé: revenue (TND) per event, split Billet / Voucher.
     *
     * Kit removed (Change B): the `generation` table has no real "kit stock"
     * source — counterkit is 0 everywhere — so the column only added clutter.
     * Numbers are otherwise byte-identical to before (kit term was always 0).
     *
     * SCALE: this is a single SQL GROUP BY over `generation` (one row per
     * event x model — ~129 rows in prod, and it does NOT grow when tickets are
     * sold, only when an event/model is created). It reads the pre-aggregated
     * prix/counter columns; it never touches the large billet/voucher/tturnstile
     * tables. Cost is independent of ticketing volume.
     */
    @Query(value = """
            SELECT e.reference AS "eventId", e.titre AS "eventTitle", e.ddate AS "eventDate",
                   COALESCE(SUM(g.counterbillet  * g.prix), 0) AS "billet",
                   COALESCE(SUM(g.countervoucher * g.prix), 0) AS "voucher",
                   COALESCE(SUM((g.counterbillet + g.countervoucher) * g.prix), 0) AS "total"
            FROM evenement e
            JOIN generation g ON g.evenement = e.reference
            WHERE (:year IS NULL OR extract(year FROM e.ddate) = :year)
            GROUP BY e.reference, e.titre, e.ddate
            ORDER BY "total" DESC, e.ddate
            """, nativeQuery = true)
    List<RecetteSummaryProjection> recetteSummary(@Param("year") Integer year);

    /**
     * Recette détaillée — HEADERS (Change C): one aggregated row per event with
     * the totals shown on each collapsible panel. Génération = SUM(stock*),
     * Vendu = SUM(counter*) PLUS invitations already AFFECTÉES, Reste = générés
     * - vendus, Recette = SUM(counter*prix) (money unchanged — invitations are
     * free). The per-model rows are loaded separately, on expand.
     *
     * AFFECTÉES → VENTE: an invitation that has been assigned to someone (a row
     * in our own badge_affectation table) is treated as distributed, so it
     * counts in "Vendu" instead of "Reste". Attribution: badge_affectation holds
     * the billet serial; we join it to billet to find that billet's event x
     * model. Only invitations are ever affected, so this never touches paid
     * models. The join hits billet by its primary key (numeroserie), so it stays
     * cheap as the affectation table grows. Money (recette) deliberately does NOT
     * include affectées — they are free, this is a head-count, not revenue.
     *
     * Single SQL GROUP BY over `generation`, left-joined to the small affected
     * count — same cheap shape as the résumé.
     */
    @Query(value = """
            SELECT e.reference AS "eventId", e.titre AS "eventTitle", e.ddate AS "eventDate",
                   COALESCE(SUM(g.stockbillet + g.stockvoucher), 0)                          AS "totalGenere",
                   COALESCE(SUM(g.counterbillet + g.countervoucher + COALESCE(aff.affected, 0)), 0) AS "totalVendu",
                   COALESCE(SUM((g.stockbillet  - g.counterbillet - COALESCE(aff.affected, 0))
                              + (g.stockvoucher - g.countervoucher)), 0)                     AS "totalReste",
                   COALESCE(SUM((g.counterbillet + g.countervoucher) * g.prix), 0)           AS "recetteTotale"
            FROM evenement e
            JOIN generation g ON g.evenement = e.reference
            LEFT JOIN (SELECT b.evenement, b.modelebillet, count(*) AS affected
                       FROM badge_affectation a
                       JOIN billet b ON b.numeroserie = a.numeroserie
                       GROUP BY b.evenement, b.modelebillet) aff
                   ON aff.evenement = g.evenement AND aff.modelebillet = g.modelebillet
            WHERE (:year IS NULL OR extract(year FROM e.ddate) = :year)
            GROUP BY e.reference, e.titre, e.ddate
            ORDER BY "recetteTotale" DESC, e.ddate
            """, nativeQuery = true)
    List<RecetteEventHeaderProjection> recetteDetailHeaders(@Param("year") Integer year);

    /**
     * Recette détaillée — ROWS (Change C): the per-model lines for ONE event,
     * fetched lazily when its panel is expanded. Invitations already AFFECTÉES
     * are added to billet "Vente" and removed from "Reste" (see headers above);
     * money (recetteTnd) stays on the paid counters only. Filtered by event id,
     * which already pins a single edition, so no year guard is needed.
     */
    @Query(value = """
            SELECT m.reference AS "modelId", m.modele AS "modelName",
                   g.prix AS "montant",
                   g.stockbillet                                              AS "billetGeneration",
                   (g.counterbillet + COALESCE(aff.affected, 0))              AS "billetVente",
                   (g.stockbillet - g.counterbillet - COALESCE(aff.affected, 0)) AS "billetReste",
                   g.stockvoucher                                             AS "voucherGeneration",
                   g.countervoucher                                           AS "voucherVente",
                   (g.stockvoucher - g.countervoucher)                        AS "voucherReste",
                   (g.counterbillet + g.countervoucher + COALESCE(aff.affected, 0)) AS "totalVendu",
                   (g.counterbillet + g.countervoucher) * g.prix              AS "recetteTnd"
            FROM generation g
            JOIN modelebillet m ON m.reference = g.modelebillet
            LEFT JOIN (SELECT b.evenement, b.modelebillet, count(*) AS affected
                       FROM badge_affectation a
                       JOIN billet b ON b.numeroserie = a.numeroserie
                       GROUP BY b.evenement, b.modelebillet) aff
                   ON aff.evenement = g.evenement AND aff.modelebillet = g.modelebillet
            WHERE g.evenement = :eventId
            ORDER BY m.modele
            """, nativeQuery = true)
    List<RecetteModelRowProjection> recetteDetailRows(@Param("eventId") int eventId);

    // -------------------------------------------------------- Recette par guichet
    // Box-office report driven by the point-of-sale tables (vente / livraison /
    // kit), NOT by generation. These tables are empty in this edition, so the
    // report is empty until guichet activity is recorded. Year-filtered by
    // evenement.ddate, like everything else.

    /** Recette par guichet — résumé: guichet revenue (TND) per event, split Billet / Kit. */
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
            WHERE (:year IS NULL OR extract(year FROM e.ddate) = :year)
              AND (vb.recette IS NOT NULL OR kk.recette IS NOT NULL)
            ORDER BY "total" DESC, e.ddate
            """, nativeQuery = true)
    List<RecetteGuichetSummaryProjection> recetteGuichetSummary(@Param("year") Integer year);

    /** Recette par guichet — détail: per (event x model) delivery + sales breakdown. */
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
            WHERE (:year IS NULL OR extract(year FROM e.ddate) = :year)
            ORDER BY e.ddate, e.titre, m.modele
            """, nativeQuery = true)
    List<RecetteGuichetDetailProjection> recetteGuichetDetail(@Param("year") Integer year);

    // ----------------------------------------------------- Statistique des tourniquets
    // Per (event x model): accessible barcodes (issued billet/voucher rows) and
    // turnstile transactions. Each scan is resolved to its billet/voucher and
    // hence to that ticket's event x model (LEFT JOINs, so an unresolved scan is
    // simply not attributed rather than dropped or mis-counted).

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
                     count(*) FILTER (WHERE t.billet IS NOT NULL)  AS billet_tx,
                     count(*) FILTER (WHERE t.voucher IS NOT NULL) AS voucher_tx
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
            WHERE (:year IS NULL OR extract(year FROM e.ddate) = :year)
            ORDER BY e.ddate, e.titre, m.modele
            """, nativeQuery = true)
    List<TourniquetProjection> tourniquets(@Param("year") Integer year);

    // ------------------------------------------------- Analyse des rejets (§5 / Part C)
    // Refused turnstile transactions (transactionstate = false). Each scan resolves
    // to its billet/voucher and hence to the event used for the year filter. All
    // read-only. The description text is grouped into stable categories with a CASE.

    /** Total refused + total scans for the year (accepted = total - refused). */
    @Query(value = """
            SELECT count(*) FILTER (WHERE t.transactionstate = false) AS rejets,
                   count(*) AS total
            FROM tturnstile t
            LEFT JOIN billet b  ON b.numeroserie = t.billet
            LEFT JOIN voucher v ON v.numeroserie = t.voucher
            LEFT JOIN evenement e ON e.reference = COALESCE(b.evenement, v.evenement)
            WHERE (:year IS NULL OR extract(year FROM e.ddate) = :year)
            """, nativeQuery = true)
    RejetKpiProjection rejetsKpi(@Param("year") Integer year);

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
            WHERE t.transactionstate = false AND (:year IS NULL OR extract(year FROM e.ddate) = :year)
            GROUP BY 1 ORDER BY 2 DESC
            """, nativeQuery = true)
    List<RejetGroupProjection> rejetsParCategorie(@Param("year") Integer year);

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
            WHERE t.transactionstate = false AND (:year IS NULL OR extract(year FROM e.ddate) = :year)
            GROUP BY 1 ORDER BY 2 DESC
            """, nativeQuery = true)
    List<RejetGroupProjection> rejetsParPorte(@Param("year") Integer year);

    @Query(value = """
            SELECT e.reference AS "eventId", e.titre AS "eventTitle", e.ddate AS "eventDate", count(*) AS "rejets"
            FROM tturnstile t
            LEFT JOIN billet b  ON b.numeroserie = t.billet
            LEFT JOIN voucher v ON v.numeroserie = t.voucher
            JOIN evenement e ON e.reference = COALESCE(b.evenement, v.evenement)
            WHERE t.transactionstate = false AND (:year IS NULL OR extract(year FROM e.ddate) = :year)
            GROUP BY e.reference, e.titre, e.ddate ORDER BY count(*) DESC
            """, nativeQuery = true)
    List<RejetEvenementProjection> rejetsParEvenement(@Param("year") Integer year);

    @Query(value = """
            SELECT m.reference AS "modelId", m.modele AS "modelName", count(*) AS "rejets"
            FROM tturnstile t
            LEFT JOIN billet b  ON b.numeroserie = t.billet
            LEFT JOIN voucher v ON v.numeroserie = t.voucher
            JOIN evenement e ON e.reference = COALESCE(b.evenement, v.evenement)
            JOIN modelebillet m ON m.reference = COALESCE(b.modelebillet, v.modelebillet)
            WHERE t.transactionstate = false AND (:year IS NULL OR extract(year FROM e.ddate) = :year)
            GROUP BY m.reference, m.modele ORDER BY count(*) DESC
            """, nativeQuery = true)
    List<RejetModeleProjection> rejetsParModele(@Param("year") Integer year);

    @Query(value = """
            SELECT t.datetransaction AS "jour", count(*) AS "rejets"
            FROM tturnstile t
            LEFT JOIN billet b  ON b.numeroserie = t.billet
            LEFT JOIN voucher v ON v.numeroserie = t.voucher
            LEFT JOIN evenement e ON e.reference = COALESCE(b.evenement, v.evenement)
            WHERE t.transactionstate = false AND (:year IS NULL OR extract(year FROM e.ddate) = :year)
              AND t.datetransaction IS NOT NULL
            GROUP BY t.datetransaction ORDER BY t.datetransaction
            """, nativeQuery = true)
    List<RejetJourProjection> rejetsParJour(@Param("year") Integer year);

    @Query(value = """
            SELECT t.codebarre AS "codebarre", e.titre AS "eventTitle", t.porte AS "porte",
                   t.heuretransaction AS "dateTime", t.description AS "description"
            FROM tturnstile t
            LEFT JOIN billet b  ON b.numeroserie = t.billet
            LEFT JOIN voucher v ON v.numeroserie = t.voucher
            LEFT JOIN evenement e ON e.reference = COALESCE(b.evenement, v.evenement)
            WHERE t.transactionstate = false AND (:year IS NULL OR extract(year FROM e.ddate) = :year)
            ORDER BY t.heuretransaction DESC NULLS LAST
            LIMIT 2000
            """, nativeQuery = true)
    List<RejetScanProjection> rejetsScans(@Param("year") Integer year);
}
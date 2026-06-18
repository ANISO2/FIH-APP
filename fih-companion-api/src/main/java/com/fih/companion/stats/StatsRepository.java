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

    /** Recette résumé: revenue (TND) per event, split Billet / Voucher / Kit. */
    @Query(value = """
            SELECT e.reference AS "eventId", e.titre AS "eventTitle", e.ddate AS "eventDate",
                   COALESCE(SUM(g.counterbillet  * g.prix), 0) AS "billet",
                   COALESCE(SUM(g.countervoucher * g.prix), 0) AS "voucher",
                   COALESCE(SUM(g.counterkit     * g.prix), 0) AS "kit",
                   COALESCE(SUM((g.counterbillet + g.countervoucher + g.counterkit) * g.prix), 0) AS "total"
            FROM evenement e
            JOIN generation g ON g.evenement = e.reference
            WHERE (:year IS NULL OR extract(year FROM e.ddate) = :year)
            GROUP BY e.reference, e.titre, e.ddate
            ORDER BY "total" DESC, e.ddate
            """, nativeQuery = true)
    List<RecetteSummaryProjection> recetteSummary(@Param("year") Integer year);

    /** Recette détaillée: one row per (event x model) generation. */
    @Query(value = """
            SELECT e.reference AS "eventId", e.titre AS "eventTitle", e.ddate AS "eventDate",
                   m.reference AS "modelId", m.modele AS "modelName",
                   g.prix AS "montant",
                   g.stockvoucher                       AS "voucherGeneration",
                   g.countervoucher                     AS "voucherVente",
                   (g.stockvoucher - g.countervoucher)  AS "voucherReste",
                   g.stockbillet                        AS "billetGeneration",
                   g.counterbillet                      AS "billetVente",
                   (g.stockbillet - g.counterbillet)    AS "billetReste",
                   g.counterkit                         AS "kitGeneration",
                   g.counterkit                         AS "kitVente",
                   0                                     AS "kitReste",
                   (g.counterbillet + g.countervoucher + g.counterkit)            AS "total",
                   (g.counterbillet + g.countervoucher + g.counterkit) * g.prix   AS "recetteTnd"
            FROM generation g
            JOIN evenement e ON e.reference = g.evenement
            JOIN modelebillet m ON m.reference = g.modelebillet
            WHERE (:year IS NULL OR extract(year FROM e.ddate) = :year)
            ORDER BY e.ddate, e.titre, m.modele
            """, nativeQuery = true)
    List<RecetteDetailProjection> recetteDetail(@Param("year") Integer year);
}

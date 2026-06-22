package com.fih.companion.repository;

import com.fih.companion.domain.BadgeAffectation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

/**
 * Read-WRITE repository — the only one in the app that can write.
 *
 * It is bound to {@link BadgeAffectation}, which maps the app-owned
 * badge_affectation table. The database role grants INSERT/UPDATE/DELETE on THAT
 * table only, so even though JpaRepository exposes save()/delete(), those calls
 * can only ever affect badge_affectation; any attempt to write a legacy table is
 * still rejected by the role itself. The id type is String (the billet
 * numeroserie).
 */
public interface BadgeAffectationRepository extends JpaRepository<BadgeAffectation, String> {

    /**
     * Change A duplicate base-name detection. True when this exact name, OR a lot
     * name derived from it (baseName-01, baseName-02, …), already exists. Case-
     * insensitive so "Anis" and "ANIS" are treated as the same base.
     */
    @Query("""
            SELECT count(b) > 0 FROM BadgeAffectation b
            WHERE lower(b.affecteeA) = lower(:base)
               OR lower(b.affecteeA) LIKE lower(concat(:base, '-%'))
            """)
    boolean baseNameUsed(@Param("base") String base);

    /**
     * Change A — every stored name that belongs to a base, i.e. the bare base
     * itself OR anything of the form "base-…". Case-insensitive so "anis" and
     * "ANIS" count as the same base. The service scans these to find the highest
     * "base-NN" number already used and continues from there (max + 1). We return
     * the raw strings and parse the trailing number in Java, which keeps the query
     * simple and database-portable (no regex in JPQL).
     */
    @Query("""
            SELECT b.affecteeA FROM BadgeAffectation b
            WHERE lower(b.affecteeA) = lower(:base)
               OR lower(b.affecteeA) LIKE lower(concat(:base, '-%'))
            """)
    List<String> findNamesForBase(@Param("base") String base);

    /**
     * §6 — stamp printed_at on serials that already have a name. We only update
     * existing rows (printing never creates a name), so missing serials are
     * silently skipped. @Modifying tells Spring this query writes rather than
     * reads; it still only ever touches our own table.
     */
    @Modifying
    @Query("UPDATE BadgeAffectation b SET b.printedAt = :ts WHERE b.numeroserie IN :serials")
    int markPrinted(@Param("serials") Collection<String> serials, @Param("ts") LocalDateTime ts);
}

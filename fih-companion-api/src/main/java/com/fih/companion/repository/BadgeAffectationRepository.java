package com.fih.companion.repository;

import com.fih.companion.domain.BadgeAffectation;
import org.springframework.data.jpa.repository.JpaRepository;

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
}

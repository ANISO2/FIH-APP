package com.fih.companion.evenement;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data repository. We only ever use the read methods (findAll, findById,
 * count). The save/delete methods exist on the interface but are never called,
 * and would be rejected by the fih_ro role anyway.
 */
public interface EvenementRepository extends JpaRepository<Evenement, Integer> {
}

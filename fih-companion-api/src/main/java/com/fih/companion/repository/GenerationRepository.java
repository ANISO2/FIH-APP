package com.fih.companion.repository;

import com.fih.companion.domain.Generation;
import com.fih.companion.domain.GenerationId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GenerationRepository extends JpaRepository<Generation, GenerationId> {
}

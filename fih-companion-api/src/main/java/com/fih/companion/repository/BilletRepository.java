package com.fih.companion.repository;

import com.fih.companion.domain.Billet;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BilletRepository extends JpaRepository<Billet, String> {

    /** Uses the existing DB index on codebarre. */
    Optional<Billet> findByCodebarre(String codebarre);

    Optional<Billet> findByNumeroserie(String numeroserie);
}

package com.fih.companion.repository;

import com.fih.companion.domain.Voucher;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface VoucherRepository extends JpaRepository<Voucher, String> {

    /** Uses the existing DB index on codebarre. */
    Optional<Voucher> findByCodebarre(String codebarre);

    Optional<Voucher> findByNumeroserie(String numeroserie);
}

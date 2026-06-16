package com.fih.companion.repository;

import com.fih.companion.domain.Holder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface HolderRepository extends JpaRepository<Holder, Integer> {

    Optional<Holder> findByBillet(String billetNumeroserie);
}

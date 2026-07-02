package com.fih.companion.repository;

import com.fih.companion.domain.ModeleBillet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ModeleBilletRepository extends JpaRepository<ModeleBillet, Integer> {

    @Query(value = "SELECT reference FROM modelebillet WHERE vente = true", nativeQuery = true)
    List<Integer> findPaidModelReferences();
}

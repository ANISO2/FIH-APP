package com.fih.companion.evenement;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;


public interface EvenementRepository extends JpaRepository<Evenement, Integer> {


    @Query(value = "SELECT * FROM evenement e ORDER BY e.ddate",
            nativeQuery = true)
    List<Evenement> findVisible();
}

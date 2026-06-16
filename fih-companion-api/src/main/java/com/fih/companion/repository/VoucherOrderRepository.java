package com.fih.companion.repository;

import com.fih.companion.domain.VoucherOrder;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VoucherOrderRepository extends JpaRepository<VoucherOrder, Integer> {
}

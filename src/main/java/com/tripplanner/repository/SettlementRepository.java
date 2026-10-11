package com.tripplanner.repository;

import com.tripplanner.entity.Settlement;
import com.tripplanner.entity.Trip;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SettlementRepository extends JpaRepository<Settlement, Long> {
    List<Settlement> findByTripOrderByCreatedAtDesc(Trip trip);
}

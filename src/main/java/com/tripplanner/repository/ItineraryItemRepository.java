package com.tripplanner.repository;

import com.tripplanner.entity.ItineraryItem;
import com.tripplanner.entity.Trip;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ItineraryItemRepository extends JpaRepository<ItineraryItem, Long> {
    List<ItineraryItem> findByTripOrderByActivityDateAscStartTimeAsc(Trip trip);
}

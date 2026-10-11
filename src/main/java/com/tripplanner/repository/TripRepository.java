package com.tripplanner.repository;

import com.tripplanner.entity.Trip;
import com.tripplanner.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TripRepository extends JpaRepository<Trip, Long> {
    List<Trip> findByCreatedBy(User createdBy);
}

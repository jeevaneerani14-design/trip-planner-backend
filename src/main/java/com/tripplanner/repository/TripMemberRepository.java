package com.tripplanner.repository;

import com.tripplanner.entity.Trip;
import com.tripplanner.entity.TripMember;
import com.tripplanner.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TripMemberRepository extends JpaRepository<TripMember, Long> {
    List<TripMember> findByTrip(Trip trip);
    Optional<TripMember> findByTripAndUser(Trip trip, User user);
    boolean existsByTripAndUser(Trip trip, User user);
    void deleteByTripAndUser(Trip trip, User user);
}

package com.tripplanner.repository;

import com.tripplanner.entity.Expense;
import com.tripplanner.entity.Trip;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ExpenseRepository extends JpaRepository<Expense, Long> {
    List<Expense> findByTripOrderByExpenseDateDesc(Trip trip);
    List<Expense> findByTripInOrderByExpenseDateDesc(List<Trip> trips);
}

package com.tripplanner.repository;

import com.tripplanner.entity.Expense;
import com.tripplanner.entity.ExpenseParticipant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ExpenseParticipantRepository extends JpaRepository<ExpenseParticipant, Long> {
    List<ExpenseParticipant> findByExpense(Expense expense);
}

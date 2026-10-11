package com.tripplanner.service;

import com.tripplanner.dto.response.DashboardSummaryDto;
import com.tripplanner.dto.response.ExpenseDto;
import com.tripplanner.dto.response.TripDto;
import com.tripplanner.entity.Expense;
import com.tripplanner.entity.Trip;
import com.tripplanner.entity.User;
import com.tripplanner.repository.ExpenseRepository;
import com.tripplanner.repository.TripMemberRepository;
import com.tripplanner.repository.TripRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

@Service
public class DashboardService {
    private final TripRepository tripRepository;
    private final TripMemberRepository tripMemberRepository;
    private final ExpenseRepository expenseRepository;
    private final AuthService authService;

    public DashboardService(TripRepository tripRepository, TripMemberRepository tripMemberRepository,
                            ExpenseRepository expenseRepository, AuthService authService) {
        this.tripRepository = tripRepository;
        this.tripMemberRepository = tripMemberRepository;
        this.expenseRepository = expenseRepository;
        this.authService = authService;
    }

    @Transactional(readOnly = true)
    public DashboardSummaryDto getSummary() {
        User current = authService.getCurrentUserEntity();
        List<Trip> trips = tripRepository.findByCreatedBy(current);
        BigDecimal totalExpenditure = BigDecimal.ZERO;
        for (Trip trip : trips) {
            for (Expense expense : expenseRepository.findByTripOrderByExpenseDateDesc(trip)) {
                totalExpenditure = totalExpenditure.add(expense.getAmount());
            }
        }

        List<TripDto> recentTrips = trips.stream()
                .sorted(Comparator.comparing(Trip::getCreatedAt).reversed())
                .limit(5)
                .map(this::tripToDto)
                .toList();

        List<ExpenseDto> recentExpenses = trips.stream()
                .flatMap(trip -> expenseRepository.findByTripOrderByExpenseDateDesc(trip).stream())
                .sorted(Comparator.comparing(Expense::getExpenseDate).reversed())
                .limit(10)
                .map(this::expenseToDto)
                .toList();

        DashboardSummaryDto summary = new DashboardSummaryDto();
        summary.setTripCount(trips.size());
        summary.setUpcomingTripCount((int) trips.stream().filter(trip -> trip.getStartDate() != null && trip.getStartDate().isAfter(LocalDate.now())).count());
        summary.setTotalExpenditure(totalExpenditure);
        summary.setRecentTrips(recentTrips);
        summary.setRecentExpenses(recentExpenses);
        return summary;
    }

    private TripDto tripToDto(Trip trip) {
        TripDto dto = new TripDto();
        dto.setId(trip.getId());
        dto.setName(trip.getName());
        dto.setDestination(trip.getDestination());
        dto.setStartDate(trip.getStartDate());
        dto.setEndDate(trip.getEndDate());
        dto.setDescription(trip.getDescription());
        dto.setCreatedById(trip.getCreatedBy().getId());
        dto.setCreatedByName(trip.getCreatedBy().getName());
        dto.setCreatedAt(trip.getCreatedAt());
        return dto;
    }

    private ExpenseDto expenseToDto(Expense expense) {
        ExpenseDto dto = new ExpenseDto();
        dto.setId(expense.getId());
        dto.setTitle(expense.getTitle());
        dto.setTripName(expense.getTrip().getName());
        dto.setAmount(expense.getAmount());
        dto.setCategory(expense.getCategory());
        dto.setDescription(expense.getDescription());
        dto.setExpenseDate(expense.getExpenseDate());
        dto.setSplitType(expense.getSplitType());
        dto.setPaidByUserId(expense.getPaidBy().getId());
        dto.setPaidByUserName(expense.getPaidBy().getName());
        dto.setCreatedByUserId(expense.getCreatedBy().getId());
        dto.setCreatedAt(expense.getCreatedAt());
        dto.setUpdatedAt(expense.getUpdatedAt());
        return dto;
    }
}

package com.tripplanner;

import com.tripplanner.dto.response.DashboardSummaryDto;
import com.tripplanner.entity.Expense;
import com.tripplanner.entity.Trip;
import com.tripplanner.entity.TripMember;
import com.tripplanner.entity.User;
import com.tripplanner.repository.ExpenseRepository;
import com.tripplanner.repository.TripMemberRepository;
import com.tripplanner.repository.TripRepository;
import com.tripplanner.repository.UserRepository;
import com.tripplanner.service.DashboardService;
import com.tripplanner.service.ExpenseService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@ActiveProfiles("test")
class DashboardServiceTest {
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private TripRepository tripRepository;
    @Autowired
    private TripMemberRepository tripMemberRepository;
    @Autowired
    private ExpenseRepository expenseRepository;
    @Autowired
    private DashboardService dashboardService;
    @Autowired
    private ExpenseService expenseService;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void summary_includesRecentExpensesAndPayerDetails() {
        String email = "dashboard-" + System.nanoTime() + "@example.com";
        User user = userRepository.save(new User("Dashboard User", email, "password-hash"));
        Trip trip = tripRepository.save(new Trip(
                "Dashboard trip", "Goa", LocalDate.now(), LocalDate.now().plusDays(2), null, user
        ));
        tripMemberRepository.save(new TripMember(trip, user, "OWNER"));
        expenseRepository.save(new Expense(
                trip, "Lunch", new BigDecimal("125.50"), "Food", null, user,
                LocalDate.now(), "EQUAL", user
        ));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(email, "password")
        );

        DashboardSummaryDto summary = dashboardService.getSummary();

        assertEquals(1, summary.getRecentExpenses().size());
        assertEquals("Lunch", summary.getRecentExpenses().get(0).getTitle());
        assertEquals("Dashboard User", summary.getRecentExpenses().get(0).getPaidByUserName());
        assertEquals(new BigDecimal("125.50"), summary.getTotalExpenditure());
        assertEquals(1, expenseService.getAllExpensesForCurrentUser().size());
        assertEquals("Dashboard trip", expenseService.getAllExpensesForCurrentUser().get(0).getTripName());
    }
}

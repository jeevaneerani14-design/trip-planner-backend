package com.tripplanner;

import com.tripplanner.dto.request.ExpenseRequest;
import com.tripplanner.entity.Trip;
import com.tripplanner.entity.TripMember;
import com.tripplanner.entity.User;
import com.tripplanner.repository.TripMemberRepository;
import com.tripplanner.repository.TripRepository;
import com.tripplanner.repository.UserRepository;
import com.tripplanner.service.AuthService;
import com.tripplanner.service.ExpenseService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ExpenseServiceTest {
    @Autowired
    private AuthService authService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private TripRepository tripRepository;
    @Autowired
    private TripMemberRepository tripMemberRepository;
    @Autowired
    private ExpenseService expenseService;

    private User userOne;
    private User userTwo;
    private User userThree;
    private Trip trip;

    @BeforeEach
    void setUp() {
        userOne = userRepository.save(new User("Alice", "alice@example.com", "pass12345"));
        userTwo = userRepository.save(new User("Bob", "bob@example.com", "pass12345"));
        userThree = userRepository.save(new User("Cara", "cara@example.com", "pass12345"));
        trip = tripRepository.save(new Trip("Goa Trip", "Goa", LocalDate.now(), LocalDate.now().plusDays(4), "Beach trip", userOne));
        tripMemberRepository.save(new TripMember(trip, userOne, "OWNER"));
        tripMemberRepository.save(new TripMember(trip, userTwo, "MEMBER"));
        tripMemberRepository.save(new TripMember(trip, userThree, "MEMBER"));

        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(userOne.getEmail(), "pass12345"));
    }

    @Test
    void equalSplit_shouldCreateDeterministicShares() {
        ExpenseRequest request = new ExpenseRequest();
        request.setTitle("Dinner");
        request.setAmount(new BigDecimal("1000.00"));
        request.setCategory("Food");
        request.setExpenseDate(LocalDate.now());
        request.setPaidByUserId(userOne.getId());
        request.setSplitType("EQUAL");
        request.setParticipantIds(List.of(userOne.getId(), userTwo.getId(), userThree.getId()));

        var expense = expenseService.createExpense(trip.getId(), request);

        assertEquals(3, expense.getParticipants().size());
        assertEquals(new BigDecimal("333.34"), expense.getParticipants().get(0).getShareAmount());
        assertEquals(new BigDecimal("333.33"), expense.getParticipants().get(1).getShareAmount());
        assertEquals(new BigDecimal("333.33"), expense.getParticipants().get(2).getShareAmount());
    }

    @Test
    void customSplit_shouldRejectInvalidAmount() {
        ExpenseRequest request = new ExpenseRequest();
        request.setTitle("Taxi");
        request.setAmount(new BigDecimal("100.00"));
        request.setCategory("Transportation");
        request.setExpenseDate(LocalDate.now());
        request.setPaidByUserId(userOne.getId());
        request.setSplitType("CUSTOM");
        request.setParticipantIds(List.of(userOne.getId(), userTwo.getId()));
        request.setCustomShares(List.of(new BigDecimal("50.00"), new BigDecimal("40.00")));

        assertThrows(RuntimeException.class, () -> expenseService.createExpense(trip.getId(), request));
    }

    @Test
    void payerNotInParticipants_shouldStillBeAllowed() {
        ExpenseRequest request = new ExpenseRequest();
        request.setTitle("Ticket");
        request.setAmount(new BigDecimal("600.00"));
        request.setCategory("Tickets and activities");
        request.setExpenseDate(LocalDate.now());
        request.setPaidByUserId(userOne.getId());
        request.setSplitType("EQUAL");
        request.setParticipantIds(List.of(userTwo.getId(), userThree.getId()));

        var expense = expenseService.createExpense(trip.getId(), request);
        assertEquals(2, expense.getParticipants().size());
        assertEquals(new BigDecimal("300.00"), expense.getParticipants().get(0).getShareAmount());
    }
}

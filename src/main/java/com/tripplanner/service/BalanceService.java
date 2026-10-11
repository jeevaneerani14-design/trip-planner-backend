package com.tripplanner.service;

import com.tripplanner.dto.response.BalanceEntryDto;
import com.tripplanner.entity.Expense;
import com.tripplanner.entity.ExpenseParticipant;
import com.tripplanner.entity.Trip;
import com.tripplanner.entity.TripMember;
import com.tripplanner.entity.User;
import com.tripplanner.exception.BadRequestException;
import com.tripplanner.exception.ResourceNotFoundException;
import com.tripplanner.repository.ExpenseParticipantRepository;
import com.tripplanner.repository.ExpenseRepository;
import com.tripplanner.repository.TripMemberRepository;
import com.tripplanner.repository.TripRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class BalanceService {
    private final TripRepository tripRepository;
    private final TripMemberRepository tripMemberRepository;
    private final ExpenseRepository expenseRepository;
    private final ExpenseParticipantRepository participantRepository;
    private final AuthService authService;

    public BalanceService(TripRepository tripRepository, TripMemberRepository tripMemberRepository,
                          ExpenseRepository expenseRepository, ExpenseParticipantRepository participantRepository,
                          AuthService authService) {
        this.tripRepository = tripRepository;
        this.tripMemberRepository = tripMemberRepository;
        this.expenseRepository = expenseRepository;
        this.participantRepository = participantRepository;
        this.authService = authService;
    }

    @Transactional(readOnly = true)
    public List<BalanceEntryDto> getBalances(Long tripId) {
        Trip trip = tripRepository.findById(tripId).orElseThrow(() -> new ResourceNotFoundException("Trip not found"));
        ensureAccess(trip);

        Map<Long, User> usersById = tripMemberRepository.findByTrip(trip).stream()
                .map(TripMember::getUser)
                .collect(Collectors.toMap(User::getId, user -> user));

        Map<Long, BigDecimal> paidMap = new java.util.HashMap<>();
        Map<Long, BigDecimal> owedMap = new java.util.HashMap<>();

        for (Expense expense : expenseRepository.findByTripOrderByExpenseDateDesc(trip)) {
            Long payerId = expense.getPaidBy().getId();
            paidMap.put(payerId, paidMap.getOrDefault(payerId, BigDecimal.ZERO).add(expense.getAmount()));
            for (ExpenseParticipant participant : expense.getParticipants()) {
                Long userId = participant.getUser().getId();
                owedMap.put(userId, owedMap.getOrDefault(userId, BigDecimal.ZERO).add(participant.getShareAmount()));
            }
        }

        List<BalanceEntryDto> entries = new ArrayList<>();
        for (TripMember member : tripMemberRepository.findByTrip(trip)) {
            User user = member.getUser();
            BigDecimal totalPaid = paidMap.getOrDefault(user.getId(), BigDecimal.ZERO);
            BigDecimal totalOwed = owedMap.getOrDefault(user.getId(), BigDecimal.ZERO);
            BigDecimal netBalance = totalPaid.subtract(totalOwed);
            BalanceEntryDto dto = new BalanceEntryDto();
            dto.setUserId(user.getId());
            dto.setUserName(user.getName());
            dto.setTotalPaid(totalPaid);
            dto.setTotalOwed(totalOwed);
            dto.setNetBalance(netBalance);
            entries.add(dto);
        }

        return entries;
    }

    private void ensureAccess(Trip trip) {
        User current = authService.getCurrentUserEntity();
        boolean allowed = trip.getCreatedBy().getId().equals(current.getId())
                || tripMemberRepository.findByTripAndUser(trip, current).isPresent();
        if (!allowed) {
            throw new BadRequestException("You are not authorized to access this trip");
        }
    }
}

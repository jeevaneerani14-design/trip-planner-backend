package com.tripplanner.service;

import com.tripplanner.dto.request.ExpenseRequest;
import com.tripplanner.dto.response.ExpenseDto;
import com.tripplanner.dto.response.ExpenseParticipantDto;
import com.tripplanner.entity.Expense;
import com.tripplanner.entity.ExpenseParticipant;
import com.tripplanner.entity.Trip;
import com.tripplanner.entity.User;
import com.tripplanner.exception.BadRequestException;
import com.tripplanner.exception.ResourceNotFoundException;
import com.tripplanner.repository.ExpenseParticipantRepository;
import com.tripplanner.repository.ExpenseRepository;
import com.tripplanner.repository.TripMemberRepository;
import com.tripplanner.repository.TripRepository;
import com.tripplanner.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

@Service
public class ExpenseService {
    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2);
    private static final int MONEY_SCALE = 2;

    private final TripRepository tripRepository;
    private final UserRepository userRepository;
    private final TripMemberRepository tripMemberRepository;
    private final ExpenseRepository expenseRepository;
    private final ExpenseParticipantRepository expenseParticipantRepository;
    private final AuthService authService;

    public ExpenseService(TripRepository tripRepository, UserRepository userRepository,
                          TripMemberRepository tripMemberRepository, ExpenseRepository expenseRepository,
                          ExpenseParticipantRepository expenseParticipantRepository, AuthService authService) {
        this.tripRepository = tripRepository;
        this.userRepository = userRepository;
        this.tripMemberRepository = tripMemberRepository;
        this.expenseRepository = expenseRepository;
        this.expenseParticipantRepository = expenseParticipantRepository;
        this.authService = authService;
    }

    @Transactional(readOnly = true)
    public List<ExpenseDto> getExpenses(Long tripId) {
        Trip trip = tripRepository.findById(tripId).orElseThrow(() -> new ResourceNotFoundException("Trip not found"));
        tripAccessAllowed(trip);
        return expenseRepository.findByTripOrderByExpenseDateDesc(trip).stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public List<ExpenseDto> getAllExpensesForCurrentUser() {
        User current = authService.getCurrentUserEntity();
        List<Trip> trips = tripRepository.findByCreatedBy(current);
        if (trips.isEmpty()) {
            return List.of();
        }
        return expenseRepository.findByTripInOrderByExpenseDateDesc(trips).stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public ExpenseDto getExpense(Long tripId, Long expenseId) {
        Trip trip = tripRepository.findById(tripId).orElseThrow(() -> new ResourceNotFoundException("Trip not found"));
        tripAccessAllowed(trip);
        Expense expense = expenseRepository.findById(expenseId).orElseThrow(() -> new ResourceNotFoundException("Expense not found"));
        if (!expense.getTrip().getId().equals(tripId)) {
            throw new BadRequestException("Expense does not belong to this trip");
        }
        return toDto(expense);
    }

    @Transactional
    public ExpenseDto createExpense(Long tripId, ExpenseRequest request) {
        Trip trip = tripRepository.findById(tripId).orElseThrow(() -> new ResourceNotFoundException("Trip not found"));
        tripAccessAllowed(trip);
        validateExpenseRequest(trip, request);

        User paidBy = userRepository.findById(request.getPaidByUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Payer not found"));
        ensureUserInTrip(trip, paidBy);

        Expense expense = new Expense();
        expense.setTrip(trip);
        expense.setTitle(request.getTitle().trim());
        expense.setAmount(request.getAmount().setScale(MONEY_SCALE, RoundingMode.HALF_UP));
        expense.setCategory(request.getCategory());
        expense.setDescription(request.getDescription());
        expense.setPaidBy(paidBy);
        expense.setExpenseDate(request.getExpenseDate() == null ? LocalDate.now() : request.getExpenseDate());
        expense.setSplitType(request.getSplitType());
        expense.setCreatedBy(authService.getCurrentUserEntity());
        Expense saved = expenseRepository.save(expense);

        List<User> participants = resolveParticipantUsers(trip, request.getParticipantIds());
        List<BigDecimal> shares = buildShares(request, participants, saved.getAmount());
        for (int i = 0; i < participants.size(); i++) {
            ExpenseParticipant participant = new ExpenseParticipant(saved, participants.get(i), shares.get(i).setScale(MONEY_SCALE, RoundingMode.HALF_UP));
            saved.getParticipants().add(participant);
        }
        expenseRepository.save(saved);
        return toDto(saved);
    }

    @Transactional
    public ExpenseDto updateExpense(Long tripId, Long expenseId, ExpenseRequest request) {
        Trip trip = tripRepository.findById(tripId).orElseThrow(() -> new ResourceNotFoundException("Trip not found"));
        tripAccessAllowed(trip);
        Expense expense = expenseRepository.findById(expenseId).orElseThrow(() -> new ResourceNotFoundException("Expense not found"));
        if (!expense.getTrip().getId().equals(tripId)) {
            throw new BadRequestException("Expense does not belong to this trip");
        }
        validateExpenseRequest(trip, request);

        User paidBy = userRepository.findById(request.getPaidByUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Payer not found"));
        ensureUserInTrip(trip, paidBy);

        expense.setTitle(request.getTitle().trim());
        expense.setAmount(request.getAmount().setScale(MONEY_SCALE, RoundingMode.HALF_UP));
        expense.setCategory(request.getCategory());
        expense.setDescription(request.getDescription());
        expense.setPaidBy(paidBy);
        expense.setExpenseDate(request.getExpenseDate());
        expense.setSplitType(request.getSplitType());

        expense.getParticipants().clear();
        List<User> participants = resolveParticipantUsers(trip, request.getParticipantIds());
        List<BigDecimal> shares = buildShares(request, participants, expense.getAmount());
        for (int i = 0; i < participants.size(); i++) {
            expense.getParticipants().add(new ExpenseParticipant(expense, participants.get(i), shares.get(i).setScale(MONEY_SCALE, RoundingMode.HALF_UP)));
        }
        return toDto(expenseRepository.save(expense));
    }

    @Transactional
    public void deleteExpense(Long tripId, Long expenseId) {
        Trip trip = tripRepository.findById(tripId).orElseThrow(() -> new ResourceNotFoundException("Trip not found"));
        tripAccessAllowed(trip);
        Expense expense = expenseRepository.findById(expenseId).orElseThrow(() -> new ResourceNotFoundException("Expense not found"));
        if (!expense.getTrip().getId().equals(tripId)) {
            throw new BadRequestException("Expense does not belong to this trip");
        }
        expenseRepository.delete(expense);
    }

    private void validateExpenseRequest(Trip trip, ExpenseRequest request) {
        if (request.getSplitType() == null || request.getSplitType().isBlank()) {
            throw new BadRequestException("Split type is required");
        }

        if (request.getParticipantIds() == null || request.getParticipantIds().isEmpty()) {
            throw new BadRequestException("At least one participant is required");
        }

        List<User> participants = resolveParticipantUsers(trip, request.getParticipantIds());
        if (request.getSplitType().equalsIgnoreCase("CUSTOM")) {
            if (request.getCustomShares() == null || request.getCustomShares().size() != participants.size()) {
                throw new BadRequestException("Custom shares must match participant count");
            }
            BigDecimal total = request.getCustomShares().stream().filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add).setScale(MONEY_SCALE, RoundingMode.HALF_UP);
            if (request.getAmount().compareTo(total) != 0) {
                throw new BadRequestException("Custom shares must sum to the expense total");
            }
            if (request.getCustomShares().stream().anyMatch(share -> share != null && share.compareTo(BigDecimal.ZERO) < 0)) {
                throw new BadRequestException("Shares cannot be negative");
            }
        }
    }

    private List<User> resolveParticipantUsers(Trip trip, List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            throw new BadRequestException("Participants are required");
        }
        List<User> users = new ArrayList<>();
        for (Long userId : userIds) {
            User user = userRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User not found"));
            ensureUserInTrip(trip, user);
            if (users.stream().anyMatch(existing -> existing.getId().equals(user.getId()))) {
                throw new BadRequestException("Duplicate participant selected");
            }
            users.add(user);
        }
        return users;
    }

    private List<BigDecimal> buildShares(ExpenseRequest request, List<User> participants, BigDecimal totalAmount) {
        List<BigDecimal> shares = new ArrayList<>();
        if (request.getSplitType().equalsIgnoreCase("EQUAL")) {
            int participantCount = participants.size();
            BigDecimal each = totalAmount.divide(BigDecimal.valueOf(participantCount), 2, RoundingMode.HALF_UP);
            BigDecimal remainder = totalAmount.subtract(each.multiply(BigDecimal.valueOf(participantCount - 1)));
            for (int i = 0; i < participantCount; i++) {
                shares.add(i == 0 ? remainder : each);
            }
            return shares;
        }
        if (request.getSplitType().equalsIgnoreCase("CUSTOM")) {
            for (BigDecimal share : request.getCustomShares()) {
                shares.add(share.setScale(MONEY_SCALE, RoundingMode.HALF_UP));
            }
            return shares;
        }
        if (request.getSplitType().equalsIgnoreCase("SELECTIVE")) {
            return buildEqualShares(totalAmount, participants.size());
        }
        throw new BadRequestException("Unsupported split type");
    }

    private List<BigDecimal> buildEqualShares(BigDecimal totalAmount, int count) {
        BigDecimal base = totalAmount.divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP);
        BigDecimal remainder = totalAmount.subtract(base.multiply(BigDecimal.valueOf(count - 1)));
        List<BigDecimal> shares = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            shares.add(i == 0 ? remainder : base);
        }
        return shares;
    }

    private void ensureUserInTrip(Trip trip, User user) {
        if (!tripMemberRepository.existsByTripAndUser(trip, user)) {
            throw new BadRequestException("User is not a member of this trip");
        }
    }

    private void tripAccessAllowed(Trip trip) {
        User current = authService.getCurrentUserEntity();
        boolean allowed = trip.getCreatedBy().getId().equals(current.getId()) || tripMemberRepository.findByTripAndUser(trip, current).isPresent();
        if (!allowed) {
            throw new BadRequestException("You are not authorized to access this trip");
        }
    }

    private ExpenseDto toDto(Expense expense) {
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
        List<ExpenseParticipantDto> participantDtos = expense.getParticipants().stream()
                .sorted(Comparator.comparing(p -> p.getUser().getName()))
                .map(this::toParticipantDto)
                .toList();
        dto.setParticipants(participantDtos);
        return dto;
    }

    private ExpenseParticipantDto toParticipantDto(ExpenseParticipant participant) {
        ExpenseParticipantDto dto = new ExpenseParticipantDto();
        dto.setId(participant.getId());
        dto.setUserId(participant.getUser().getId());
        dto.setUserName(participant.getUser().getName());
        dto.setShareAmount(participant.getShareAmount());
        return dto;
    }
}

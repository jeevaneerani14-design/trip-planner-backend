package com.tripplanner.service;

import com.tripplanner.dto.response.BalanceEntryDto;
import com.tripplanner.dto.response.SettlementDto;
import com.tripplanner.entity.Settlement;
import com.tripplanner.entity.Trip;
import com.tripplanner.entity.User;
import com.tripplanner.exception.BadRequestException;
import com.tripplanner.exception.ResourceNotFoundException;
import com.tripplanner.repository.SettlementRepository;
import com.tripplanner.repository.TripMemberRepository;
import com.tripplanner.repository.TripRepository;
import com.tripplanner.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Service
public class SettlementService {
    private final TripRepository tripRepository;
    private final SettlementRepository settlementRepository;
    private final TripMemberRepository tripMemberRepository;
    private final UserRepository userRepository;
    private final AuthService authService;
    private final BalanceService balanceService;

    public SettlementService(TripRepository tripRepository, SettlementRepository settlementRepository,
                             TripMemberRepository tripMemberRepository, UserRepository userRepository,
                             AuthService authService, BalanceService balanceService) {
        this.tripRepository = tripRepository;
        this.settlementRepository = settlementRepository;
        this.tripMemberRepository = tripMemberRepository;
        this.userRepository = userRepository;
        this.authService = authService;
        this.balanceService = balanceService;
    }

    public List<SettlementDto> getSettlements(Long tripId) {
        Trip trip = tripRepository.findById(tripId).orElseThrow(() -> new ResourceNotFoundException("Trip not found"));
        ensureAccess(trip);
        return settlementRepository.findByTripOrderByCreatedAtDesc(trip).stream().map(this::toDto).toList();
    }

    public List<SettlementDto> generateSuggestions(Long tripId) {
        Trip trip = tripRepository.findById(tripId).orElseThrow(() -> new ResourceNotFoundException("Trip not found"));
        ensureAccess(trip);

        List<BalanceEntryDto> balances = balanceService.getBalances(tripId);
        List<SettlementDto> suggestions = new ArrayList<>();

        List<Map.Entry<Long, BigDecimal>> debtors = balances.stream()
                .filter(balance -> balance.getNetBalance().compareTo(BigDecimal.ZERO) < 0)
                .map(balance -> Map.entry(balance.getUserId(), balance.getNetBalance().abs().setScale(2, RoundingMode.HALF_UP)))
                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                .toList();

        List<Map.Entry<Long, BigDecimal>> creditors = balances.stream()
                .filter(balance -> balance.getNetBalance().compareTo(BigDecimal.ZERO) > 0)
                .map(balance -> Map.entry(balance.getUserId(), balance.getNetBalance().setScale(2, RoundingMode.HALF_UP)))
                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                .toList();

        int i = 0;
        int j = 0;
        while (i < debtors.size() && j < creditors.size()) {
            Long debtorId = debtors.get(i).getKey();
            Long creditorId = creditors.get(j).getKey();
            BigDecimal debtorAmount = debtors.get(i).getValue();
            BigDecimal creditorAmount = creditors.get(j).getValue();

            BigDecimal amount = debtorAmount.min(creditorAmount).setScale(2, RoundingMode.HALF_UP);
            if (amount.compareTo(BigDecimal.ZERO) > 0) {
                User debtor = userRepository.findById(debtorId).orElseThrow();
                User creditor = userRepository.findById(creditorId).orElseThrow();
                SettlementDto suggestion = new SettlementDto();
                suggestion.setFromUserId(debtorId);
                suggestion.setFromUserName(debtor.getName());
                suggestion.setToUserId(creditorId);
                suggestion.setToUserName(creditor.getName());
                suggestion.setAmount(amount);
                suggestion.setStatus("PENDING");
                suggestions.add(suggestion);
            }

            if (debtorAmount.compareTo(creditorAmount) <= 0) {
                i++;
            } else {
                debtors.set(i, Map.entry(debtorId, debtorAmount.subtract(creditorAmount)));
                j++;
            }

            if (debtorAmount.compareTo(creditorAmount) >= 0) {
                j++;
            } else {
                creditors.set(j, Map.entry(creditorId, creditorAmount.subtract(debtorAmount)));
                i++;
            }
        }

        return suggestions;
    }

    @Transactional
    public SettlementDto confirmSettlement(Long tripId, Long settlementId) {
        Trip trip = tripRepository.findById(tripId).orElseThrow(() -> new ResourceNotFoundException("Trip not found"));
        ensureAccess(trip);
        Settlement settlement = settlementRepository.findById(settlementId)
                .orElseThrow(() -> new ResourceNotFoundException("Settlement not found"));
        if (!settlement.getTrip().getId().equals(tripId)) {
            throw new BadRequestException("Settlement does not belong to this trip");
        }
        if ("PAID".equalsIgnoreCase(settlement.getStatus())) {
            throw new BadRequestException("This settlement has already been confirmed");
        }
        settlement.setStatus("PAID");
        settlement.setPaidAt(LocalDateTime.now());
        return toDto(settlementRepository.save(settlement));
    }

    @Transactional
    public void reverseSettlement(Long tripId, Long settlementId) {
        Trip trip = tripRepository.findById(tripId).orElseThrow(() -> new ResourceNotFoundException("Trip not found"));
        ensureAccess(trip);
        Settlement settlement = settlementRepository.findById(settlementId)
                .orElseThrow(() -> new ResourceNotFoundException("Settlement not found"));
        if (!settlement.getTrip().getId().equals(tripId)) {
            throw new BadRequestException("Settlement does not belong to this trip");
        }
        settlement.setStatus("REVERSED");
        settlement.setPaidAt(LocalDateTime.now());
        settlementRepository.save(settlement);
    }

    private SettlementDto toDto(Settlement settlement) {
        SettlementDto dto = new SettlementDto();
        dto.setId(settlement.getId());
        dto.setFromUserId(settlement.getFromUser().getId());
        dto.setFromUserName(settlement.getFromUser().getName());
        dto.setToUserId(settlement.getToUser().getId());
        dto.setToUserName(settlement.getToUser().getName());
        dto.setAmount(settlement.getAmount());
        dto.setStatus(settlement.getStatus());
        dto.setCreatedAt(settlement.getCreatedAt());
        dto.setPaidAt(settlement.getPaidAt());
        return dto;
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

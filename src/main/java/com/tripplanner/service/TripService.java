package com.tripplanner.service;

import com.tripplanner.dto.request.TripRequest;
import com.tripplanner.dto.response.TripDto;
import com.tripplanner.entity.Trip;
import com.tripplanner.entity.TripMember;
import com.tripplanner.entity.User;
import com.tripplanner.exception.BadRequestException;
import com.tripplanner.exception.ResourceNotFoundException;
import com.tripplanner.repository.TripMemberRepository;
import com.tripplanner.repository.TripRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

@Service
public class TripService {
    private final TripRepository tripRepository;
    private final TripMemberRepository tripMemberRepository;
    private final AuthService authService;
    private final ItinerarySuggestionService itinerarySuggestionService;

    public TripService(TripRepository tripRepository, TripMemberRepository tripMemberRepository, AuthService authService,
                       ItinerarySuggestionService itinerarySuggestionService) {
        this.tripRepository = tripRepository;
        this.tripMemberRepository = tripMemberRepository;
        this.authService = authService;
        this.itinerarySuggestionService = itinerarySuggestionService;
    }

    @Transactional(readOnly = true)
    public List<TripDto> getTripsForCurrentUser() {
        User user = authService.getCurrentUserEntity();
        return tripRepository.findByCreatedBy(user).stream()
                .map(this::toDto)
                .sorted(Comparator.comparing(TripDto::getStartDate, Comparator.nullsLast(LocalDate::compareTo)))
                .toList();
    }

    @Transactional
    public TripDto createTrip(TripRequest request) {
        if (request.getStartDate() != null && request.getEndDate() != null && request.getStartDate().isAfter(request.getEndDate())) {
            throw new BadRequestException("Start date cannot be after end date");
        }
        User currentUser = authService.getCurrentUserEntity();
        Trip trip = new Trip(request.getName().trim(), request.getDestination().trim(), request.getStartDate(), request.getEndDate(), request.getDescription(), currentUser);
        Trip saved = tripRepository.save(trip);

        TripMember tripMember = new TripMember(saved, currentUser, "OWNER");
        tripMemberRepository.save(tripMember);
        itinerarySuggestionService.createSuggestions(saved);
        return toDto(saved);
    }

    @Transactional(readOnly = true)
    public TripDto getTripById(Long tripId) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new ResourceNotFoundException("Trip not found"));
        authorizeTripAccess(trip);
        return toDto(trip);
    }

    public TripDto updateTrip(Long tripId, TripRequest request) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new ResourceNotFoundException("Trip not found"));
        authorizeTripAccess(trip);

        if (request.getStartDate() != null && request.getEndDate() != null && request.getStartDate().isAfter(request.getEndDate())) {
            throw new BadRequestException("Start date cannot be after end date");
        }

        trip.setName(request.getName().trim());
        trip.setDestination(request.getDestination().trim());
        trip.setStartDate(request.getStartDate());
        trip.setEndDate(request.getEndDate());
        trip.setDescription(request.getDescription());
        return toDto(tripRepository.save(trip));
    }

    @Transactional
    public void deleteTrip(Long tripId) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new ResourceNotFoundException("Trip not found"));
        authorizeTripAccess(trip);
        tripRepository.delete(trip);
    }

    public boolean isMemberOfTrip(Trip trip, User user) {
        return tripMemberRepository.findByTripAndUser(trip, user).isPresent();
    }

    public void authorizeTripAccess(Trip trip) {
        User currentUser = authService.getCurrentUserEntity();
        if (!trip.getCreatedBy().getId().equals(currentUser.getId()) && !isMemberOfTrip(trip, currentUser)) {
            throw new BadRequestException("You are not authorized to access this trip");
        }
    }

    private TripDto toDto(Trip trip) {
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
}

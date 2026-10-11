package com.tripplanner.service;

import com.tripplanner.dto.request.ItineraryRequest;
import com.tripplanner.dto.response.ItineraryItemDto;
import com.tripplanner.entity.ItineraryItem;
import com.tripplanner.entity.Trip;
import com.tripplanner.exception.BadRequestException;
import com.tripplanner.exception.ResourceNotFoundException;
import com.tripplanner.repository.ItineraryItemRepository;
import com.tripplanner.repository.TripRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ItineraryService {
    private final TripRepository tripRepository;
    private final ItineraryItemRepository itineraryItemRepository;
    private final TripService tripService;

    public ItineraryService(TripRepository tripRepository, ItineraryItemRepository itineraryItemRepository, TripService tripService) {
        this.tripRepository = tripRepository;
        this.itineraryItemRepository = itineraryItemRepository;
        this.tripService = tripService;
    }

    public List<ItineraryItemDto> getItems(Long tripId) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new ResourceNotFoundException("Trip not found"));
        tripService.authorizeTripAccess(trip);
        return itineraryItemRepository.findByTripOrderByActivityDateAscStartTimeAsc(trip).stream().map(this::toDto).toList();
    }

    public ItineraryItemDto createItem(Long tripId, ItineraryRequest request) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new ResourceNotFoundException("Trip not found"));
        tripService.authorizeTripAccess(trip);
        if (request.getActivityDate() == null) {
            throw new BadRequestException("Activity date is required");
        }
        ItineraryItem item = new ItineraryItem(
                trip,
                request.getTitle().trim(),
                request.getDescription(),
                request.getLocation(),
                request.getActivityDate(),
                request.getStartTime(),
                request.getEndTime(),
                request.getEstimatedCost() == null ? java.math.BigDecimal.ZERO : request.getEstimatedCost(),
                request.getNotes()
        );
        return toDto(itineraryItemRepository.save(item));
    }

    public ItineraryItemDto updateItem(Long tripId, Long itemId, ItineraryRequest request) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new ResourceNotFoundException("Trip not found"));
        tripService.authorizeTripAccess(trip);
        ItineraryItem item = itineraryItemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Itinerary item not found"));
        if (!item.getTrip().getId().equals(tripId)) {
            throw new BadRequestException("Item does not belong to this trip");
        }
        item.setTitle(request.getTitle().trim());
        item.setDescription(request.getDescription());
        item.setLocation(request.getLocation());
        item.setActivityDate(request.getActivityDate());
        item.setStartTime(request.getStartTime());
        item.setEndTime(request.getEndTime());
        item.setEstimatedCost(request.getEstimatedCost() == null ? java.math.BigDecimal.ZERO : request.getEstimatedCost());
        item.setNotes(request.getNotes());
        return toDto(itineraryItemRepository.save(item));
    }

    @Transactional
    public void deleteItem(Long tripId, Long itemId) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new ResourceNotFoundException("Trip not found"));
        tripService.authorizeTripAccess(trip);
        ItineraryItem item = itineraryItemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Itinerary item not found"));
        if (!item.getTrip().getId().equals(tripId)) {
            throw new BadRequestException("Item does not belong to this trip");
        }
        itineraryItemRepository.delete(item);
    }

    private ItineraryItemDto toDto(ItineraryItem item) {
        ItineraryItemDto dto = new ItineraryItemDto();
        dto.setId(item.getId());
        dto.setTitle(item.getTitle());
        dto.setDescription(item.getDescription());
        dto.setLocation(item.getLocation());
        dto.setActivityDate(item.getActivityDate());
        dto.setStartTime(item.getStartTime());
        dto.setEndTime(item.getEndTime());
        dto.setEstimatedCost(item.getEstimatedCost());
        dto.setNotes(item.getNotes());
        return dto;
    }
}

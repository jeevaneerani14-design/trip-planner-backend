package com.tripplanner.controller;

import com.tripplanner.dto.request.ItineraryRequest;
import com.tripplanner.dto.response.ItineraryItemDto;
import com.tripplanner.service.ItineraryService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/trips")
public class ItineraryController {
    private final ItineraryService itineraryService;

    public ItineraryController(ItineraryService itineraryService) {
        this.itineraryService = itineraryService;
    }

    @GetMapping("/{tripId}/itinerary")
    public ResponseEntity<List<ItineraryItemDto>> getItems(@PathVariable Long tripId) {
        return ResponseEntity.ok(itineraryService.getItems(tripId));
    }

    @PostMapping("/{tripId}/itinerary")
    public ResponseEntity<ItineraryItemDto> createItem(@PathVariable Long tripId, @Valid @RequestBody ItineraryRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(itineraryService.createItem(tripId, request));
    }

    @PutMapping("/{tripId}/itinerary/{itemId}")
    public ResponseEntity<ItineraryItemDto> updateItem(@PathVariable Long tripId, @PathVariable Long itemId, @Valid @RequestBody ItineraryRequest request) {
        return ResponseEntity.ok(itineraryService.updateItem(tripId, itemId, request));
    }

    @DeleteMapping("/{tripId}/itinerary/{itemId}")
    public ResponseEntity<Void> deleteItem(@PathVariable Long tripId, @PathVariable Long itemId) {
        itineraryService.deleteItem(tripId, itemId);
        return ResponseEntity.noContent().build();
    }
}

package com.tripplanner.controller;

import com.tripplanner.dto.request.TripRequest;
import com.tripplanner.dto.response.TripDto;
import com.tripplanner.service.TripService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class TripController {
    private final TripService tripService;

    public TripController(TripService tripService) {
        this.tripService = tripService;
    }

    @GetMapping("/trips")
    public ResponseEntity<List<TripDto>> getTrips() {
        return ResponseEntity.ok(tripService.getTripsForCurrentUser());
    }

    @PostMapping("/trips")
    public ResponseEntity<TripDto> createTrip(@Valid @RequestBody TripRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(tripService.createTrip(request));
    }

    @GetMapping("/trips/{tripId}")
    public ResponseEntity<TripDto> getTrip(@PathVariable Long tripId) {
        return ResponseEntity.ok(tripService.getTripById(tripId));
    }

    @PutMapping("/trips/{tripId}")
    public ResponseEntity<TripDto> updateTrip(@PathVariable Long tripId, @Valid @RequestBody TripRequest request) {
        return ResponseEntity.ok(tripService.updateTrip(tripId, request));
    }

    @DeleteMapping("/trips/{tripId}")
    public ResponseEntity<Void> deleteTrip(@PathVariable Long tripId) {
        tripService.deleteTrip(tripId);
        return ResponseEntity.noContent().build();
    }
}

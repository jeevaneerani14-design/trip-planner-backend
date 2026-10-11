package com.tripplanner.controller;

import com.tripplanner.dto.request.AddMemberRequest;
import com.tripplanner.dto.response.TripMemberDto;
import com.tripplanner.service.TripMemberService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/trips")
public class TripMemberController {
    private final TripMemberService tripMemberService;

    public TripMemberController(TripMemberService tripMemberService) {
        this.tripMemberService = tripMemberService;
    }

    @GetMapping("/{tripId}/members")
    public ResponseEntity<List<TripMemberDto>> getMembers(@PathVariable Long tripId) {
        return ResponseEntity.ok(tripMemberService.getMembers(tripId));
    }

    @PostMapping("/{tripId}/members")
    public ResponseEntity<TripMemberDto> addMember(@PathVariable Long tripId, @Valid @RequestBody AddMemberRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(tripMemberService.addMember(tripId, request));
    }

    @DeleteMapping("/{tripId}/members/{memberId}")
    public ResponseEntity<Void> removeMember(@PathVariable Long tripId, @PathVariable Long memberId) {
        tripMemberService.removeMember(tripId, memberId);
        return ResponseEntity.noContent().build();
    }
}

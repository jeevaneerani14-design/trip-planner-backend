package com.tripplanner.controller;

import com.tripplanner.dto.response.SettlementDto;
import com.tripplanner.service.SettlementService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/trips")
public class SettlementController {
    private final SettlementService settlementService;

    public SettlementController(SettlementService settlementService) {
        this.settlementService = settlementService;
    }

    @GetMapping("/{tripId}/settlements")
    public ResponseEntity<List<SettlementDto>> getSettlements(@PathVariable Long tripId) {
        return ResponseEntity.ok(settlementService.getSettlements(tripId));
    }

    @GetMapping("/{tripId}/settlements/suggestions")
    public ResponseEntity<List<SettlementDto>> getSuggestions(@PathVariable Long tripId) {
        return ResponseEntity.ok(settlementService.generateSuggestions(tripId));
    }

    @PostMapping("/{tripId}/settlements/{settlementId}/confirm")
    public ResponseEntity<SettlementDto> confirmSettlement(@PathVariable Long tripId, @PathVariable Long settlementId) {
        return ResponseEntity.ok(settlementService.confirmSettlement(tripId, settlementId));
    }

    @PostMapping("/{tripId}/settlements/{settlementId}/reverse")
    public ResponseEntity<Void> reverseSettlement(@PathVariable Long tripId, @PathVariable Long settlementId) {
        settlementService.reverseSettlement(tripId, settlementId);
        return ResponseEntity.noContent().build();
    }
}

package com.tripplanner.controller;

import com.tripplanner.dto.response.BalanceEntryDto;
import com.tripplanner.service.BalanceService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/trips")
public class BalanceController {
    private final BalanceService balanceService;

    public BalanceController(BalanceService balanceService) {
        this.balanceService = balanceService;
    }

    @GetMapping("/{tripId}/balances")
    public ResponseEntity<List<BalanceEntryDto>> getBalances(@PathVariable Long tripId) {
        return ResponseEntity.ok(balanceService.getBalances(tripId));
    }
}

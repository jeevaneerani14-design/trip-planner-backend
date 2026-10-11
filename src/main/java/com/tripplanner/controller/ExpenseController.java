package com.tripplanner.controller;

import com.tripplanner.dto.request.ExpenseRequest;
import com.tripplanner.dto.response.ExpenseDto;
import com.tripplanner.service.ExpenseService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/trips")
public class ExpenseController {
    private final ExpenseService expenseService;

    public ExpenseController(ExpenseService expenseService) {
        this.expenseService = expenseService;
    }

    @GetMapping("/{tripId}/expenses")
    public ResponseEntity<List<ExpenseDto>> getExpenses(@PathVariable Long tripId) {
        return ResponseEntity.ok(expenseService.getExpenses(tripId));
    }

    @PostMapping("/{tripId}/expenses")
    public ResponseEntity<ExpenseDto> createExpense(@PathVariable Long tripId, @Valid @RequestBody ExpenseRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(expenseService.createExpense(tripId, request));
    }

    @GetMapping("/{tripId}/expenses/{expenseId}")
    public ResponseEntity<ExpenseDto> getExpense(@PathVariable Long tripId, @PathVariable Long expenseId) {
        return ResponseEntity.ok(expenseService.getExpense(tripId, expenseId));
    }

    @PutMapping("/{tripId}/expenses/{expenseId}")
    public ResponseEntity<ExpenseDto> updateExpense(@PathVariable Long tripId, @PathVariable Long expenseId, @Valid @RequestBody ExpenseRequest request) {
        return ResponseEntity.ok(expenseService.updateExpense(tripId, expenseId, request));
    }

    @DeleteMapping("/{tripId}/expenses/{expenseId}")
    public ResponseEntity<Void> deleteExpense(@PathVariable Long tripId, @PathVariable Long expenseId) {
        expenseService.deleteExpense(tripId, expenseId);
        return ResponseEntity.noContent().build();
    }
}

package com.financeflow.controller;

import com.financeflow.dto.request.transaction.TransactionRequest;
import com.financeflow.dto.response.ApiResponse;
import com.financeflow.dto.response.transaction.TransactionResponse;
import com.financeflow.service.TransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/transactions")
@Tag(name = "Transactions", description = "Manage the authenticated user's transactions")
public class TransactionController {

    private final TransactionService transactionService;

    @PostMapping
    @Operation(summary = "Create a transaction")
    public ResponseEntity<ApiResponse<TransactionResponse>> create(
            @Valid @RequestBody TransactionRequest request) {
        TransactionResponse transaction = transactionService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(transaction, "Transaction created successfully"));
    }

    @GetMapping
    @Operation(summary = "Get all transactions for the current user")
    public ResponseEntity<ApiResponse<List<TransactionResponse>>> findAll() {
        return ResponseEntity.ok(ApiResponse.success(
                transactionService.findAll(), "Transactions retrieved successfully"));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a transaction by ID")
    public ResponseEntity<ApiResponse<TransactionResponse>> findById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(
                transactionService.findById(id), "Transaction retrieved successfully"));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a transaction and roll back its balance change")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        transactionService.delete(id);
        return ResponseEntity.ok(ApiResponse.success(
                null, "Transaction deleted successfully"));
    }
}

package com.financeflow.controller;

import com.financeflow.dto.request.account.AccountRequest;
import com.financeflow.dto.response.ApiResponse;
import com.financeflow.dto.response.account.AccountResponse;
import com.financeflow.service.AccountService;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/accounts")
@Tag(name = "Accounts", description = "Manage the authenticated user's financial accounts")
public class AccountController {

    private final AccountService accountService;

    @PostMapping
    @Operation(summary = "Create an account")
    public ResponseEntity<ApiResponse<AccountResponse>> create(
            @Valid @RequestBody AccountRequest request) {
        AccountResponse account = accountService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(account, "Account created successfully"));
    }

    @GetMapping
    @Operation(summary = "Get all accounts for the current user")
    public ResponseEntity<ApiResponse<List<AccountResponse>>> findAll() {
        return ResponseEntity.ok(ApiResponse.success(
                accountService.findAll(), "Accounts retrieved successfully"));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get an account by ID")
    public ResponseEntity<ApiResponse<AccountResponse>> findById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(
                accountService.findById(id), "Account retrieved successfully"));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an account")
    public ResponseEntity<ApiResponse<AccountResponse>> update(
            @PathVariable Long id,
            @Valid @RequestBody AccountRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                accountService.update(id, request), "Account updated successfully"));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete an account")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        accountService.delete(id);
        return ResponseEntity.ok(ApiResponse.success(
                null, "Account deleted successfully"));
    }
}

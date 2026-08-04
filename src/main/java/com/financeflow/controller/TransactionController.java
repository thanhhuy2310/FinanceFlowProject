package com.financeflow.controller;

import com.financeflow.dto.request.transaction.TransactionRequest;
import com.financeflow.dto.response.ApiResponse;
import com.financeflow.dto.response.transaction.TransactionResponse;
import com.financeflow.exception.BadRequestException;
import com.financeflow.service.TransactionExportService;
import com.financeflow.service.TransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/transactions")
@Tag(name = "Transactions", description = "Manage the authenticated user's transactions")
public class TransactionController {

    private final TransactionService transactionService;
    private final TransactionExportService transactionExportService;

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

    @GetMapping("/export")
    @Operation(summary = "Export transactions as CSV or Excel")
    public ResponseEntity<byte[]> export(
            @RequestParam(defaultValue = "csv") String format,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {

        if (from != null && to != null && from.isAfter(to)) {
            throw new BadRequestException("Invalid date range: 'from' must not be after 'to'");
        }

        LocalDateTime start = from == null ? null : from.atStartOfDay();
        LocalDateTime end = to == null ? null : to.plusDays(1).atStartOfDay();

        String normalizedFormat = format.toLowerCase(Locale.ROOT);
        boolean excel = normalizedFormat.equals("xlsx") || normalizedFormat.equals("excel");

        byte[] content;
        String extension;
        MediaType contentType;
        if (excel) {
            content = transactionExportService.exportXlsx(start, end);
            extension = "xlsx";
            contentType = MediaType.parseMediaType(
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        } else if (normalizedFormat.equals("csv")) {
            content = transactionExportService.exportCsv(start, end);
            extension = "csv";
            contentType = MediaType.parseMediaType("text/csv; charset=UTF-8");
        } else {
            throw new BadRequestException("Unsupported export format: " + format + " (use csv or xlsx)");
        }

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=transactions-" + LocalDate.now() + "." + extension)
                .contentType(contentType)
                .contentLength(content.length)
                .body(content);
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

package com.financeflow.controller;

import com.financeflow.dto.request.importbatch.ImportBatchRequest;
import com.financeflow.dto.response.ApiResponse;
import com.financeflow.dto.response.importbatch.ImportBatchResponse;
import com.financeflow.service.ImportBatchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/import-batches")
@Tag(name = "Import Batches", description = "Prepare CSV import batches for the current user")
public class ImportBatchController {

    private final ImportBatchService importBatchService;

    @PostMapping
    @Operation(summary = "Create an import batch")
    public ResponseEntity<ApiResponse<ImportBatchResponse>> create(
            @Valid @RequestBody ImportBatchRequest request) {
        ImportBatchResponse importBatch = importBatchService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(importBatch, "Import batch created successfully"));
    }

    @GetMapping
    @Operation(summary = "Get all import batches for the current user")
    public ResponseEntity<ApiResponse<List<ImportBatchResponse>>> findAll() {
        return ResponseEntity.ok(ApiResponse.success(
                importBatchService.findAll(), "Import batches retrieved successfully"));
    }

    @GetMapping("/template")
    @Operation(summary = "Download a CSV template showing the expected columns")
    public ResponseEntity<byte[]> downloadTemplate() {
        String template = """
                Date,Description,Amount,Type,Category,Provider,Reference
                2026-08-01,Coffee,65000,EXPENSE,Food,Highlands,
                2026-08-02,Monthly salary,15000000,INCOME,Salary,Techcombank,
                """;
        byte[] content = template.getBytes(StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=financeflow-import-template.csv")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .contentLength(content.length)
                .body(content);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get an import batch by ID")
    public ResponseEntity<ApiResponse<ImportBatchResponse>> findById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(
                importBatchService.findById(id), "Import batch retrieved successfully"));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete an import batch")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        importBatchService.delete(id);
        return ResponseEntity.ok(ApiResponse.success(
                null, "Import batch deleted successfully"));
    }

    @PostMapping("/{id}/import")
    @Operation(summary = "Import transactions from an uploaded CSV file")
    public ResponseEntity<ApiResponse<ImportBatchResponse>> importCsv(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(ApiResponse.success(
                importBatchService.importCsv(id, file), "CSV imported successfully"));
    }
}

package com.financeflow.controller;

import com.financeflow.dto.response.ApiResponse;
import com.financeflow.dto.response.provider.ProviderResponse;
import com.financeflow.service.ProviderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/providers")
@RequiredArgsConstructor
@Tag(name = "Providers", description = "System provider master data")
public class ProviderController {

    private final ProviderService providerService;

    @GetMapping
    @Operation(summary = "Get all providers")
    public ResponseEntity<ApiResponse<List<ProviderResponse>>> findAll() {
        return ResponseEntity.ok(ApiResponse.success(
                providerService.findAll(), "Providers retrieved successfully"));
    }
}

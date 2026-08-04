package com.financeflow.controller;

import com.financeflow.dto.response.ApiResponse;
import com.financeflow.dto.response.dashboard.DashboardResponse;
import com.financeflow.exception.BadRequestException;
import com.financeflow.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/dashboard")
@Tag(name = "Dashboard", description = "View financial summaries for the current user")
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping
    @Operation(summary = "Get dashboard summary, optionally filtered by date range")
    public ResponseEntity<ApiResponse<DashboardResponse>> getDashboard(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {

        if (from != null && to != null && from.isAfter(to)) {
            throw new BadRequestException("Invalid date range: 'from' must not be after 'to'");
        }

        LocalDateTime start = from == null ? null : from.atStartOfDay();
        LocalDateTime end = to == null ? null : to.plusDays(1).atStartOfDay();

        return ResponseEntity.ok(ApiResponse.success(
                dashboardService.getDashboard(start, end), "Dashboard retrieved successfully"));
    }
}

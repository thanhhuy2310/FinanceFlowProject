package com.financeflow.controller;

import com.financeflow.dto.request.rule.RuleRequest;
import com.financeflow.dto.response.ApiResponse;
import com.financeflow.dto.response.rule.RuleResponse;
import com.financeflow.service.RuleService;
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
@RequestMapping("/api/rules")
@Tag(name = "Rules", description = "Manage automatic category rules for the current user")
public class RuleController {

    private final RuleService ruleService;

    @PostMapping
    @Operation(summary = "Create a rule")
    public ResponseEntity<ApiResponse<RuleResponse>> create(
            @Valid @RequestBody RuleRequest request) {
        RuleResponse rule = ruleService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(rule, "Rule created successfully"));
    }

    @GetMapping
    @Operation(summary = "Get all rules for the current user")
    public ResponseEntity<ApiResponse<List<RuleResponse>>> findAll() {
        return ResponseEntity.ok(ApiResponse.success(
                ruleService.findAll(), "Rules retrieved successfully"));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a rule")
    public ResponseEntity<ApiResponse<RuleResponse>> update(
            @PathVariable Long id,
            @Valid @RequestBody RuleRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                ruleService.update(id, request), "Rule updated successfully"));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a rule")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        ruleService.delete(id);
        return ResponseEntity.ok(ApiResponse.success(
                null, "Rule deleted successfully"));
    }
}

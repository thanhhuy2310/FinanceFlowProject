package com.financeflow.controller;

import com.financeflow.dto.request.category.CategoryRequest;
import com.financeflow.dto.response.ApiResponse;
import com.financeflow.dto.response.category.CategoryResponse;
import com.financeflow.service.CategoryService;
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
@RequestMapping("/api/categories")
@Tag(name = "Categories", description = "Manage the authenticated user's income and expense categories")
public class CategoryController {

    private final CategoryService categoryService;

    @PostMapping
    @Operation(summary = "Create a category")
    public ResponseEntity<ApiResponse<CategoryResponse>> create(
            @Valid @RequestBody CategoryRequest request) {
        CategoryResponse category = categoryService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(category, "Category created successfully"));
    }

    @GetMapping
    @Operation(summary = "Get all categories for the current user")
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> findAll() {
        return ResponseEntity.ok(ApiResponse.success(
                categoryService.findAll(), "Categories retrieved successfully"));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a category by ID")
    public ResponseEntity<ApiResponse<CategoryResponse>> findById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(
                categoryService.findById(id), "Category retrieved successfully"));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a category")
    public ResponseEntity<ApiResponse<CategoryResponse>> update(
            @PathVariable Long id,
            @Valid @RequestBody CategoryRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                categoryService.update(id, request), "Category updated successfully"));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a category")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        categoryService.delete(id);
        return ResponseEntity.ok(ApiResponse.success(
                null, "Category deleted successfully"));
    }
}

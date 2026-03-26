package com.finance.tracker.controller.api;

import com.finance.tracker.dto.request.CategoryRequest;
import com.finance.tracker.dto.request.CategoryUpdateRequest;
import com.finance.tracker.dto.response.CategoryResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import java.util.List;
import org.springframework.http.ResponseEntity;

@Tag(name = "Categories", description = "Operations with categories")
public interface CategoryControllerApi {

    @Operation(summary = "Get category by id")
    ResponseEntity<CategoryResponse> getById(@Positive Long id);

    @Operation(summary = "Get all categories")
    ResponseEntity<List<CategoryResponse>> getAll();

    @Operation(summary = "Create a category")
    ResponseEntity<CategoryResponse> create(@Valid CategoryRequest request);

    @Operation(summary = "Update a category")
    ResponseEntity<CategoryResponse> update(@Positive Long id, @Valid CategoryUpdateRequest request);

    @Operation(summary = "Delete a category")
    ResponseEntity<Void> delete(@Positive Long id);
}

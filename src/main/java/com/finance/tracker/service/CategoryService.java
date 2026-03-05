package com.finance.tracker.service;

import com.finance.tracker.dto.request.CategoryRequest;
import com.finance.tracker.dto.response.CategoryResponse;

import java.util.List;

public interface CategoryService {
    CategoryResponse findById(Long id);

    List<CategoryResponse> findAll();

    CategoryResponse create(CategoryRequest request);

    CategoryResponse update(Long id, CategoryRequest request);

    void delete(Long id);
}

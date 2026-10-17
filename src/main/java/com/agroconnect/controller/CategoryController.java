package com.agroconnect.controller;

import com.agroconnect.dto.CategoryRequest;
import com.agroconnect.dto.CategoryResponse;
import com.agroconnect.service.CategoryService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
public class CategoryController {

    private final CategoryService service;

    public CategoryController(CategoryService service) { this.service = service; }

    // Public: used by dashboards. Only active categories.
    @GetMapping("/api/categories")
    public List<CategoryResponse> active() { return service.listActive(); }

    // Admin only (SecurityConfig protects /api/admin/**)
    @GetMapping("/api/admin/categories")
    public List<CategoryResponse> all() { return service.listAll(); }

    @PostMapping("/api/admin/categories")
    @ResponseStatus(HttpStatus.CREATED)
    public CategoryResponse create(@Valid @RequestBody CategoryRequest r) { return service.create(r); }

    @PutMapping("/api/admin/categories/{id}")
    public CategoryResponse update(@PathVariable Long id, @Valid @RequestBody CategoryRequest r) {
        return service.update(id, r);
    }
}

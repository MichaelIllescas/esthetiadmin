package com.esthetiadmin.backend.catalog.controller;

import com.esthetiadmin.backend.catalog.dto.ActiveRequest;
import com.esthetiadmin.backend.catalog.dto.CategoryRequest;
import com.esthetiadmin.backend.catalog.dto.CategoryResponse;
import com.esthetiadmin.backend.catalog.service.CategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController implements CategoryControllerDocs {

    private final CategoryService categoryService;

    @Override
    @GetMapping
    public List<CategoryResponse> findAll() {
        return categoryService.findAll();
    }

    @Override
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CategoryResponse create(@Valid @RequestBody CategoryRequest request) {
        return categoryService.create(request);
    }

    @Override
    @PutMapping("/{id}")
    public CategoryResponse update(@PathVariable Long id, @Valid @RequestBody CategoryRequest request) {
        return categoryService.update(id, request);
    }

    @Override
    @PatchMapping("/{id}/active")
    public CategoryResponse setActive(@PathVariable Long id, @Valid @RequestBody ActiveRequest request) {
        return categoryService.setActive(id, request.active());
    }

    @Override
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        categoryService.delete(id);
    }
}

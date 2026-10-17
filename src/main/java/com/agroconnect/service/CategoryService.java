package com.agroconnect.service;

import com.agroconnect.dto.CategoryRequest;
import com.agroconnect.dto.CategoryResponse;
import com.agroconnect.entity.FruitCategory;
import com.agroconnect.exception.ApiException;
import com.agroconnect.repository.FruitCategoryRepository;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class CategoryService {

    private final FruitCategoryRepository repo;

    public CategoryService(FruitCategoryRepository repo) { this.repo = repo; }

    public List<CategoryResponse> listActive() {
        return repo.findByActiveTrueOrderByNameAsc().stream().map(CategoryResponse::from).toList();
    }

    public List<CategoryResponse> listAll() {
        return repo.findAll(Sort.by("name")).stream().map(CategoryResponse::from).toList();
    }

    @Transactional
    public CategoryResponse create(CategoryRequest r) {
        if (repo.findByNameIgnoreCase(r.name().trim()).isPresent()) {
            throw new ApiException(HttpStatus.CONFLICT, "CATEGORY_EXISTS", "Category already exists");
        }
        FruitCategory c = new FruitCategory();
        apply(c, r);
        c.setActive(r.active() == null || r.active());
        return CategoryResponse.from(repo.save(c));
    }

    @Transactional
    public CategoryResponse update(Long id, CategoryRequest r) {
        FruitCategory c = repo.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", "Category not found"));
        repo.findByNameIgnoreCase(r.name().trim()).ifPresent(other -> {
            if (!other.getId().equals(id)) {
                throw new ApiException(HttpStatus.CONFLICT, "CATEGORY_EXISTS", "Category already exists");
            }
        });
        apply(c, r);
        if (r.active() != null) c.setActive(r.active());
        return CategoryResponse.from(repo.save(c));
    }

    private void apply(FruitCategory c, CategoryRequest r) {
        c.setName(r.name().trim());
        c.setNameMr(blankToNull(r.nameMr()));
        c.setNameHi(blankToNull(r.nameHi()));
    }

    private String blankToNull(String s) { return s == null || s.isBlank() ? null : s.trim(); }
}

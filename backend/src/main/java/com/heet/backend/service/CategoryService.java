package com.heet.backend.service;

import com.heet.backend.dto.request.CategoryRequest;
import com.heet.backend.dto.response.CategoryResponse;
import com.heet.backend.entity.Category;
import com.heet.backend.entity.User;
import com.heet.backend.exception.CategoryInUseException;
import com.heet.backend.exception.CategoryNotFoundException;
import com.heet.backend.exception.DuplicateCategoryException;
import com.heet.backend.repository.CategoryRepository;
import com.heet.backend.repository.TransactionRepository;
import com.heet.backend.security.SecurityUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final TransactionRepository transactionRepository;

    public CategoryService(CategoryRepository categoryRepository, TransactionRepository transactionRepository) {
        this.categoryRepository = categoryRepository;
        this.transactionRepository = transactionRepository;
    }

    @Transactional
    public CategoryResponse createCategory(CategoryRequest request) {
        User user = SecurityUtils.getCurrentUser();
        String name = request.getName().trim();

        if (categoryRepository.existsByNameIgnoreCaseAndUserId(name, user.getId())) {
            throw new DuplicateCategoryException(name);
        }

        Category category = new Category();
        category.setName(name);
        category.setDescription(request.getDescription());
        category.setColor(request.getColor());
        category.setIcon(request.getIcon());
        category.setIsDefault(request.getIsDefault() != null ? request.getIsDefault() : false);
        category.setUser(user);

        Category saved = categoryRepository.save(category);
        return mapToResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> getAllCategories() {
        User user = SecurityUtils.getCurrentUser();
        return categoryRepository.findByUserId(user.getId()).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public CategoryResponse getCategoryById(Long id) {
        Category category = findCategoryByIdAndCurrentUser(id);
        return mapToResponse(category);
    }

    @Transactional
    public CategoryResponse updateCategory(Long id, CategoryRequest request) {
        Category category = findCategoryByIdAndCurrentUser(id);
        User user = SecurityUtils.getCurrentUser();
        String name = request.getName().trim();

        if (categoryRepository.existsByNameIgnoreCaseAndUserIdAndIdNot(name, user.getId(), id)) {
            throw new DuplicateCategoryException(name);
        }

        category.setName(name);
        category.setDescription(request.getDescription());
        category.setColor(request.getColor());
        category.setIcon(request.getIcon());
        if (request.getIsDefault() != null) {
            category.setIsDefault(request.getIsDefault());
        }

        Category updated = categoryRepository.save(category);
        return mapToResponse(updated);
    }

    @Transactional
    public void deleteCategory(Long id) {
        Category category = findCategoryByIdAndCurrentUser(id);
        User user = SecurityUtils.getCurrentUser();

        if (transactionRepository.existsByCategoryIdAndUserId(id, user.getId())) {
            throw new CategoryInUseException(id);
        }

        categoryRepository.delete(category);
    }

    private Category findCategoryByIdAndCurrentUser(Long id) {
        User user = SecurityUtils.getCurrentUser();
        return categoryRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new CategoryNotFoundException(id));
    }

    private CategoryResponse mapToResponse(Category category) {
        return CategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .description(category.getDescription())
                .color(category.getColor())
                .icon(category.getIcon())
                .isDefault(category.getIsDefault())
                .createdAt(category.getCreatedAt())
                .updatedAt(category.getUpdatedAt())
                .build();
    }
}

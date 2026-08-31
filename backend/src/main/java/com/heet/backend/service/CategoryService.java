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

    private static final List<DefaultCategoryDefinition> DEFAULT_CATEGORIES = List.of(
            new DefaultCategoryDefinition("Food & Dining", "Restaurants, cafes, takeaway, and dining out", "#f97316", "Utensils"),
            new DefaultCategoryDefinition("Groceries", "Supermarket, provisions, daily produce, and staples", "#10b981", "ShoppingCart"),
            new DefaultCategoryDefinition("Healthcare & Medical", "Medicines, consultations, pharmacy, and hospital bills", "#ef4444", "HeartPulse"),
            new DefaultCategoryDefinition("Transportation", "Fuel, cabs, public transit, metro, and commute", "#3b82f6", "Car"),
            new DefaultCategoryDefinition("Housing & Rent", "Rent, maintenance, home repairs, and housing dues", "#6366f1", "Home"),
            new DefaultCategoryDefinition("Bills & Utilities", "Electricity, water, gas, broadband, and mobile recharge", "#eab308", "Zap"),
            new DefaultCategoryDefinition("Shopping", "Retail purchases, electronics, gadgets, and general shopping", "#ec4899", "ShoppingBag"),
            new DefaultCategoryDefinition("Clothing", "Apparel, footwear, fashion, and accessories", "#8b5cf6", "Shirt"),
            new DefaultCategoryDefinition("Personal Care", "Grooming, salon, cosmetics, wellness, and self-care", "#14b8a6", "Sparkles"),
            new DefaultCategoryDefinition("Entertainment", "Movies, events, recreational activities, and outings", "#a855f7", "Film"),
            new DefaultCategoryDefinition("Education", "Courses, books, tuition, and learning materials", "#06b6d4", "GraduationCap"),
            new DefaultCategoryDefinition("Travel", "Flights, hotels, holidays, and vacations", "#0ea5e9", "Plane"),
            new DefaultCategoryDefinition("Subscriptions", "Recurring memberships, streaming, and SaaS subscriptions", "#f59e0b", "CalendarCheck"),
            new DefaultCategoryDefinition("Other", "Miscellaneous expenses and uncategorized entries", "#64748b", "MoreHorizontal")
    );

    @Transactional
    public void createDefaultCategoriesForUser(User user) {
        if (user == null || user.getId() == null) {
            return;
        }

        for (DefaultCategoryDefinition def : DEFAULT_CATEGORIES) {
            if (!categoryRepository.existsByNameIgnoreCaseAndUserId(def.name(), user.getId())) {
                Category cat = new Category();
                cat.setName(def.name());
                cat.setDescription(def.description());
                cat.setColor(def.color());
                cat.setIcon(def.icon());
                cat.setIsDefault(true);
                cat.setUser(user);
                categoryRepository.save(cat);
            }
        }
    }

    @Transactional
    public List<CategoryResponse> getAllCategories() {
        User user = SecurityUtils.getCurrentUser();
        // Ensure default categories are present for the user
        createDefaultCategoriesForUser(user);

        List<Category> userCategories = categoryRepository.findByUserId(user.getId());
        return userCategories.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private record DefaultCategoryDefinition(String name, String description, String color, String icon) {}

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

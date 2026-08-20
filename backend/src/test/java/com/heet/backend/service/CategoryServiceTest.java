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
import com.heet.backend.security.UserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private CategoryService categoryService;

    private User testUser;
    private Category category;
    private CategoryRequest categoryRequest;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setName("Test User");
        testUser.setEmail("test@example.com");
        testUser.setPasswordHash("hashed_password");

        UserPrincipal principal = new UserPrincipal(testUser);
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);

        category = new Category();
        category.setId(1L);
        category.setName("Shopping");
        category.setDescription("Shopping expenses");
        category.setColor("#123456");
        category.setIcon("shopping-bag");
        category.setIsDefault(false);
        category.setUser(testUser);

        categoryRequest = CategoryRequest.builder()
                .name("Shopping")
                .description("Shopping expenses")
                .color("#123456")
                .icon("shopping-bag")
                .isDefault(false)
                .build();
    }

    @Test
    void createCategory_Success() {
        when(categoryRepository.existsByNameIgnoreCaseAndUserId("Shopping", 1L)).thenReturn(false);
        when(categoryRepository.save(any(Category.class))).thenReturn(category);

        CategoryResponse response = categoryService.createCategory(categoryRequest);

        assertNotNull(response);
        assertEquals("Shopping", response.getName());
        verify(categoryRepository, times(1)).save(any(Category.class));
    }

    @Test
    void createCategory_ThrowsDuplicateCategoryException_CaseInsensitive() {
        when(categoryRepository.existsByNameIgnoreCaseAndUserId("Shopping", 1L)).thenReturn(true);

        assertThrows(DuplicateCategoryException.class, () -> categoryService.createCategory(categoryRequest));
        verify(categoryRepository, never()).save(any(Category.class));
    }

    @Test
    void getAllCategories_Success() {
        when(categoryRepository.findByUserId(1L)).thenReturn(List.of(category));

        List<CategoryResponse> responses = categoryService.getAllCategories();

        assertEquals(1, responses.size());
        assertEquals("Shopping", responses.get(0).getName());
    }

    @Test
    void getCategoryById_Success() {
        when(categoryRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(category));

        CategoryResponse response = categoryService.getCategoryById(1L);

        assertNotNull(response);
        assertEquals(1L, response.getId());
    }

    @Test
    void getCategoryById_NotFound() {
        when(categoryRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.empty());

        assertThrows(CategoryNotFoundException.class, () -> categoryService.getCategoryById(1L));
    }

    @Test
    void deleteCategory_Success() {
        when(categoryRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(category));
        when(transactionRepository.existsByCategoryIdAndUserId(1L, 1L)).thenReturn(false);

        categoryService.deleteCategory(1L);

        verify(categoryRepository, times(1)).delete(category);
    }

    @Test
    void deleteCategory_ThrowsCategoryInUseException() {
        when(categoryRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(category));
        when(transactionRepository.existsByCategoryIdAndUserId(1L, 1L)).thenReturn(true);

        assertThrows(CategoryInUseException.class, () -> categoryService.deleteCategory(1L));
        verify(categoryRepository, never()).delete(any(Category.class));
    }
}

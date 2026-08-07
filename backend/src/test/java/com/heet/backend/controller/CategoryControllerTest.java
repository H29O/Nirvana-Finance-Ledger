package com.heet.backend.controller;

import com.heet.backend.dto.request.CategoryRequest;
import com.heet.backend.dto.response.CategoryResponse;
import com.heet.backend.service.CategoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoryControllerTest {

    @Mock
    private CategoryService categoryService;

    @InjectMocks
    private CategoryController categoryController;

    private CategoryRequest request;
    private CategoryResponse response;

    @BeforeEach
    void setUp() {
        request = CategoryRequest.builder()
                .name("Shopping")
                .description("Shopping items")
                .color("#FF0000")
                .icon("cart")
                .isDefault(false)
                .build();

        response = CategoryResponse.builder()
                .id(1L)
                .name("Shopping")
                .description("Shopping items")
                .color("#FF0000")
                .icon("cart")
                .isDefault(false)
                .build();
    }

    @Test
    void createCategory_Returns201Created() {
        when(categoryService.createCategory(any(CategoryRequest.class))).thenReturn(response);

        ResponseEntity<CategoryResponse> result = categoryController.createCategory(request);

        assertEquals(HttpStatus.CREATED, result.getStatusCode());
        assertNotNull(result.getBody());
        assertEquals("Shopping", result.getBody().getName());
    }

    @Test
    void getAllCategories_Returns200OK() {
        when(categoryService.getAllCategories()).thenReturn(List.of(response));

        ResponseEntity<List<CategoryResponse>> result = categoryController.getAllCategories();

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertNotNull(result.getBody());
        assertEquals(1, result.getBody().size());
    }

    @Test
    void getCategoryById_Returns200OK() {
        when(categoryService.getCategoryById(1L)).thenReturn(response);

        ResponseEntity<CategoryResponse> result = categoryController.getCategoryById(1L);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertNotNull(result.getBody());
        assertEquals(1L, result.getBody().getId());
    }

    @Test
    void updateCategory_Returns200OK() {
        when(categoryService.updateCategory(eq(1L), any(CategoryRequest.class))).thenReturn(response);

        ResponseEntity<CategoryResponse> result = categoryController.updateCategory(1L, request);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertNotNull(result.getBody());
        assertEquals(1L, result.getBody().getId());
    }

    @Test
    void deleteCategory_Returns204NoContent() {
        doNothing().when(categoryService).deleteCategory(1L);

        ResponseEntity<Void> result = categoryController.deleteCategory(1L);

        assertEquals(HttpStatus.NO_CONTENT, result.getStatusCode());
        verify(categoryService, times(1)).deleteCategory(1L);
    }
}

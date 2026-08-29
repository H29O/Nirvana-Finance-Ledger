package com.heet.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.heet.backend.dto.request.CategoryRequest;
import com.heet.backend.dto.request.CreateLoanRecordRequest;
import com.heet.backend.dto.request.LoanPaymentRequest;
import com.heet.backend.dto.request.LoginRequest;
import com.heet.backend.dto.request.RegisterRequest;
import com.heet.backend.enums.LoanType;
import com.heet.backend.exception.CategoryInUseException;
import com.heet.backend.exception.CategoryNotFoundException;
import com.heet.backend.exception.DuplicateCategoryException;
import com.heet.backend.exception.DuplicateEmailException;
import com.heet.backend.exception.GlobalExceptionHandler;
import com.heet.backend.exception.InvalidCredentialsException;
import com.heet.backend.exception.InvalidLoanPaymentException;
import com.heet.backend.exception.InvalidReceiptException;
import com.heet.backend.exception.LoanRecordNotFoundException;
import com.heet.backend.exception.ReceiptNotFoundException;
import com.heet.backend.service.AuthService;
import com.heet.backend.service.CategoryService;
import com.heet.backend.service.LoanRecordService;
import com.heet.backend.service.MoneyWrappedService;
import com.heet.backend.service.ReceiptService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class RequestValidationMockMvcTest {

    private ObjectMapper objectMapper;

    @Mock
    private AuthService authService;

    @InjectMocks
    private AuthController authController;

    @Mock
    private CategoryService categoryService;

    @InjectMocks
    private CategoryController categoryController;

    @Mock
    private LoanRecordService loanRecordService;

    @InjectMocks
    private LoanRecordController loanRecordController;

    @Mock
    private MoneyWrappedService moneyWrappedService;

    @InjectMocks
    private MoneyWrappedController moneyWrappedController;

    @Mock
    private ReceiptService receiptService;

    @InjectMocks
    private ReceiptController receiptController;

    private MockMvc authMockMvc;
    private MockMvc categoryMockMvc;
    private MockMvc loanMockMvc;
    private MockMvc wrappedMockMvc;
    private MockMvc receiptMockMvc;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper().findAndRegisterModules();

        GlobalExceptionHandler handler = new GlobalExceptionHandler();

        authMockMvc = MockMvcBuilders.standaloneSetup(authController)
                .setControllerAdvice(handler)
                .build();

        categoryMockMvc = MockMvcBuilders.standaloneSetup(categoryController)
                .setControllerAdvice(handler)
                .build();

        loanMockMvc = MockMvcBuilders.standaloneSetup(loanRecordController)
                .setControllerAdvice(handler)
                .build();

        wrappedMockMvc = MockMvcBuilders.standaloneSetup(moneyWrappedController)
                .setControllerAdvice(handler)
                .build();

        receiptMockMvc = MockMvcBuilders.standaloneSetup(receiptController)
                .setControllerAdvice(handler)
                .build();
    }

    // --- AuthController Validation & Error Tests ---

    @Test
    void register_InvalidFields_Returns400WithFieldErrors() throws Exception {
        RegisterRequest invalidRequest = RegisterRequest.builder()
                .name("") // blank
                .email("invalid-email-address") // malformed email
                .password("123") // too short (< 6)
                .build();

        authMockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.name", is("Name is required")))
                .andExpect(jsonPath("$.email", is("Invalid email format")))
                .andExpect(jsonPath("$.password", is("Password must be at least 6 characters long")));
    }

    @Test
    void register_DuplicateEmail_Returns409Conflict() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .name("Alice")
                .email("alice@example.com")
                .password("password123")
                .build();

        when(authService.register(any())).thenThrow(new DuplicateEmailException("alice@example.com"));

        authMockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error", containsString("User with email 'alice@example.com' already exists.")));
    }

    @Test
    void login_InvalidCredentials_Returns401Unauthorized() throws Exception {
        LoginRequest request = LoginRequest.builder()
                .email("wrong@example.com")
                .password("wrongpass")
                .build();

        when(authService.login(any())).thenThrow(new InvalidCredentialsException());

        authMockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error", is("Invalid email or password")));
    }

    // --- CategoryController Validation & Error Tests ---

    @Test
    void createCategory_BlankName_Returns400BadRequest() throws Exception {
        CategoryRequest request = CategoryRequest.builder()
                .name("")
                .build();

        categoryMockMvc.perform(post("/api/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.name", is("Category name cannot be blank")));
    }

    @Test
    void createCategory_DuplicateName_Returns409Conflict() throws Exception {
        CategoryRequest request = CategoryRequest.builder()
                .name("Entertainment")
                .build();

        when(categoryService.createCategory(any())).thenThrow(new DuplicateCategoryException("Entertainment"));

        categoryMockMvc.perform(post("/api/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error", containsString("Category with name 'Entertainment' already exists")));
    }

    @Test
    void deleteCategory_InUse_Returns409Conflict() throws Exception {
        doThrow(new CategoryInUseException(5L))
                .when(categoryService).deleteCategory(5L);

        categoryMockMvc.perform(delete("/api/categories/5"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error", containsString("Cannot delete category with id 5 because it is referenced by existing transactions")));
    }

    @Test
    void getCategoryById_NotFound_Returns404NotFound() throws Exception {
        when(categoryService.getCategoryById(999L)).thenThrow(new CategoryNotFoundException(999L));

        categoryMockMvc.perform(get("/api/categories/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error", containsString("Category with id 999 not found")));
    }

    // --- LoanRecordController Validation & Error Tests ---

    @Test
    void createLoan_MissingFieldsAndZeroAmount_Returns400BadRequest() throws Exception {
        CreateLoanRecordRequest invalid = new CreateLoanRecordRequest();
        invalid.setOriginalAmount(new BigDecimal("0.00")); // amount <= 0

        loanMockMvc.perform(post("/api/loans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type", is("Loan type is required")))
                .andExpect(jsonPath("$.personName", is("Person name is required")))
                .andExpect(jsonPath("$.originalAmount", is("Original amount must be greater than zero")));
    }

    @Test
    void recordLoanPayment_PaymentExceedsRemaining_Returns400BadRequest() throws Exception {
        LoanPaymentRequest payment = new LoanPaymentRequest(new BigDecimal("500.00"));

        when(loanRecordService.recordPayment(eq(10L), any()))
                .thenThrow(new InvalidLoanPaymentException("Payment amount cannot exceed remaining amount"));

        loanMockMvc.perform(patch("/api/loans/10/payment")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payment)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("Payment amount cannot exceed remaining amount")));
    }

    @Test
    void getLoanById_NotFound_Returns404NotFound() throws Exception {
        when(loanRecordService.getLoanRecordById(888L)).thenThrow(new LoanRecordNotFoundException(888L));

        loanMockMvc.perform(get("/api/loans/888"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error", containsString("Loan record with id 888 not found")));
    }

    // --- MoneyWrappedController Error Handling ---

    @Test
    void getWrapped_YearLessThan2000_Returns400BadRequest() throws Exception {
        wrappedMockMvc.perform(get("/api/wrapped")
                        .param("year", "1999")
                        .param("month", "5"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("Year must be greater than or equal to 2000")));
    }

    @Test
    void getWrapped_MonthGreaterThan12_Returns400BadRequest() throws Exception {
        wrappedMockMvc.perform(get("/api/wrapped")
                        .param("year", "2026")
                        .param("month", "13"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("Month must be between 1 and 12")));
    }

    // --- ReceiptController Error Handling ---

    @Test
    void getReceipt_NotFound_Returns404NotFound() throws Exception {
        when(receiptService.getReceiptMetadata(777L)).thenThrow(new ReceiptNotFoundException(777L));

        receiptMockMvc.perform(get("/api/receipts/777"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error", containsString("Receipt with id 777 not found")));
    }

    @Test
    void uploadReceipt_InvalidFile_Returns400BadRequest() throws Exception {
        when(receiptService.uploadReceipt(any(), any()))
                .thenThrow(new InvalidReceiptException("Unsupported file type"));

        receiptMockMvc.perform(multipart("/api/receipts")
                        .file("file", "dummy.exe".getBytes()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("Unsupported file type")));
    }
}

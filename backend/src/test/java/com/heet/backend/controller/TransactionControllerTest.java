package com.heet.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.heet.backend.dto.request.CreateTransactionRequest;
import com.heet.backend.dto.response.TransactionResponse;
import com.heet.backend.enums.EntrySource;
import com.heet.backend.enums.PaymentMethod;
import com.heet.backend.enums.TransactionType;
import com.heet.backend.exception.CategoryNotFoundException;
import com.heet.backend.exception.GlobalExceptionHandler;
import com.heet.backend.exception.TransactionNotFoundException;
import com.heet.backend.service.TransactionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class TransactionControllerTest {

    private MockMvc mockMvc;

    private ObjectMapper objectMapper;

    @Mock
    private TransactionService transactionService;

    @InjectMocks
    private TransactionController transactionController;

    private CreateTransactionRequest validRequest;
    private TransactionResponse sampleResponse;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(transactionController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        objectMapper = new ObjectMapper().findAndRegisterModules();

        validRequest = new CreateTransactionRequest(
                new BigDecimal("49.99"),
                TransactionType.EXPENSE,
                PaymentMethod.DIGITAL,
                EntrySource.MANUAL,
                "Supermarket",
                "Weekly groceries",
                1L
        );

        sampleResponse = TransactionResponse.builder()
                .id(100L)
                .amount(new BigDecimal("49.99"))
                .transactionType(TransactionType.EXPENSE)
                .paymentMethod(PaymentMethod.DIGITAL)
                .entrySource(EntrySource.MANUAL)
                .transactionDate(LocalDateTime.of(2026, 8, 29, 10, 0))
                .merchant("Supermarket")
                .notes("Weekly groceries")
                .categoryId(1L)
                .categoryName("Groceries")
                .build();
    }

    @Test
    void createTransaction_ValidRequest_Returns201Created() throws Exception {
        when(transactionService.createTransaction(any(CreateTransactionRequest.class)))
                .thenReturn(sampleResponse);

        mockMvc.perform(post("/api/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id", is(100)))
                .andExpect(jsonPath("$.amount", is(49.99)))
                .andExpect(jsonPath("$.transactionType", is("EXPENSE")))
                .andExpect(jsonPath("$.paymentMethod", is("DIGITAL")))
                .andExpect(jsonPath("$.entrySource", is("MANUAL")))
                .andExpect(jsonPath("$.merchant", is("Supermarket")))
                .andExpect(jsonPath("$.categoryName", is("Groceries")));
    }

    @Test
    void createTransaction_MissingRequiredFields_Returns400BadRequest() throws Exception {
        CreateTransactionRequest invalidRequest = new CreateTransactionRequest();
        // Missing amount, transactionType, paymentMethod, entrySource, merchant

        mockMvc.perform(post("/api/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.amount", is("Amount is required")))
                .andExpect(jsonPath("$.transactionType", is("Transaction type is required")))
                .andExpect(jsonPath("$.paymentMethod", is("Payment method is required")))
                .andExpect(jsonPath("$.entrySource", is("Entry source is required")))
                .andExpect(jsonPath("$.merchant", is("Merchant cannot be blank")));
    }

    @Test
    void createTransaction_AmountLessThanZero_Returns400BadRequest() throws Exception {
        CreateTransactionRequest zeroAmountRequest = new CreateTransactionRequest(
                new BigDecimal("0.00"),
                TransactionType.EXPENSE,
                PaymentMethod.CASH,
                EntrySource.MANUAL,
                "Cafe",
                "Coffee",
                null
        );

        mockMvc.perform(post("/api/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(zeroAmountRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.amount", is("Amount must be greater than zero")));
    }

    @Test
    void createTransaction_CategoryNotFound_Returns404NotFound() throws Exception {
        when(transactionService.createTransaction(any(CreateTransactionRequest.class)))
                .thenThrow(new CategoryNotFoundException(99L));

        mockMvc.perform(post("/api/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error", containsString("Category with id 99 not found")));
    }

    @Test
    void getTransactions_WithPaginationAndFilters_Returns200OK() throws Exception {
        PageImpl<TransactionResponse> page = new PageImpl<>(
                List.of(sampleResponse),
                PageRequest.of(0, 10),
                1
        );

        when(transactionService.getTransactions(
                eq(TransactionType.EXPENSE),
                eq(PaymentMethod.DIGITAL),
                any(),
                any(),
                any(Pageable.class)))
                .thenReturn(page);

        mockMvc.perform(get("/api/transactions")
                        .param("type", "EXPENSE")
                        .param("paymentMethod", "DIGITAL")
                        .param("page", "0")
                        .param("size", "10")
                        .param("sort", "transactionDate,desc"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content[0].id", is(100)))
                .andExpect(jsonPath("$.content[0].merchant", is("Supermarket")))
                .andExpect(jsonPath("$.totalElements", is(1)));
    }

    @Test
    void updateTransaction_ValidRequest_Returns200OK() throws Exception {
        when(transactionService.updateTransaction(eq(100L), any(CreateTransactionRequest.class)))
                .thenReturn(sampleResponse);

        mockMvc.perform(put("/api/transactions/100")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(100)))
                .andExpect(jsonPath("$.merchant", is("Supermarket")));
    }

    @Test
    void updateTransaction_NotFound_Returns404NotFound() throws Exception {
        when(transactionService.updateTransaction(eq(999L), any(CreateTransactionRequest.class)))
                .thenThrow(new TransactionNotFoundException(999L));

        mockMvc.perform(put("/api/transactions/999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error", containsString("Transaction with id 999 not found")));
    }

    @Test
    void deleteTransaction_Success_Returns204NoContent() throws Exception {
        doNothing().when(transactionService).deleteTransaction(100L);

        mockMvc.perform(delete("/api/transactions/100"))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteTransaction_NotFound_Returns404NotFound() throws Exception {
        doThrow(new TransactionNotFoundException(999L))
                .when(transactionService).deleteTransaction(999L);

        mockMvc.perform(delete("/api/transactions/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error", containsString("Transaction with id 999 not found")));
    }
}

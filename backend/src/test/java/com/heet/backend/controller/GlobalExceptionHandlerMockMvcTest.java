package com.heet.backend.controller;

import com.heet.backend.exception.FileStorageException;
import com.heet.backend.exception.GlobalExceptionHandler;
import com.heet.backend.service.TransactionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.time.DateTimeException;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerMockMvcTest {

    private MockMvc mockMvc;

    @Mock
    private TransactionService transactionService;

    @InjectMocks
    private TransactionController transactionController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(transactionController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void handleDateTimeException_Returns400BadRequest() throws Exception {
        when(transactionService.getTransactions(any(), any(), any(), any(), any()))
                .thenThrow(new DateTimeException("Invalid date"));

        mockMvc.perform(get("/api/transactions"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("Invalid year or month supplied.")));
    }

    @Test
    void handleFileStorageException_Returns500InternalServerError() throws Exception {
        when(transactionService.getTransactions(any(), any(), any(), any(), any()))
                .thenThrow(new FileStorageException("Disk write failed"));

        mockMvc.perform(get("/api/transactions"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error", is("Disk write failed")));
    }

    @Test
    void handleMaxUploadSizeExceededException_Returns400BadRequest() throws Exception {
        when(transactionService.getTransactions(any(), any(), any(), any(), any()))
                .thenThrow(new MaxUploadSizeExceededException(10485760L));

        mockMvc.perform(get("/api/transactions"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("File size exceeds maximum allowed limit of 10MB.")));
    }

    @Test
    void handleGenericException_Returns500InternalServerError() throws Exception {
        when(transactionService.getTransactions(any(), any(), any(), any(), any()))
                .thenThrow(new RuntimeException("Unexpected DB network glitch"));

        mockMvc.perform(get("/api/transactions"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error", is("An unexpected error occurred.")));
    }

    @Test
    void handleIllegalArgumentException_Returns400BadRequest() throws Exception {
        when(transactionService.getTransactions(any(), any(), any(), any(), any()))
                .thenThrow(new IllegalArgumentException("Invalid argument provided"));

        mockMvc.perform(get("/api/transactions"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("Invalid argument provided")));
    }
}

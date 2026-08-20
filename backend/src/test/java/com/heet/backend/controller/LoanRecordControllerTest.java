package com.heet.backend.controller;

import com.heet.backend.dto.request.CreateLoanRecordRequest;
import com.heet.backend.dto.request.LoanPaymentRequest;
import com.heet.backend.dto.request.UpdateLoanRecordRequest;
import com.heet.backend.dto.response.LoanRecordResponse;
import com.heet.backend.enums.LoanStatus;
import com.heet.backend.enums.LoanType;
import com.heet.backend.service.LoanRecordService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoanRecordControllerTest {

    @Mock
    private LoanRecordService loanRecordService;

    @InjectMocks
    private LoanRecordController loanRecordController;

    private CreateLoanRecordRequest createRequest;
    private UpdateLoanRecordRequest updateRequest;
    private LoanPaymentRequest paymentRequest;
    private LoanRecordResponse response;

    @BeforeEach
    void setUp() {
        createRequest = new CreateLoanRecordRequest(
                LoanType.LENT,
                "John Doe",
                "Dinner loan",
                BigDecimal.valueOf(50.00),
                LocalDate.of(2026, 12, 31)
        );

        updateRequest = new UpdateLoanRecordRequest(
                LoanType.LENT,
                "John Doe",
                "Updated dinner loan",
                LocalDate.of(2026, 12, 31)
        );

        paymentRequest = new LoanPaymentRequest(BigDecimal.valueOf(25.00));

        response = LoanRecordResponse.builder()
                .id(1L)
                .type(LoanType.LENT)
                .personName("John Doe")
                .description("Dinner loan")
                .originalAmount(BigDecimal.valueOf(50.00))
                .remainingAmount(BigDecimal.valueOf(50.00))
                .status(LoanStatus.ACTIVE)
                .dueDate(LocalDate.of(2026, 12, 31))
                .build();
    }

    @Test
    void createLoanRecord_Returns201Created() {
        when(loanRecordService.createLoanRecord(any(CreateLoanRecordRequest.class))).thenReturn(response);

        ResponseEntity<LoanRecordResponse> result = loanRecordController.createLoanRecord(createRequest);

        assertEquals(HttpStatus.CREATED, result.getStatusCode());
        assertNotNull(result.getBody());
        assertEquals(1L, result.getBody().getId());
    }

    @Test
    void getLoanRecords_Returns200OK() {
        Page<LoanRecordResponse> page = new PageImpl<>(List.of(response));
        when(loanRecordService.getLoanRecords(eq(LoanType.LENT), eq(LoanStatus.ACTIVE), any(Pageable.class)))
                .thenReturn(page);

        ResponseEntity<Page<LoanRecordResponse>> result = loanRecordController.getLoanRecords(
                LoanType.LENT, LoanStatus.ACTIVE, 0, 10, "createdAt,desc");

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertNotNull(result.getBody());
        assertEquals(1, result.getBody().getTotalElements());
    }

    @Test
    void getLoanRecordById_Returns200OK() {
        when(loanRecordService.getLoanRecordById(1L)).thenReturn(response);

        ResponseEntity<LoanRecordResponse> result = loanRecordController.getLoanRecordById(1L);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertNotNull(result.getBody());
        assertEquals(1L, result.getBody().getId());
    }

    @Test
    void updateLoanRecord_Returns200OK() {
        when(loanRecordService.updateLoanRecord(eq(1L), any(UpdateLoanRecordRequest.class))).thenReturn(response);

        ResponseEntity<LoanRecordResponse> result = loanRecordController.updateLoanRecord(1L, updateRequest);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertNotNull(result.getBody());
        assertEquals(1L, result.getBody().getId());
    }

    @Test
    void deleteLoanRecord_Returns204NoContent() {
        doNothing().when(loanRecordService).deleteLoanRecord(1L);

        ResponseEntity<Void> result = loanRecordController.deleteLoanRecord(1L);

        assertEquals(HttpStatus.NO_CONTENT, result.getStatusCode());
        verify(loanRecordService, times(1)).deleteLoanRecord(1L);
    }

    @Test
    void recordPayment_Returns200OK() {
        when(loanRecordService.recordPayment(eq(1L), any(LoanPaymentRequest.class))).thenReturn(response);

        ResponseEntity<LoanRecordResponse> result = loanRecordController.recordPayment(1L, paymentRequest);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertNotNull(result.getBody());
        assertEquals(1L, result.getBody().getId());
    }
}

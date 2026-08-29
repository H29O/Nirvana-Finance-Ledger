package com.heet.backend.controller;

import com.heet.backend.dto.response.MoneyWrappedResponse;
import com.heet.backend.service.MoneyWrappedService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MoneyWrappedControllerTest {

    @Mock
    private MoneyWrappedService moneyWrappedService;

    @InjectMocks
    private MoneyWrappedController moneyWrappedController;

    private MoneyWrappedResponse sampleResponse;

    @BeforeEach
    void setUp() {
        sampleResponse = MoneyWrappedResponse.builder()
                .year(2026)
                .month(8)
                .build();
    }

    @Test
    void getMoneyWrapped_WithExplicitYearAndMonth_Returns200OK() {
        when(moneyWrappedService.getMoneyWrapped(eq(2026), eq(8)))
                .thenReturn(sampleResponse);

        ResponseEntity<MoneyWrappedResponse> response =
                moneyWrappedController.getMoneyWrapped(2026, 8);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(2026, response.getBody().getYear());
        assertEquals(8, response.getBody().getMonth());
    }

    @Test
    void getMoneyWrapped_WithDefaults_UsesCurrentYearAndMonth() {
        int currentYear = LocalDate.now().getYear();
        int currentMonth = LocalDate.now().getMonthValue();

        MoneyWrappedResponse currentResponse = MoneyWrappedResponse.builder()
                .year(currentYear)
                .month(currentMonth)
                .build();

        when(moneyWrappedService.getMoneyWrapped(eq(currentYear), eq(currentMonth)))
                .thenReturn(currentResponse);

        ResponseEntity<MoneyWrappedResponse> response =
                moneyWrappedController.getMoneyWrapped(null, null);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(currentYear, response.getBody().getYear());
        assertEquals(currentMonth, response.getBody().getMonth());
    }

    @Test
    void getMoneyWrapped_InvalidYear_ThrowsIllegalArgumentException() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                moneyWrappedController.getMoneyWrapped(1999, 5));

        assertEquals("Year must be greater than or equal to 2000", ex.getMessage());
    }

    @Test
    void getMoneyWrapped_InvalidMonth_ThrowsIllegalArgumentException() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                moneyWrappedController.getMoneyWrapped(2026, 13));

        assertEquals("Month must be between 1 and 12", ex.getMessage());
    }
}

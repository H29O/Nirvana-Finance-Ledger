package com.heet.backend.controller;

import com.heet.backend.dto.response.MoneyWrappedResponse;
import com.heet.backend.service.MoneyWrappedService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.YearMonth;

@RestController
@RequestMapping("/api/wrapped")
public class MoneyWrappedController {

    private final MoneyWrappedService moneyWrappedService;

    public MoneyWrappedController(MoneyWrappedService moneyWrappedService) {
        this.moneyWrappedService = moneyWrappedService;
    }

    @GetMapping
    public ResponseEntity<MoneyWrappedResponse> getMoneyWrapped(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month) {

        YearMonth yearMonth = resolveYearMonth(year, month);
        MoneyWrappedResponse response = moneyWrappedService.getMoneyWrapped(
                yearMonth.getYear(),
                yearMonth.getMonthValue());

        return ResponseEntity.ok(response);
    }

    private YearMonth resolveYearMonth(Integer year, Integer month) {
        int targetYear = year != null
                ? year
                : LocalDate.now().getYear();

        int targetMonth = month != null
                ? month
                : LocalDate.now().getMonthValue();

        if (targetYear < 2000) {
            throw new IllegalArgumentException("Year must be greater than or equal to 2000");
        }

        if (targetMonth < 1 || targetMonth > 12) {
            throw new IllegalArgumentException("Month must be between 1 and 12");
        }

        return YearMonth.of(targetYear, targetMonth);
    }
}

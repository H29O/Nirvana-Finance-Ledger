package com.heet.backend.controller;

import com.heet.backend.dto.request.AddLoanPrincipalRequest;
import com.heet.backend.dto.request.CreateLoanRecordRequest;
import com.heet.backend.dto.request.LoanPaymentRequest;
import com.heet.backend.dto.request.UpdateLoanRecordRequest;
import com.heet.backend.dto.response.LoanRecordResponse;
import com.heet.backend.enums.LoanStatus;
import com.heet.backend.enums.LoanType;
import com.heet.backend.service.LoanRecordService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/loans")
public class LoanRecordController {

    private final LoanRecordService loanRecordService;

    public LoanRecordController(LoanRecordService loanRecordService) {
        this.loanRecordService = loanRecordService;
    }

    @PostMapping
    public ResponseEntity<LoanRecordResponse> createLoanRecord(
            @Valid @RequestBody CreateLoanRecordRequest request) {
        LoanRecordResponse response = loanRecordService.createLoanRecord(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<Page<LoanRecordResponse>> getLoanRecords(
            @RequestParam(required = false) LoanType type,
            @RequestParam(required = false) LoanStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort) {

        String[] sortParameters = sort.split(",", 2);
        String sortProperty = sortParameters[0];
        Sort.Direction sortDirection = sortParameters.length > 1
                ? Sort.Direction.fromString(sortParameters[1])
                : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sortProperty));

        Page<LoanRecordResponse> response = loanRecordService.getLoanRecords(type, status, pageable);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<LoanRecordResponse> getLoanRecordById(@PathVariable Long id) {
        LoanRecordResponse response = loanRecordService.getLoanRecordById(id);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<LoanRecordResponse> updateLoanRecord(
            @PathVariable Long id,
            @Valid @RequestBody UpdateLoanRecordRequest request) {
        LoanRecordResponse response = loanRecordService.updateLoanRecord(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteLoanRecord(@PathVariable Long id) {
        loanRecordService.deleteLoanRecord(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/payment")
    public ResponseEntity<LoanRecordResponse> recordPayment(
            @PathVariable Long id,
            @Valid @RequestBody LoanPaymentRequest request) {
        LoanRecordResponse response = loanRecordService.recordPayment(id, request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/add-money")
    public ResponseEntity<LoanRecordResponse> addMoreMoney(
            @PathVariable Long id,
            @Valid @RequestBody AddLoanPrincipalRequest request) {
        LoanRecordResponse response = loanRecordService.addMoreMoney(id, request);
        return ResponseEntity.ok(response);
    }
}

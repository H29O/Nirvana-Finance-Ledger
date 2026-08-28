package com.heet.backend.controller;

import com.heet.backend.dto.response.ReceiptOcrResponse;
import com.heet.backend.service.ReceiptOcrService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/receipts/{receiptId}/ocr")
public class ReceiptOcrController {

    private final ReceiptOcrService receiptOcrService;

    public ReceiptOcrController(ReceiptOcrService receiptOcrService) {
        this.receiptOcrService = receiptOcrService;
    }

    @PostMapping
    public ResponseEntity<ReceiptOcrResponse> processOcr(@PathVariable Long receiptId) {
        ReceiptOcrResponse response = receiptOcrService.processOcr(receiptId);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<ReceiptOcrResponse> getOcrData(@PathVariable Long receiptId) {
        ReceiptOcrResponse response = receiptOcrService.getOcrData(receiptId);
        return ResponseEntity.ok(response);
    }
}

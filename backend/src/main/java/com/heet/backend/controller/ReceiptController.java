package com.heet.backend.controller;

import com.heet.backend.dto.response.ReceiptFileResource;
import com.heet.backend.dto.response.ReceiptResponse;
import com.heet.backend.service.ReceiptService;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/receipts")
public class ReceiptController {

    private final ReceiptService receiptService;

    public ReceiptController(ReceiptService receiptService) {
        this.receiptService = receiptService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ReceiptResponse> uploadReceipt(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "transactionId", required = false) Long transactionId) {

        ReceiptResponse response = receiptService.uploadReceipt(file, transactionId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<Page<ReceiptResponse>> getReceipts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "uploadedAt,desc") String sort) {

        String[] sortParameters = sort.split(",", 2);
        String sortProperty = sortParameters[0];
        Sort.Direction sortDirection = sortParameters.length > 1
                ? Sort.Direction.fromString(sortParameters[1])
                : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sortProperty));

        Page<ReceiptResponse> response = receiptService.getReceipts(pageable);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ReceiptResponse> getReceiptMetadata(@PathVariable Long id) {
        ReceiptResponse response = receiptService.getReceiptMetadata(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/file")
    public ResponseEntity<Resource> getReceiptFile(@PathVariable Long id) {
        ReceiptFileResource fileResource = receiptService.getReceiptFile(id);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(fileResource.getContentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + fileResource.getOriginalFileName() + "\"")
                .body(fileResource.getResource());
    }

    @PatchMapping("/{id}/transaction/{transactionId}")
    public ResponseEntity<ReceiptResponse> linkTransaction(
            @PathVariable Long id,
            @PathVariable Long transactionId) {

        ReceiptResponse response = receiptService.linkTransaction(id, transactionId);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}/transaction")
    public ResponseEntity<ReceiptResponse> unlinkTransaction(@PathVariable Long id) {
        ReceiptResponse response = receiptService.unlinkTransaction(id);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteReceipt(@PathVariable Long id) {
        receiptService.deleteReceipt(id);
        return ResponseEntity.noContent().build();
    }
}

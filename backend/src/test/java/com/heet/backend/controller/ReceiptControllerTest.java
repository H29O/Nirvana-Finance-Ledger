package com.heet.backend.controller;

import com.heet.backend.dto.response.ReceiptFileResource;
import com.heet.backend.dto.response.ReceiptResponse;
import com.heet.backend.service.ReceiptService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;

import java.time.LocalDateTime;
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
class ReceiptControllerTest {

    @Mock
    private ReceiptService receiptService;

    @InjectMocks
    private ReceiptController receiptController;

    private ReceiptResponse sampleResponse;
    private MockMultipartFile sampleFile;

    @BeforeEach
    void setUp() {
        sampleResponse = ReceiptResponse.builder()
                .id(1L)
                .originalFileName("receipt.pdf")
                .contentType("application/pdf")
                .fileSize(2048L)
                .uploadedAt(LocalDateTime.now())
                .transactionId(100L)
                .build();

        sampleFile = new MockMultipartFile(
                "file",
                "receipt.pdf",
                "application/pdf",
                "dummy content".getBytes()
        );
    }

    @Test
    void uploadReceipt_Returns201Created() {
        when(receiptService.uploadReceipt(eq(sampleFile), eq(100L))).thenReturn(sampleResponse);

        ResponseEntity<ReceiptResponse> result = receiptController.uploadReceipt(sampleFile, 100L);

        assertEquals(HttpStatus.CREATED, result.getStatusCode());
        assertNotNull(result.getBody());
        assertEquals(1L, result.getBody().getId());
    }

    @Test
    void getReceipts_Returns200OK() {
        Page<ReceiptResponse> page = new PageImpl<>(List.of(sampleResponse));
        when(receiptService.getReceipts(any(Pageable.class))).thenReturn(page);

        ResponseEntity<Page<ReceiptResponse>> result = receiptController.getReceipts(0, 10, "uploadedAt,desc");

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertNotNull(result.getBody());
        assertEquals(1, result.getBody().getTotalElements());
    }

    @Test
    void getReceiptMetadata_Returns200OK() {
        when(receiptService.getReceiptMetadata(1L)).thenReturn(sampleResponse);

        ResponseEntity<ReceiptResponse> result = receiptController.getReceiptMetadata(1L);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertNotNull(result.getBody());
        assertEquals(1L, result.getBody().getId());
    }

    @Test
    void getReceiptFile_Returns200OK() {
        Resource resource = new ByteArrayResource("content".getBytes());
        ReceiptFileResource fileResource = new ReceiptFileResource(resource, "application/pdf", "receipt.pdf");

        when(receiptService.getReceiptFile(1L)).thenReturn(fileResource);

        ResponseEntity<Resource> result = receiptController.getReceiptFile(1L);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertNotNull(result.getBody());
        assertEquals("application/pdf", result.getHeaders().getContentType().toString());
    }

    @Test
    void linkTransaction_Returns200OK() {
        when(receiptService.linkTransaction(1L, 100L)).thenReturn(sampleResponse);

        ResponseEntity<ReceiptResponse> result = receiptController.linkTransaction(1L, 100L);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertNotNull(result.getBody());
        assertEquals(100L, result.getBody().getTransactionId());
    }

    @Test
    void unlinkTransaction_Returns200OK() {
        ReceiptResponse unlinkedResponse = ReceiptResponse.builder()
                .id(1L)
                .originalFileName("receipt.pdf")
                .contentType("application/pdf")
                .fileSize(2048L)
                .uploadedAt(LocalDateTime.now())
                .transactionId(null)
                .build();

        when(receiptService.unlinkTransaction(1L)).thenReturn(unlinkedResponse);

        ResponseEntity<ReceiptResponse> result = receiptController.unlinkTransaction(1L);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertNotNull(result.getBody());
        assertEquals(null, result.getBody().getTransactionId());
    }

    @Test
    void deleteReceipt_Returns204NoContent() {
        doNothing().when(receiptService).deleteReceipt(1L);

        ResponseEntity<Void> result = receiptController.deleteReceipt(1L);

        assertEquals(HttpStatus.NO_CONTENT, result.getStatusCode());
        verify(receiptService, times(1)).deleteReceipt(1L);
    }
}

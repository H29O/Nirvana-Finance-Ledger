package com.heet.backend.service;

import com.heet.backend.dto.response.ReceiptFileResource;
import com.heet.backend.dto.response.ReceiptResponse;
import com.heet.backend.entity.Receipt;
import com.heet.backend.entity.Transaction;
import com.heet.backend.entity.User;
import com.heet.backend.exception.InvalidReceiptException;
import com.heet.backend.exception.ReceiptNotFoundException;
import com.heet.backend.exception.TransactionNotFoundException;
import com.heet.backend.repository.ReceiptRepository;
import com.heet.backend.repository.TransactionRepository;
import com.heet.backend.security.UserPrincipal;
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
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReceiptServiceTest {

    @Mock
    private ReceiptRepository receiptRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private ReceiptStorageService receiptStorageService;

    @InjectMocks
    private ReceiptService receiptService;

    private User testUser;
    private Transaction testTransaction;
    private Receipt sampleReceipt;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setName("Test User");
        testUser.setEmail("test@example.com");

        UserPrincipal principal = new UserPrincipal(testUser);
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);

        testTransaction = new Transaction();
        testTransaction.setId(100L);
        testTransaction.setUser(testUser);

        sampleReceipt = new Receipt();
        sampleReceipt.setId(10L);
        sampleReceipt.setUser(testUser);
        sampleReceipt.setOriginalFileName("test_receipt.pdf");
        sampleReceipt.setStoredFileName("uuid.pdf");
        sampleReceipt.setContentType("application/pdf");
        sampleReceipt.setFileSize(1024L);
        sampleReceipt.setStoragePath("uploads/receipts/1/uuid.pdf");
        sampleReceipt.setUploadedAt(LocalDateTime.now());
    }

    @Test
    void uploadReceipt_WithoutTransaction_Success() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "receipt.pdf",
                "application/pdf",
                "sample pdf content".getBytes()
        );

        when(receiptStorageService.storeFile(eq(1L), any(), any())).thenReturn("uploads/receipts/1/uuid.pdf");
        when(receiptRepository.save(any(Receipt.class))).thenAnswer(invocation -> {
            Receipt r = invocation.getArgument(0);
            r.setId(10L);
            return r;
        });

        ReceiptResponse response = receiptService.uploadReceipt(file, null);

        assertNotNull(response);
        assertEquals(10L, response.getId());
        assertEquals("receipt.pdf", response.getOriginalFileName());
        assertEquals("application/pdf", response.getContentType());
        assertNull(response.getTransactionId());
        verify(receiptStorageService, times(1)).storeFile(eq(1L), any(), any());
        verify(receiptRepository, times(1)).save(any(Receipt.class));
    }

    @Test
    void uploadReceipt_WithValidTransaction_Success() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "receipt.png",
                "image/png",
                "image bytes".getBytes()
        );

        when(transactionRepository.findByIdAndUserId(100L, 1L)).thenReturn(Optional.of(testTransaction));
        when(receiptRepository.existsByTransactionId(100L)).thenReturn(false);
        when(receiptStorageService.storeFile(eq(1L), any(), any())).thenReturn("uploads/receipts/1/uuid.png");
        when(receiptRepository.save(any(Receipt.class))).thenAnswer(invocation -> {
            Receipt r = invocation.getArgument(0);
            r.setId(10L);
            return r;
        });

        ReceiptResponse response = receiptService.uploadReceipt(file, 100L);

        assertNotNull(response);
        assertEquals(10L, response.getId());
        assertEquals(100L, response.getTransactionId());
    }

    @Test
    void uploadReceipt_EmptyFile_ThrowsInvalidReceiptException() {
        MockMultipartFile emptyFile = new MockMultipartFile(
                "file",
                "empty.pdf",
                "application/pdf",
                new byte[0]
        );

        assertThrows(InvalidReceiptException.class, () -> receiptService.uploadReceipt(emptyFile, null));
        verify(receiptStorageService, never()).storeFile(any(), any(), any());
    }

    @Test
    void uploadReceipt_UnsupportedMimeType_ThrowsInvalidReceiptException() {
        MockMultipartFile invalidMimeFile = new MockMultipartFile(
                "file",
                "document.txt",
                "text/plain",
                "text content".getBytes()
        );

        assertThrows(InvalidReceiptException.class, () -> receiptService.uploadReceipt(invalidMimeFile, null));
        verify(receiptStorageService, never()).storeFile(any(), any(), any());
    }

    @Test
    void uploadReceipt_PathTraversalFilename_ThrowsInvalidReceiptException() {
        MockMultipartFile invalidNameFile = new MockMultipartFile(
                "file",
                "../secret.pdf",
                "application/pdf",
                "content".getBytes()
        );

        assertThrows(InvalidReceiptException.class, () -> receiptService.uploadReceipt(invalidNameFile, null));
    }

    @Test
    void uploadReceipt_TransactionAlreadyHasReceipt_ThrowsInvalidReceiptException() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "receipt.jpg",
                "image/jpeg",
                "image content".getBytes()
        );

        when(transactionRepository.findByIdAndUserId(100L, 1L)).thenReturn(Optional.of(testTransaction));
        when(receiptRepository.existsByTransactionId(100L)).thenReturn(true);

        assertThrows(InvalidReceiptException.class, () -> receiptService.uploadReceipt(file, 100L));
    }

    @Test
    void uploadReceipt_TransactionNotFound_ThrowsTransactionNotFoundException() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "receipt.jpg",
                "image/jpeg",
                "image content".getBytes()
        );

        when(transactionRepository.findByIdAndUserId(999L, 1L)).thenReturn(Optional.empty());

        assertThrows(TransactionNotFoundException.class, () -> receiptService.uploadReceipt(file, 999L));
    }

    @Test
    void getReceipts_Success() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Receipt> page = new PageImpl<>(List.of(sampleReceipt), pageable, 1);

        when(receiptRepository.findByUserId(1L, pageable)).thenReturn(page);

        Page<ReceiptResponse> response = receiptService.getReceipts(pageable);

        assertNotNull(response);
        assertEquals(1, response.getTotalElements());
        assertEquals("test_receipt.pdf", response.getContent().get(0).getOriginalFileName());
    }

    @Test
    void getReceiptMetadata_Success() {
        when(receiptRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(sampleReceipt));

        ReceiptResponse response = receiptService.getReceiptMetadata(10L);

        assertNotNull(response);
        assertEquals(10L, response.getId());
        assertEquals("test_receipt.pdf", response.getOriginalFileName());
    }

    @Test
    void getReceiptMetadata_NotFound_ThrowsReceiptNotFoundException() {
        when(receiptRepository.findByIdAndUserId(99L, 1L)).thenReturn(Optional.empty());

        assertThrows(ReceiptNotFoundException.class, () -> receiptService.getReceiptMetadata(99L));
    }

    @Test
    void getReceiptFile_Success() {
        Resource mockResource = new ByteArrayResource("file content".getBytes());
        when(receiptRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(sampleReceipt));
        when(receiptStorageService.loadFileAsResource("uploads/receipts/1/uuid.pdf")).thenReturn(mockResource);

        ReceiptFileResource result = receiptService.getReceiptFile(10L);

        assertNotNull(result);
        assertEquals("application/pdf", result.getContentType());
        assertEquals("test_receipt.pdf", result.getOriginalFileName());
        assertEquals(mockResource, result.getResource());
    }

    @Test
    void linkTransaction_Success() {
        when(receiptRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(sampleReceipt));
        when(transactionRepository.findByIdAndUserId(100L, 1L)).thenReturn(Optional.of(testTransaction));
        when(receiptRepository.existsByTransactionIdAndIdNot(100L, 10L)).thenReturn(false);
        when(receiptRepository.save(any(Receipt.class))).thenAnswer(i -> i.getArgument(0));

        ReceiptResponse response = receiptService.linkTransaction(10L, 100L);

        assertNotNull(response);
        assertEquals(100L, response.getTransactionId());
    }

    @Test
    void linkTransaction_AlreadyLinkedToAnotherReceipt_ThrowsInvalidReceiptException() {
        when(receiptRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(sampleReceipt));
        when(transactionRepository.findByIdAndUserId(100L, 1L)).thenReturn(Optional.of(testTransaction));
        when(receiptRepository.existsByTransactionIdAndIdNot(100L, 10L)).thenReturn(true);

        assertThrows(InvalidReceiptException.class, () -> receiptService.linkTransaction(10L, 100L));
    }

    @Test
    void unlinkTransaction_Success() {
        sampleReceipt.setTransaction(testTransaction);
        when(receiptRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(sampleReceipt));
        when(receiptRepository.save(any(Receipt.class))).thenAnswer(i -> i.getArgument(0));

        ReceiptResponse response = receiptService.unlinkTransaction(10L);

        assertNotNull(response);
        assertNull(response.getTransactionId());
    }

    @Test
    void deleteReceipt_Success() {
        when(receiptRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(sampleReceipt));

        receiptService.deleteReceipt(10L);

        verify(receiptStorageService, times(1)).deleteFile("uploads/receipts/1/uuid.pdf");
        verify(receiptRepository, times(1)).delete(sampleReceipt);
    }

    @Test
    void getReceiptFile_PhysicalFileMissingOnDisk_ThrowsFileStorageException() {
        when(receiptRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(sampleReceipt));
        when(receiptStorageService.loadFileAsResource("uploads/receipts/1/uuid.pdf"))
                .thenThrow(new com.heet.backend.exception.FileStorageException("File not found on disk"));

        assertThrows(com.heet.backend.exception.FileStorageException.class,
                () -> receiptService.getReceiptFile(10L));
    }
}

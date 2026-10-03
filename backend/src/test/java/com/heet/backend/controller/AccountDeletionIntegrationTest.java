package com.heet.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.heet.backend.entity.*;
import com.heet.backend.enums.*;
import com.heet.backend.repository.*;
import com.heet.backend.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@Transactional
class AccountDeletionIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private LoanRecordRepository loanRecordRepository;

    @Autowired
    private ReceiptRepository receiptRepository;

    @Autowired
    private ReceiptOcrDataRepository receiptOcrDataRepository;

    @Autowired
    private EmailVerificationRepository emailVerificationRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockitoBean
    private JavaMailSender mailSender;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();
    }

    @Test
    void deleteCurrentUser_AuthenticatedUser_DeletesUserAndAllAssociatedRecords() throws Exception {
        // 1. Create primary user
        User user = new User();
        user.setName("Delete Me");
        user.setEmail("deleteme_" + System.currentTimeMillis() + "@example.com");
        user.setPasswordHash(passwordEncoder.encode("secret123"));
        user.setEmailVerified(true);
        user = userRepository.save(user);
        Long userId = user.getId();

        // 2. Create Category for user
        Category category = new Category();
        category.setName("Groceries");
        category.setUser(user);
        category.setColor("#10b981");
        category.setIsDefault(false);
        category = categoryRepository.save(category);

        // 3. Create Transaction for user
        Transaction transaction = new Transaction();
        transaction.setUser(user);
        transaction.setCategory(category);
        transaction.setAmount(new BigDecimal("120.50"));
        transaction.setTransactionType(TransactionType.EXPENSE);
        transaction.setPaymentMethod(PaymentMethod.DIGITAL);
        transaction.setEntrySource(EntrySource.MANUAL);
        transaction.setTransactionDate(LocalDateTime.now());
        transaction.setMerchant("Local Supermarket");
        transaction = transactionRepository.save(transaction);

        // 4. Create Receipt for user and transaction
        Receipt receipt = new Receipt();
        receipt.setUser(user);
        receipt.setTransaction(transaction);
        receipt.setOriginalFileName("test_receipt.jpg");
        receipt.setStoredFileName("stored_uuid.jpg");
        receipt.setContentType("image/jpeg");
        receipt.setFileSize(1024L);
        receipt.setStoragePath("uploads/receipts/" + userId + "/stored_uuid.jpg");
        receipt = receiptRepository.save(receipt);

        // 5. Create ReceiptOcrData for receipt and user
        ReceiptOcrData ocrData = new ReceiptOcrData();
        ocrData.setReceipt(receipt);
        ocrData.setUser(user);
        ocrData.setStatus(OcrStatus.COMPLETED);
        ocrData.setDetectedAmount(new BigDecimal("120.50"));
        ocrData.setDetectedMerchant("Local Supermarket");
        receiptOcrDataRepository.save(ocrData);

        // 6. Create LoanRecord for user
        LoanRecord loan = new LoanRecord();
        loan.setUser(user);
        loan.setType(LoanType.LENT);
        loan.setPersonName("Friend Bob");
        loan.setOriginalAmount(new BigDecimal("500.00"));
        loan.setRemainingAmount(new BigDecimal("500.00"));
        loan.setStatus(LoanStatus.ACTIVE);
        loan.setDueDate(LocalDate.now().plusDays(10));
        loanRecordRepository.save(loan);

        // 7. Create EmailVerification record for user
        EmailVerification verification = EmailVerification.builder()
                .user(user)
                .otpHash(passwordEncoder.encode("123456"))
                .expiresAt(LocalDateTime.now().plusMinutes(5))
                .status(EmailVerification.VerificationStatus.VERIFIED)
                .attemptCount(1)
                .build();
        emailVerificationRepository.save(verification);

        // Generate JWT token for this user
        String token = jwtTokenProvider.generateToken(user.getEmail());

        // Perform DELETE /api/users/me
        mockMvc.perform(delete("/api/users/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Your account has been permanently deleted."));

        // Assert all user records in PostgreSQL are completely gone
        assertFalse(userRepository.findById(userId).isPresent(), "User entity must be deleted");
        assertEquals(0, receiptOcrDataRepository.findByReceiptId(receipt.getId()).stream().count());
        assertEquals(0, receiptRepository.findByIdAndUserId(receipt.getId(), userId).stream().count());
        assertEquals(0, transactionRepository.findByUserId(userId, null).getTotalElements());
        assertEquals(0, categoryRepository.findByUserId(userId).size());
        assertEquals(0, loanRecordRepository.findByUserId(userId, null).getTotalElements());
        assertEquals(0, emailVerificationRepository.findByUserIdAndStatus(userId, EmailVerification.VerificationStatus.VERIFIED).size());
    }

    @Test
    void deleteCurrentUser_UnauthenticatedRequest_Returns401() throws Exception {
        mockMvc.perform(delete("/api/users/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void deleteCurrentUser_DoesNotAffectOtherUsersData() throws Exception {
        // User A (to be deleted)
        User userA = new User();
        userA.setName("User A");
        userA.setEmail("userA_" + System.currentTimeMillis() + "@example.com");
        userA.setPasswordHash(passwordEncoder.encode("passA"));
        userA.setEmailVerified(true);
        userA = userRepository.save(userA);

        Category catA = new Category();
        catA.setName("Cat A");
        catA.setUser(userA);
        catA.setColor("#10b981");
        catA.setIsDefault(false);
        categoryRepository.save(catA);

        // User B (must remain untouched)
        User userB = new User();
        userB.setName("User B");
        userB.setEmail("userB_" + System.currentTimeMillis() + "@example.com");
        userB.setPasswordHash(passwordEncoder.encode("passB"));
        userB.setEmailVerified(true);
        userB = userRepository.save(userB);

        Category catB = new Category();
        catB.setName("Cat B");
        catB.setUser(userB);
        catB.setColor("#6366f1");
        catB.setIsDefault(false);
        categoryRepository.save(catB);

        String tokenA = jwtTokenProvider.generateToken(userA.getEmail());

        // User A deletes account
        mockMvc.perform(delete("/api/users/me")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk());

        // Verify User A is deleted
        assertFalse(userRepository.findById(userA.getId()).isPresent());
        assertEquals(0, categoryRepository.findByUserId(userA.getId()).size());

        // Verify User B is completely intact
        assertTrue(userRepository.findById(userB.getId()).isPresent(), "User B must remain in database");
        assertEquals(1, categoryRepository.findByUserId(userB.getId()).size(), "User B's category must be preserved");
    }
}

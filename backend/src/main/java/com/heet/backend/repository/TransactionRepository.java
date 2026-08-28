package com.heet.backend.repository;

import com.heet.backend.dto.projection.CategoryExpenseProjection;
import com.heet.backend.dto.projection.MonthlyTypeSumProjection;
import com.heet.backend.entity.Transaction;
import com.heet.backend.enums.PaymentMethod;
import com.heet.backend.enums.TransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    Optional<Transaction> findByIdAndUserId(Long id, Long userId);

    Page<Transaction> findByUserId(Long userId, Pageable pageable);

    Page<Transaction> findByUserIdAndTransactionType(Long userId, TransactionType type, Pageable pageable);

    Page<Transaction> findByUserIdAndPaymentMethod(Long userId, PaymentMethod paymentMethod, Pageable pageable);

    Page<Transaction> findByUserIdAndTransactionDateBetween(Long userId, LocalDateTime start, LocalDateTime end, Pageable pageable);

    boolean existsByCategoryIdAndUserId(Long categoryId, Long userId);

    List<Transaction> findByTransactionType(TransactionType type);

    List<Transaction> findByPaymentMethod(PaymentMethod paymentMethod);

    List<Transaction> findByTransactionDateBetween(LocalDateTime start, LocalDateTime end);

    boolean existsByCategoryId(Long categoryId);

    @Query("SELECT SUM(t.amount) FROM Transaction t WHERE t.user.id = :userId AND t.transactionType = :type AND t.transactionDate BETWEEN :startDate AND :endDate")
    BigDecimal sumAmountByUserIdAndTransactionTypeAndDateBetween(
            @Param("userId") Long userId,
            @Param("type") TransactionType type,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    @Query("SELECT COUNT(t) FROM Transaction t WHERE t.user.id = :userId AND t.transactionDate BETWEEN :startDate AND :endDate")
    Long countByUserIdAndTransactionDateBetween(
            @Param("userId") Long userId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    @Query("SELECT c.id AS categoryId, c.name AS categoryName, c.color AS color, c.icon AS icon, SUM(t.amount) AS totalAmount, COUNT(t) AS transactionCount " +
           "FROM Transaction t LEFT JOIN t.category c " +
           "WHERE t.user.id = :userId AND t.transactionType = :type AND t.transactionDate BETWEEN :startDate AND :endDate " +
           "GROUP BY c.id, c.name, c.color, c.icon " +
           "ORDER BY SUM(t.amount) DESC")
    List<CategoryExpenseProjection> findCategoryExpenseBreakdownByUserId(
            @Param("userId") Long userId,
            @Param("type") TransactionType type,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    @Query("SELECT c.id AS categoryId, c.name AS categoryName, c.color AS color, c.icon AS icon, SUM(t.amount) AS totalAmount, COUNT(t) AS transactionCount " +
           "FROM Transaction t LEFT JOIN t.category c " +
           "WHERE t.user.id = :userId AND t.transactionType = :type AND t.transactionDate BETWEEN :startDate AND :endDate " +
           "GROUP BY c.id, c.name, c.color, c.icon " +
           "ORDER BY SUM(t.amount) DESC")
    List<CategoryExpenseProjection> findTopExpenseCategoriesByUserId(
            @Param("userId") Long userId,
            @Param("type") TransactionType type,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable);

    @Query("SELECT YEAR(t.transactionDate) AS year, " +
           "MONTH(t.transactionDate) AS month, " +
           "t.transactionType AS transactionType, " +
           "SUM(t.amount) AS totalAmount " +
           "FROM Transaction t " +
           "WHERE t.user.id = :userId " +
           "AND t.transactionType IN (com.heet.backend.enums.TransactionType.INCOME, com.heet.backend.enums.TransactionType.EXPENSE) " +
           "AND t.transactionDate BETWEEN :startDate AND :endDate " +
           "GROUP BY YEAR(t.transactionDate), MONTH(t.transactionDate), t.transactionType " +
           "ORDER BY YEAR(t.transactionDate) ASC, MONTH(t.transactionDate) ASC")
    List<MonthlyTypeSumProjection> findMonthlyTypeSumsByUserId(
            @Param("userId") Long userId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    @Query("SELECT SUM(t.amount) FROM Transaction t WHERE t.transactionType = :type AND t.transactionDate BETWEEN :startDate AND :endDate")
    BigDecimal sumAmountByTransactionTypeAndDateBetween(@Param("type") TransactionType type, @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    @Query("SELECT COUNT(t) FROM Transaction t WHERE t.transactionDate BETWEEN :startDate AND :endDate")
    Long countByTransactionDateBetween(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    @Query("SELECT c.id AS categoryId, c.name AS categoryName, c.color AS color, c.icon AS icon, SUM(t.amount) AS totalAmount, COUNT(t) AS transactionCount " +
           "FROM Transaction t LEFT JOIN t.category c " +
           "WHERE t.transactionType = :type AND t.transactionDate BETWEEN :startDate AND :endDate " +
           "GROUP BY c.id, c.name, c.color, c.icon " +
           "ORDER BY SUM(t.amount) DESC")
    List<CategoryExpenseProjection> findCategoryExpenseBreakdown(@Param("type") TransactionType type, @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    @Query("SELECT c.id AS categoryId, c.name AS categoryName, c.color AS color, c.icon AS icon, SUM(t.amount) AS totalAmount, COUNT(t) AS transactionCount " +
           "FROM Transaction t LEFT JOIN t.category c " +
           "WHERE t.transactionType = :type AND t.transactionDate BETWEEN :startDate AND :endDate " +
           "GROUP BY c.id, c.name, c.color, c.icon " +
           "ORDER BY SUM(t.amount) DESC")
    List<CategoryExpenseProjection> findTopExpenseCategories(@Param("type") TransactionType type, @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate, Pageable pageable);
}

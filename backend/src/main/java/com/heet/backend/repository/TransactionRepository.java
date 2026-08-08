package com.heet.backend.repository;

import com.heet.backend.dto.projection.CategoryExpenseProjection;
import com.heet.backend.entity.Transaction;
import com.heet.backend.enums.PaymentMethod;
import com.heet.backend.enums.TransactionType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    List<Transaction> findByTransactionType(TransactionType type);

    List<Transaction> findByPaymentMethod(PaymentMethod paymentMethod);

    List<Transaction> findByTransactionDateBetween(LocalDateTime start, LocalDateTime end);

    boolean existsByCategoryId(Long categoryId);

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

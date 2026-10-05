package com.heet.backend.repository;

import com.heet.backend.entity.PasswordResetToken;
import com.heet.backend.entity.PasswordResetToken.ResetTokenStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    Optional<PasswordResetToken> findByTokenHash(String tokenHash);

    List<PasswordResetToken> findByUserIdAndStatus(Long userId, ResetTokenStatus status);

    Optional<PasswordResetToken> findTopByUserIdOrderByCreatedAtDesc(Long userId);

    @Modifying
    @Query("UPDATE PasswordResetToken p SET p.status = 'INVALIDATED' WHERE p.user.id = :userId AND p.status = 'PENDING'")
    void invalidateAllPendingForUser(@Param("userId") Long userId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM PasswordResetToken p WHERE p.user.id = :userId")
    int deleteByUserId(@Param("userId") Long userId);
}

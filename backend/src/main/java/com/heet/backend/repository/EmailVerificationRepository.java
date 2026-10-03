package com.heet.backend.repository;

import com.heet.backend.entity.EmailVerification;
import com.heet.backend.entity.EmailVerification.VerificationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EmailVerificationRepository extends JpaRepository<EmailVerification, Long> {

    Optional<EmailVerification> findTopByUserIdOrderByCreatedAtDesc(Long userId);

    Optional<EmailVerification> findTopByUserIdAndStatusOrderByCreatedAtDesc(Long userId, VerificationStatus status);

    List<EmailVerification> findByUserIdAndStatus(Long userId, VerificationStatus status);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM EmailVerification e WHERE e.user.id = :userId")
    int deleteByUserId(@Param("userId") Long userId);
}

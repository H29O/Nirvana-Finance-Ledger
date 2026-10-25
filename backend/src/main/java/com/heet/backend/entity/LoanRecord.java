package com.heet.backend.entity;

import com.heet.backend.enums.LoanStatus;
import com.heet.backend.enums.LoanType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "loan_records")
public class LoanRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LoanType type;

    @Column(nullable = false)
    private String personName;

    private String description;

    @Column(nullable = false)
    private BigDecimal originalAmount;

    @Column(nullable = false)
    private BigDecimal remainingAmount;

    @Column(nullable = false)
    private BigDecimal additionalAmount = BigDecimal.ZERO;

    @Version
    @Column(nullable = false)
    private Long version = 0L;

    @OneToMany(mappedBy = "loanRecord", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("eventDate ASC, id ASC")
    private java.util.List<LoanEvent> events = new java.util.ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LoanStatus status;

    private LocalDate dueDate;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (additionalAmount == null) {
            additionalAmount = BigDecimal.ZERO;
        }
        if (version == null) {
            version = 0L;
        }
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public BigDecimal getTotalPrincipal() {
        BigDecimal orig = originalAmount != null ? originalAmount : BigDecimal.ZERO;
        BigDecimal add = additionalAmount != null ? additionalAmount : BigDecimal.ZERO;
        return orig.add(add);
    }

    public BigDecimal getTotalRepaid() {
        BigDecimal rem = remainingAmount != null ? remainingAmount : BigDecimal.ZERO;
        BigDecimal diff = getTotalPrincipal().subtract(rem);
        return diff.compareTo(BigDecimal.ZERO) > 0 ? diff : BigDecimal.ZERO;
    }
}

package com.agroconnect.entity;

import com.agroconnect.enums.PaymentMethod;
import com.agroconnect.enums.PaymentStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "payment_records", indexes = {
        @Index(name = "idx_payment_deal", columnList = "deal_id")
})
@Getter @Setter
public class PaymentRecord {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "deal_id")
    private Deal deal;

    @Column(name = "payment_date", nullable = false) private LocalDate paymentDate;

    @Column(nullable = false, precision = 14, scale = 2) private BigDecimal amount;

    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20)
    private PaymentMethod method;

    @Column(name = "reference_no", length = 100) private String referenceNo;
    @Column(length = 300) private String notes;

    // A user-entered record starts as RECORDED. VERIFIED needs a real process (not built).
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 25)
    private PaymentStatus status = PaymentStatus.RECORDED;

    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "recorded_by")
    private User recordedBy;

    @Column(name = "created_at", nullable = false, updatable = false) private LocalDateTime createdAt;

    @PrePersist void onCreate() { createdAt = LocalDateTime.now(); }
}

package com.agroconnect.entity;

import com.agroconnect.enums.SupplyStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "supply_records", indexes = {
        @Index(name = "idx_supply_deal", columnList = "deal_id")
})
@Getter @Setter
public class SupplyRecord {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "deal_id")
    private Deal deal;

    @Column(name = "delivery_date", nullable = false) private LocalDate deliveryDate;

    @Column(name = "quantity_kg", nullable = false, precision = 12, scale = 2)
    private BigDecimal quantityKg;

    @Column(length = 300) private String notes;

    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20)
    private SupplyStatus status = SupplyStatus.RECORDED;

    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "recorded_by")
    private User recordedBy;

    @Column(name = "created_at", nullable = false, updatable = false) private LocalDateTime createdAt;

    @PrePersist void onCreate() { createdAt = LocalDateTime.now(); }
}

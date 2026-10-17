package com.agroconnect.entity;

import com.agroconnect.enums.RequirementStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "merchant_requirements", indexes = {
        @Index(name = "idx_req_category_status", columnList = "category_id, status")
})
@Getter @Setter
public class MerchantRequirement {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "merchant_id")
    private User merchant;

    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "category_id")
    private FruitCategory category;

    @Column(length = 80) private String variety;

    @Column(name = "quantity_kg", nullable = false, precision = 12, scale = 2)
    private BigDecimal quantityKg;

    @Column(name = "target_price_min", precision = 10, scale = 2) private BigDecimal targetPriceMin;
    @Column(name = "target_price_max", nullable = false, precision = 10, scale = 2) private BigDecimal targetPriceMax;

    @Column(name = "quality_grade", length = 40) private String qualityGrade;
    @Column(name = "required_date") private LocalDate requiredDate;
    @Column(name = "preferred_area", length = 120) private String preferredArea;
    @Column(name = "delivery_location", length = 160) private String deliveryLocation;

    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20)
    private RequirementStatus status = RequirementStatus.ACTIVE;

    @Column(name = "created_at", nullable = false, updatable = false) private LocalDateTime createdAt;
    @Column(name = "updated_at", nullable = false) private LocalDateTime updatedAt;

    @PrePersist void onCreate() { createdAt = updatedAt = LocalDateTime.now(); }
    @PreUpdate  void onUpdate() { updatedAt = LocalDateTime.now(); }
}

package com.agroconnect.entity;

import com.agroconnect.enums.DealStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "deals", indexes = {
        @Index(name = "idx_deal_farmer", columnList = "farmer_id"),
        @Index(name = "idx_deal_merchant", columnList = "merchant_id")
})
@Getter @Setter
public class Deal {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "farmer_id")
    private User farmer;

    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "merchant_id")
    private User merchant;

    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "category_id")
    private FruitCategory category;                       // which fruit the deal is about

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "listing_id")
    private ProduceListing listing;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "requirement_id")
    private MerchantRequirement requirement;

    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "created_by")
    private User createdBy;

    @Column(name = "agreed_quantity_kg", nullable = false, precision = 12, scale = 2)
    private BigDecimal agreedQuantityKg;

    @Column(name = "agreed_price_per_kg", nullable = false, precision = 10, scale = 2)
    private BigDecimal agreedPricePerKg;

    // Calculated by the SERVICE (quantity x price). Never trusted from the browser.
    @Column(name = "total_value", nullable = false, precision = 14, scale = 2)
    private BigDecimal totalValue;

    @Column(name = "agreement_date", nullable = false) private LocalDate agreementDate;
    @Column(length = 1000) private String terms;

    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 25)
    private DealStatus status = DealStatus.PENDING_CONFIRMATION;

    @Column(name = "created_at", nullable = false, updatable = false) private LocalDateTime createdAt;
    @Column(name = "updated_at", nullable = false) private LocalDateTime updatedAt;

    @PrePersist void onCreate() { createdAt = updatedAt = LocalDateTime.now(); }
    @PreUpdate  void onUpdate() { updatedAt = LocalDateTime.now(); }
}

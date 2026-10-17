package com.agroconnect.entity;

import com.agroconnect.enums.ListingStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "produce_listings", indexes = {
        @Index(name = "idx_listing_category_status", columnList = "category_id, status"),
        @Index(name = "idx_listing_district", columnList = "district")
})
@Getter @Setter
public class ProduceListing {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "farmer_id")
    private User farmer;

    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "category_id")
    private FruitCategory category;

    @Column(length = 80) private String variety;

    @Column(name = "quantity_kg", nullable = false, precision = 12, scale = 2)
    private BigDecimal quantityKg;                       // always stored in kg

    @Column(name = "expected_price_per_kg", nullable = false, precision = 10, scale = 2)
    private BigDecimal expectedPricePerKg;

    @Column(name = "quality_grade", length = 40) private String qualityGrade;
    @Column(name = "available_from") private LocalDate availableFrom;

    // Produce location (can differ from the farmer's account location)
    @Column(nullable = false, length = 60) private String state;
    @Column(nullable = false, length = 60) private String district;
    @Column(length = 60) private String taluka;
    @Column(name = "village_or_city", nullable = false, length = 80) private String villageOrCity;

    @Column(length = 500) private String description;

    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20)
    private ListingStatus status = ListingStatus.ACTIVE;

    @OneToMany(mappedBy = "listing", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProduceImage> images = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false) private LocalDateTime createdAt;
    @Column(name = "updated_at", nullable = false) private LocalDateTime updatedAt;

    @PrePersist void onCreate() { createdAt = updatedAt = LocalDateTime.now(); }
    @PreUpdate  void onUpdate() { updatedAt = LocalDateTime.now(); }
}

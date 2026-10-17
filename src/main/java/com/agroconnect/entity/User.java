package com.agroconnect.entity;

import com.agroconnect.enums.Language;
import com.agroconnect.enums.Role;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(name = "users", indexes = {
        @Index(name = "idx_users_role", columnList = "role"),
        @Index(name = "idx_users_district", columnList = "district")
})
@Getter @Setter
public class User {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20)
    private Role role;

    @Column(nullable = false, length = 100)
    private String name;                 // farmer name or merchant contact person

    @Column(nullable = false, unique = true, length = 150)
    private String email;

    @Column(nullable = false, length = 20)
    private String phone;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;         // never the plain password

    @Enumerated(EnumType.STRING) @Column(name = "preferred_language", nullable = false, length = 5)
    private Language preferredLanguage = Language.EN;

    // General location (used for recommendations; no street address on purpose)
    @Column(nullable = false, length = 60) private String state;
    @Column(nullable = false, length = 60) private String district;
    @Column(length = 60) private String taluka;
    @Column(name = "village_or_city", nullable = false, length = 80) private String villageOrCity;
    @Column(name = "pin_code", nullable = false, length = 6) private String pinCode;

    // Farmer-only (nullable)
    @Column(name = "farm_name", length = 120) private String farmName;

    // Merchant-only (nullable)
    @Column(name = "business_name", length = 120) private String businessName;
    @Column(name = "business_type", length = 60) private String businessType;
    @Column(name = "business_description", length = 500) private String businessDescription;

    @Column(name = "avatar_url") private String avatarUrl;   // optional illustrated avatar

    @Column(nullable = false) private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false) private LocalDateTime createdAt;
    @Column(name = "updated_at", nullable = false) private LocalDateTime updatedAt;

    @PrePersist void onCreate() { createdAt = updatedAt = LocalDateTime.now(); }
    @PreUpdate  void onUpdate() { updatedAt = LocalDateTime.now(); }
}

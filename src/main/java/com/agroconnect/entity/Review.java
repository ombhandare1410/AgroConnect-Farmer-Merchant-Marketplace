package com.agroconnect.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(name = "reviews",
        uniqueConstraints = @UniqueConstraint(name = "uk_review_deal_reviewer",
                                              columnNames = {"deal_id", "reviewer_id"}),
        indexes = @Index(name = "idx_review_reviewee", columnList = "reviewee_id"))
@Getter @Setter
public class Review {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "deal_id")
    private Deal deal;

    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "reviewer_id")
    private User reviewer;

    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "reviewee_id")
    private User reviewee;

    @Column(nullable = false) private int rating;           // 1 to 5, validated in the service
    @Column(length = 500) private String comment;

    @Column(nullable = false) private boolean hidden = false;   // set by admin moderation

    @Column(name = "created_at", nullable = false, updatable = false) private LocalDateTime createdAt;

    @PrePersist void onCreate() { createdAt = LocalDateTime.now(); }
}

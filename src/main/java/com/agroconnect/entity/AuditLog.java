package com.agroconnect.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

/** One row per sensitive administrator action. Rows are only ever added, never edited. */
@Entity
@Table(name = "audit_logs", indexes = @Index(name = "idx_audit_created", columnList = "created_at"))
@Getter @Setter
public class AuditLog {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "actor_id")
    private User actor;

    @Column(nullable = false, length = 40) private String action;            // e.g. USER_DEACTIVATED
    @Column(name = "target_type", nullable = false, length = 30) private String targetType;
    @Column(name = "target_id") private Long targetId;
    @Column(length = 300) private String details;

    @Column(name = "created_at", nullable = false, updatable = false) private LocalDateTime createdAt;

    @PrePersist void onCreate() { createdAt = LocalDateTime.now(); }
}

package com.agroconnect.entity;

import com.agroconnect.enums.ComplaintStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(name = "complaints")
@Getter @Setter
public class Complaint {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "reporter_id")
    private User reporter;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "reported_user_id")
    private User reportedUser;                              // optional

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "review_id")
    private Review review;                                  // optional

    @Column(nullable = false, length = 500) private String reason;

    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20)
    private ComplaintStatus status = ComplaintStatus.OPEN;

    @Column(name = "admin_note", length = 500) private String adminNote;

    // Filled in when the administrator closes the report (nullable: old rows have no value)
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "resolved_by")
    private User resolvedBy;
    @Column(name = "resolved_at") private LocalDateTime resolvedAt;

    @Column(name = "created_at", nullable = false, updatable = false) private LocalDateTime createdAt;

    @PrePersist void onCreate() { createdAt = LocalDateTime.now(); }
}

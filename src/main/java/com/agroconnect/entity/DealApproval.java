package com.agroconnect.entity;

import com.agroconnect.enums.DealRequestStatus;
import com.agroconnect.enums.DealRequestType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

/**
 * One approval request between the two people of a deal (new table deal_approvals).
 * Stores who asked, who decided, what they decided, and when. The deals table itself is unchanged.
 */
@Entity
@Table(name = "deal_approvals", indexes = {
        @Index(name = "idx_dealappr_deal", columnList = "deal_id, type, status")
})
@Getter @Setter
public class DealApproval {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "deal_id")
    private Deal deal;

    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20)
    private DealRequestType type;

    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20)
    private DealRequestStatus status = DealRequestStatus.PENDING;

    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "requested_by")
    private User requestedBy;

    @Column(name = "requested_at", nullable = false) private LocalDateTime requestedAt;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "decided_by")
    private User decidedBy;

    @Column(name = "decided_at") private LocalDateTime decidedAt;

    @Column(length = 300) private String reason;          // optional reason given when rejecting

    @PrePersist void onCreate() { if (requestedAt == null) requestedAt = LocalDateTime.now(); }
}

package com.agroconnect.entity;

import com.agroconnect.enums.AppointmentStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "appointments", indexes = {
        @Index(name = "idx_appt_farmer", columnList = "farmer_id"),
        @Index(name = "idx_appt_merchant", columnList = "merchant_id")
})
@Getter @Setter
public class Appointment {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "farmer_id")
    private User farmer;

    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "merchant_id")
    private User merchant;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "listing_id")
    private ProduceListing listing;                       // optional

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "requirement_id")
    private MerchantRequirement requirement;              // optional

    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "requested_by")
    private User requestedBy;

    @Column(name = "appointment_date", nullable = false) private LocalDate appointmentDate;
    @Column(name = "appointment_time", nullable = false) private LocalTime appointmentTime;
    @Column(name = "meeting_location", nullable = false, length = 200) private String meetingLocation;
    @Column(length = 300) private String note;

    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 25)
    private AppointmentStatus status = AppointmentStatus.PENDING;

    @Column(name = "created_at", nullable = false, updatable = false) private LocalDateTime createdAt;
    @Column(name = "updated_at", nullable = false) private LocalDateTime updatedAt;

    @PrePersist void onCreate() { createdAt = updatedAt = LocalDateTime.now(); }
    @PreUpdate  void onUpdate() { updatedAt = LocalDateTime.now(); }
}

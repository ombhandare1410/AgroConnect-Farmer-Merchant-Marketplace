package com.agroconnect.repository;

import com.agroconnect.entity.Complaint;
import com.agroconnect.enums.ComplaintStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ComplaintRepository extends JpaRepository<Complaint, Long> {
    boolean existsByReporterIdAndReviewId(Long reporterId, Long reviewId);
    List<Complaint> findAllByOrderByCreatedAtDesc();
    long countByStatus(ComplaintStatus status);
}

package com.agroconnect.repository;

import com.agroconnect.entity.DealApproval;
import com.agroconnect.enums.DealRequestStatus;
import com.agroconnect.enums.DealRequestType;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface DealApprovalRepository extends JpaRepository<DealApproval, Long> {
    List<DealApproval> findByDealIdOrderByRequestedAtAsc(Long dealId);
    Optional<DealApproval> findFirstByDealIdAndTypeAndStatus(Long dealId, DealRequestType type, DealRequestStatus status);
}

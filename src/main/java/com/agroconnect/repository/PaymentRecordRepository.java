package com.agroconnect.repository;

import com.agroconnect.entity.PaymentRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PaymentRecordRepository extends JpaRepository<PaymentRecord, Long> {
    List<PaymentRecord> findByDealIdOrderByPaymentDateAscCreatedAtAsc(Long dealId);
}

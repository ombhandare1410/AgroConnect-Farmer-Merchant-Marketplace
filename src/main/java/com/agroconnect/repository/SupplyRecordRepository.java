package com.agroconnect.repository;

import com.agroconnect.entity.SupplyRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.math.BigDecimal;
import java.util.List;

public interface SupplyRecordRepository extends JpaRepository<SupplyRecord, Long> {

    List<SupplyRecord> findByDealIdOrderByDeliveryDateAscCreatedAtAsc(Long dealId);

    // Only CONFIRMED deliveries of COMPLETED deals count toward profile numbers. Sums are null when no rows.
    @Query("select sum(s.quantityKg) from SupplyRecord s where s.deal.merchant.id = :id " +
           "and s.status = com.agroconnect.enums.SupplyStatus.CONFIRMED " +
           "and s.deal.status = com.agroconnect.enums.DealStatus.COMPLETED")
    BigDecimal sumKgForMerchantCompletedDeals(@Param("id") Long merchantId);

    @Query("select sum(s.quantityKg) from SupplyRecord s where s.deal.farmer.id = :id " +
           "and s.status = com.agroconnect.enums.SupplyStatus.CONFIRMED " +
           "and s.deal.status = com.agroconnect.enums.DealStatus.COMPLETED")
    BigDecimal sumKgForFarmerCompletedDeals(@Param("id") Long farmerId);

    @Query("select s from SupplyRecord s join fetch s.deal d join fetch d.category " +
           "where d.merchant.id = :id and s.status = com.agroconnect.enums.SupplyStatus.CONFIRMED " +
           "and d.status = com.agroconnect.enums.DealStatus.COMPLETED")
    List<SupplyRecord> completedForMerchant(@Param("id") Long merchantId);

    @Query("select s from SupplyRecord s join fetch s.deal d join fetch d.category " +
           "where d.farmer.id = :id and s.status = com.agroconnect.enums.SupplyStatus.CONFIRMED " +
           "and d.status = com.agroconnect.enums.DealStatus.COMPLETED")
    List<SupplyRecord> completedForFarmer(@Param("id") Long farmerId);
}

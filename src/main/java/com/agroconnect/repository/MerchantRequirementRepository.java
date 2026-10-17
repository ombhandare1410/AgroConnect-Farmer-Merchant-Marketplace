package com.agroconnect.repository;

import com.agroconnect.entity.MerchantRequirement;
import com.agroconnect.enums.RequirementStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface MerchantRequirementRepository extends JpaRepository<MerchantRequirement, Long> {

    List<MerchantRequirement> findByMerchantIdOrderByCreatedAtDesc(Long merchantId);

    List<MerchantRequirement> findByMerchantIdAndStatus(Long merchantId, RequirementStatus status);

    @Query("select r from MerchantRequirement r join fetch r.merchant join fetch r.category " +
           "where r.status = com.agroconnect.enums.RequirementStatus.ACTIVE " +
           "and r.category.id = :catId and r.category.active = true and r.merchant.active = true")
    List<MerchantRequirement> findDiscoverable(@Param("catId") Long categoryId);
}

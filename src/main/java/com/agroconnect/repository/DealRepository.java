package com.agroconnect.repository;

import com.agroconnect.entity.Deal;
import com.agroconnect.enums.DealStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface DealRepository extends JpaRepository<Deal, Long> {
    long countByMerchantIdAndStatus(Long merchantId, DealStatus status);
    long countByFarmerIdAndStatus(Long farmerId, DealStatus status);
    long countByStatus(DealStatus status);
    boolean existsByFarmerIdAndMerchantIdAndStatusIn(Long farmerId, Long merchantId, Collection<DealStatus> statuses);

    @Query("select d from Deal d join fetch d.farmer join fetch d.merchant join fetch d.category " +
           "where d.farmer.id = :uid or d.merchant.id = :uid order by d.createdAt desc")
    List<Deal> findForUser(@Param("uid") Long userId);

    // Locks the deal row until the transaction ends, so two people cannot add records at the same instant
    // and push totals over the agreed quantity / value.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select d from Deal d where d.id = :id")
    Optional<Deal> lockById(@Param("id") Long id);
}

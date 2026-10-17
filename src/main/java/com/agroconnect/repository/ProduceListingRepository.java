package com.agroconnect.repository;

import com.agroconnect.entity.ProduceListing;
import com.agroconnect.enums.ListingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Collection;
import java.util.List;

public interface ProduceListingRepository extends JpaRepository<ProduceListing, Long> {

    List<ProduceListing> findByFarmerIdOrderByCreatedAtDesc(Long farmerId);

    List<ProduceListing> findByFarmerIdAndStatusOrderByCreatedAtDesc(Long farmerId, ListingStatus status);

    // Hard relevance rules: ACTIVE listing, in a wanted ACTIVE category, from an ACTIVE farmer
    @Query("select l from ProduceListing l join fetch l.farmer join fetch l.category " +
           "where l.status = com.agroconnect.enums.ListingStatus.ACTIVE " +
           "and l.category.id in :catIds and l.category.active = true and l.farmer.active = true")
    List<ProduceListing> findDiscoverable(@Param("catIds") Collection<Long> catIds);
}

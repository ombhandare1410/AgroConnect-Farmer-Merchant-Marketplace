package com.agroconnect.repository;

import com.agroconnect.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    @Query("select avg(r.rating) from Review r where r.reviewee.id = :id and r.hidden = false")
    Double averageVisibleRating(@Param("id") Long id);

    @Query("select count(r) from Review r where r.reviewee.id = :id and r.hidden = false")
    long countVisible(@Param("id") Long id);

    List<Review> findTop10ByRevieweeIdAndHiddenFalseOrderByCreatedAtDesc(Long revieweeId);

    boolean existsByDealIdAndReviewerId(Long dealId, Long reviewerId);
}

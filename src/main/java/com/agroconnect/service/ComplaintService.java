package com.agroconnect.service;

import com.agroconnect.entity.Complaint;
import com.agroconnect.entity.Review;
import com.agroconnect.entity.User;
import com.agroconnect.exception.ApiException;
import com.agroconnect.repository.ComplaintRepository;
import com.agroconnect.repository.ReviewRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ComplaintService {

    private final ComplaintRepository complaints;
    private final ReviewRepository reviews;

    public ComplaintService(ComplaintRepository complaints, ReviewRepository reviews) {
        this.complaints = complaints;
        this.reviews = reviews;
    }

    /** Anyone viewing a review may report it. The administrator reviews reports in the admin phase. */
    @Transactional
    public void reportReview(User me, Long reviewId, String reason) {
        Review review = reviews.findById(reviewId).filter(r -> !r.isHidden())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", "Review not found"));
        if (complaints.existsByReporterIdAndReviewId(me.getId(), reviewId)) {
            throw new ApiException(HttpStatus.CONFLICT, "ALREADY_REPORTED", "You already reported this review");
        }
        Complaint c = new Complaint();
        c.setReporter(me);
        c.setReview(review);
        c.setReportedUser(review.getReviewer());
        c.setReason(reason.trim());
        complaints.save(c);
    }
}

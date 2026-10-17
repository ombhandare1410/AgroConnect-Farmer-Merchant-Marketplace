package com.agroconnect.dto;

import com.agroconnect.enums.ComplaintStatus;
import java.time.LocalDateTime;

public record AdminComplaintView(Long id, ComplaintStatus status, String reason, String adminNote,
                                 String reporterName, String reportedUserName,
                                 Long reviewId, Integer reviewRating, String reviewComment, Boolean reviewHidden,
                                 LocalDateTime createdAt, LocalDateTime resolvedAt) { }

package com.agroconnect.dto;

import com.agroconnect.enums.Role;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** What one user may see about another. phone is null unless a confirmed meeting/deal allows it. No email, PIN or street address. */
public record PublicProfile(Long id, Role role, String displayName, String contactName, String farmName,
                            String businessType, String description, String location, String phone,
                            String avatarUrl, UserStats stats, List<HistoryLine> history,
                            List<RequirementResponse> requirements, List<ListingResponse> listings,
                            List<ReviewView> reviews) {

    public record HistoryLine(CategoryResponse category, long deals, BigDecimal totalKg) { }

    public record ReviewView(Long id, int rating, String comment, String reviewerName, LocalDateTime createdAt) { }
}

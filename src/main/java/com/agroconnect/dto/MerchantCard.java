package com.agroconnect.dto;

import java.math.BigDecimal;

/** One merchant buying requirement as shown to a farmer. No phone, no email, no street address. */
public record MerchantCard(Long requirementId, Long merchantId, String businessName, String location,
                           CategoryResponse category, String variety, BigDecimal quantityKg,
                           BigDecimal targetPriceMin, BigDecimal targetPriceMax, String qualityGrade,
                           Double avgRating, long reviewCount, long completedDeals, BigDecimal totalKgPurchased,
                           int matchRank, String matchLevel) { }

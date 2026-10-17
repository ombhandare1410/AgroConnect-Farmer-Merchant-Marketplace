package com.agroconnect.dto;

import java.math.BigDecimal;
import java.util.List;

/** One farmer produce listing as shown to a merchant. No phone, no email, no street address. */
public record FarmerCard(Long listingId, Long farmerId, String farmerName, String farmName, String location,
                         CategoryResponse category, String variety, BigDecimal quantityKg,
                         BigDecimal expectedPricePerKg, String qualityGrade, List<ListingResponse.ImageInfo> images,
                         Double avgRating, long reviewCount, long completedDeals,
                         int matchRank, String matchLevel) { }

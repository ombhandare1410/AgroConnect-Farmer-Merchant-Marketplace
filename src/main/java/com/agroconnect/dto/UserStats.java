package com.agroconnect.dto;

import java.math.BigDecimal;

/** Trust numbers, always calculated from stored records (never typed in). avgRating is null = "No ratings yet". */
public record UserStats(Double avgRating, long reviewCount, long completedDeals, BigDecimal totalKg) { }

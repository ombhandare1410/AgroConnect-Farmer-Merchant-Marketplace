package com.agroconnect.dto;

import com.agroconnect.enums.ListingStatus;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;

/** Quantity is ALWAYS in kg here. The browser converts tons to kg before sending. */
public record ListingRequest(
        @NotNull Long categoryId,
        @Size(max = 80) String variety,
        @NotNull @DecimalMin("0.01") @DecimalMax("1000000000") BigDecimal quantityKg,
        @NotNull @DecimalMin("0.01") @DecimalMax("100000") BigDecimal expectedPricePerKg,
        @Size(max = 40) String qualityGrade,
        LocalDate availableFrom,
        @Size(max = 60) String state,
        @Size(max = 60) String district,
        @Size(max = 60) String taluka,
        @Size(max = 80) String villageOrCity,      // location fields are optional: default = farmer's profile area
        @Size(max = 500) String description,
        ListingStatus status) { }

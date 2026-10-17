package com.agroconnect.dto;

import com.agroconnect.enums.RequirementStatus;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;

public record RequirementRequest(
        @NotNull Long categoryId,
        @Size(max = 80) String variety,
        @NotNull @DecimalMin("0.01") @DecimalMax("1000000000") BigDecimal quantityKg,
        @DecimalMin("0.01") @DecimalMax("100000") BigDecimal targetPriceMin,      // optional
        @NotNull @DecimalMin("0.01") @DecimalMax("100000") BigDecimal targetPriceMax,
        @Size(max = 40) String qualityGrade,
        LocalDate requiredDate,
        @Size(max = 120) String preferredArea,
        @Size(max = 160) String deliveryLocation,
        RequirementStatus status) { }

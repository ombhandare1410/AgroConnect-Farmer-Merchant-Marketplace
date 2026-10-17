package com.agroconnect.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;

/** There is NO total field on purpose: the server calculates quantity x price itself. */
public record DealRequest(
        @NotNull Long otherUserId,
        @NotNull Long categoryId,
        Long listingId,
        Long requirementId,
        @NotNull @DecimalMin("0.01") @DecimalMax("1000000000") BigDecimal agreedQuantityKg,
        @NotNull @DecimalMin("0.01") @DecimalMax("100000") BigDecimal agreedPricePerKg,
        LocalDate agreementDate,
        @Size(max = 1000) String terms) { }

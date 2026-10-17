package com.agroconnect.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;

public record SupplyRequest(@NotNull LocalDate deliveryDate,
                            @NotNull @DecimalMin("0.01") @DecimalMax("1000000000") BigDecimal quantityKg,
                            @Size(max = 300) String notes) { }

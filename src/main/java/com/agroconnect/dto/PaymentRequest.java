package com.agroconnect.dto;

import com.agroconnect.enums.PaymentMethod;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;

public record PaymentRequest(@NotNull LocalDate paymentDate,
                             @NotNull @DecimalMin("0.01") @DecimalMax("999999999999") BigDecimal amount,
                             @NotNull PaymentMethod method,
                             @Size(max = 100) String referenceNo,
                             @Size(max = 300) String notes) { }

package com.agroconnect.dto;

import jakarta.validation.constraints.*;

public record ReviewRequest(@NotNull @Min(1) @Max(5) Integer rating, @Size(max = 500) String comment) { }

package com.agroconnect.dto;

import jakarta.validation.constraints.NotNull;

public record SetActiveRequest(@NotNull Boolean active) { }

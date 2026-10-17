package com.agroconnect.dto;

import com.agroconnect.enums.Language;
import jakarta.validation.constraints.NotNull;

public record LanguageRequest(@NotNull Language language) { }

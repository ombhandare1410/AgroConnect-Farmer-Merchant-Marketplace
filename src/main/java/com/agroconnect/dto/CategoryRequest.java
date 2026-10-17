package com.agroconnect.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CategoryRequest(@NotBlank @Size(max = 80) String name,
                              @Size(max = 80) String nameMr,
                              @Size(max = 80) String nameHi,
                              Boolean active) { }

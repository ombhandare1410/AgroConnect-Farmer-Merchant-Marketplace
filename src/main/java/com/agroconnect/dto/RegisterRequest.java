package com.agroconnect.dto;

import com.agroconnect.enums.Language;
import com.agroconnect.enums.Role;
import jakarta.validation.constraints.*;

public record RegisterRequest(
        @NotNull Role role,
        @NotBlank @Size(max = 100) String name,
        @NotBlank @Pattern(regexp = "^[6-9]\\d{9}$", message = "Phone must be a 10-digit mobile number") String phone,
        @NotBlank @Email @Size(max = 150) String email,
        @NotBlank @Size(min = 8, max = 72) String password,
        Language preferredLanguage,
        @NotBlank @Size(max = 60) String state,
        @NotBlank @Size(max = 60) String district,
        @Size(max = 60) String taluka,
        @NotBlank @Size(max = 80) String villageOrCity,
        @NotBlank @Pattern(regexp = "^\\d{6}$", message = "PIN code must be 6 digits") String pinCode,
        @Size(max = 120) String farmName,
        @Size(max = 120) String businessName,
        @Size(max = 60) String businessType,
        @Size(max = 500) String businessDescription) { }

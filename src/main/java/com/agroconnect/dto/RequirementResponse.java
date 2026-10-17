package com.agroconnect.dto;

import com.agroconnect.entity.MerchantRequirement;
import com.agroconnect.enums.RequirementStatus;
import java.math.BigDecimal;
import java.time.LocalDate;

public record RequirementResponse(Long id, CategoryResponse category, String variety, BigDecimal quantityKg,
                                  BigDecimal targetPriceMin, BigDecimal targetPriceMax, String qualityGrade,
                                  LocalDate requiredDate, String preferredArea, String deliveryLocation,
                                  RequirementStatus status) {
    public static RequirementResponse from(MerchantRequirement r) {
        return new RequirementResponse(r.getId(), CategoryResponse.from(r.getCategory()), r.getVariety(),
                r.getQuantityKg(), r.getTargetPriceMin(), r.getTargetPriceMax(), r.getQualityGrade(),
                r.getRequiredDate(), r.getPreferredArea(), r.getDeliveryLocation(), r.getStatus());
    }
}

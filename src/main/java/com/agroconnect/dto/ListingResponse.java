package com.agroconnect.dto;

import com.agroconnect.entity.ProduceListing;
import com.agroconnect.enums.ListingStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record ListingResponse(Long id, CategoryResponse category, String variety, BigDecimal quantityKg,
                              BigDecimal expectedPricePerKg, String qualityGrade, LocalDate availableFrom,
                              String state, String district, String taluka, String villageOrCity,
                              String description, ListingStatus status, List<ImageInfo> images) {

    public record ImageInfo(Long id, String url) { }

    public static ListingResponse from(ProduceListing l) {
        return new ListingResponse(l.getId(), CategoryResponse.from(l.getCategory()), l.getVariety(),
                l.getQuantityKg(), l.getExpectedPricePerKg(), l.getQualityGrade(), l.getAvailableFrom(),
                l.getState(), l.getDistrict(), l.getTaluka(), l.getVillageOrCity(), l.getDescription(),
                l.getStatus(), l.getImages().stream().map(i -> new ImageInfo(i.getId(), i.getImagePath())).toList());
    }
}

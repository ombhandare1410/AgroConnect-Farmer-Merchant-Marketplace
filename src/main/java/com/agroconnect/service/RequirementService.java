package com.agroconnect.service;

import com.agroconnect.dto.RequirementRequest;
import com.agroconnect.dto.RequirementResponse;
import com.agroconnect.entity.FruitCategory;
import com.agroconnect.entity.MerchantRequirement;
import com.agroconnect.entity.User;
import com.agroconnect.enums.RequirementStatus;
import com.agroconnect.exception.ApiException;
import com.agroconnect.repository.FruitCategoryRepository;
import com.agroconnect.repository.MerchantRequirementRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class RequirementService {

    private final MerchantRequirementRepository requirements;
    private final FruitCategoryRepository categories;

    public RequirementService(MerchantRequirementRepository requirements, FruitCategoryRepository categories) {
        this.requirements = requirements;
        this.categories = categories;
    }

    @Transactional(readOnly = true)
    public List<RequirementResponse> mine(User merchant) {
        return requirements.findByMerchantIdOrderByCreatedAtDesc(merchant.getId())
                .stream().map(RequirementResponse::from).toList();
    }

    @Transactional
    public RequirementResponse create(User merchant, RequirementRequest r) {
        MerchantRequirement m = new MerchantRequirement();
        m.setMerchant(merchant);
        apply(m, r);
        return RequirementResponse.from(requirements.save(m));
    }

    /** Also used to pause or close a requirement (just send a different status). */
    @Transactional
    public RequirementResponse update(User merchant, Long id, RequirementRequest r) {
        MerchantRequirement m = requirements.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", "Requirement not found"));
        if (!m.getMerchant().getId().equals(merchant.getId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN", "This is not your requirement");
        }
        apply(m, r);
        return RequirementResponse.from(requirements.save(m));
    }

    private void apply(MerchantRequirement m, RequirementRequest r) {
        FruitCategory c = categories.findById(r.categoryId())
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION", "Unknown fruit category"));
        if (!c.isActive()) throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION", "This fruit category is not active");
        if (r.targetPriceMin() != null && r.targetPriceMin().compareTo(r.targetPriceMax()) > 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_PRICE_RANGE", "Highest price must be at least the lowest price");
        }
        m.setCategory(c);
        m.setVariety(blankToNull(r.variety()));
        m.setQuantityKg(r.quantityKg());
        m.setTargetPriceMin(r.targetPriceMin());
        m.setTargetPriceMax(r.targetPriceMax());
        m.setQualityGrade(blankToNull(r.qualityGrade()));
        m.setRequiredDate(r.requiredDate());
        m.setPreferredArea(blankToNull(r.preferredArea()));
        m.setDeliveryLocation(blankToNull(r.deliveryLocation()));
        m.setStatus(r.status() == null ? RequirementStatus.ACTIVE : r.status());
    }

    private static String blankToNull(String s) { return s == null || s.isBlank() ? null : s.trim(); }
}

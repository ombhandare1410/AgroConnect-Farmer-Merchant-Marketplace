package com.agroconnect.service;

import com.agroconnect.dto.*;
import com.agroconnect.entity.*;
import com.agroconnect.enums.ListingStatus;
import com.agroconnect.enums.RequirementStatus;
import com.agroconnect.enums.Role;
import com.agroconnect.exception.ApiException;
import com.agroconnect.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.*;

@Service
public class ProfileService {

    private final UserRepository users;
    private final MerchantRequirementRepository requirements;
    private final ProduceListingRepository listings;
    private final ReviewRepository reviews;
    private final SupplyRecordRepository supplies;
    private final StatsService stats;
    private final ContactAccessService contact;

    public ProfileService(UserRepository users, MerchantRequirementRepository requirements,
                          ProduceListingRepository listings, ReviewRepository reviews,
                          SupplyRecordRepository supplies, StatsService stats,
                          ContactAccessService contact) {
        this.users = users;
        this.requirements = requirements;
        this.listings = listings;
        this.reviews = reviews;
        this.supplies = supplies;
        this.stats = stats;
        this.contact = contact;
    }

    /** A farmer may view merchant profiles and a merchant may view farmer profiles. Anything else = not found. */
    @Transactional(readOnly = true)
    public PublicProfile view(User viewer, Long id) {
        User t = users.findById(id).filter(User::isActive)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", "Profile not found"));
        if (t.getRole() == Role.ADMIN || t.getRole() == viewer.getRole()) {
            throw new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", "Profile not found");
        }
        boolean merchant = t.getRole() == Role.MERCHANT;

        List<RequirementResponse> reqs = merchant
                ? requirements.findByMerchantIdAndStatus(id, RequirementStatus.ACTIVE).stream().map(RequirementResponse::from).toList()
                : List.of();
        List<ListingResponse> lists = merchant
                ? List.of()
                : listings.findByFarmerIdAndStatusOrderByCreatedAtDesc(id, ListingStatus.ACTIVE).stream().map(ListingResponse::from).toList();

        List<PublicProfile.ReviewView> revs = reviews.findTop10ByRevieweeIdAndHiddenFalseOrderByCreatedAtDesc(id).stream()
                .map(r -> new PublicProfile.ReviewView(r.getId(), r.getRating(), r.getComment(), nameOf(r.getReviewer()), r.getCreatedAt()))
                .toList();

        return new PublicProfile(t.getId(), t.getRole(), merchant ? t.getBusinessName() : t.getName(), t.getName(),
                t.getFarmName(), t.getBusinessType(), t.getBusinessDescription(),
                t.getVillageOrCity() + ", " + t.getDistrict() + ", " + t.getState(),
                contact.canSeePhone(viewer, t) ? t.getPhone() : null, t.getAvatarUrl(),
                stats.forUser(t), history(t), reqs, lists, revs);
    }

    @Transactional
    public User update(User u, UpdateProfileRequest r) {
        if (u.getRole() == Role.MERCHANT && (blank(r.businessName()) || blank(r.businessType()))) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION", "Business name and type are required");
        }
        u.setName(r.name().trim());
        u.setPhone(r.phone());
        u.setState(r.state().trim());
        u.setDistrict(r.district().trim());
        u.setTaluka(blank(r.taluka()) ? null : r.taluka().trim());
        u.setVillageOrCity(r.villageOrCity().trim());
        u.setPinCode(r.pinCode());
        if (u.getRole() == Role.FARMER) {
            u.setFarmName(blank(r.farmName()) ? null : r.farmName().trim());
        } else if (u.getRole() == Role.MERCHANT) {
            u.setBusinessName(r.businessName().trim());
            u.setBusinessType(r.businessType().trim());
            u.setBusinessDescription(blank(r.businessDescription()) ? null : r.businessDescription().trim());
        }
        return users.save(u);
    }

    /** Completed-deal summary per fruit, built from stored supply records of COMPLETED deals. */
    private List<PublicProfile.HistoryLine> history(User t) {
        List<SupplyRecord> rows = t.getRole() == Role.MERCHANT
                ? supplies.completedForMerchant(t.getId()) : supplies.completedForFarmer(t.getId());
        Map<Long, FruitCategory> cats = new LinkedHashMap<>();
        Map<Long, BigDecimal> kg = new HashMap<>();
        Map<Long, Set<Long>> dealIds = new HashMap<>();
        for (SupplyRecord s : rows) {
            Long cid = s.getDeal().getCategory().getId();
            cats.putIfAbsent(cid, s.getDeal().getCategory());
            kg.merge(cid, s.getQuantityKg(), BigDecimal::add);
            dealIds.computeIfAbsent(cid, k -> new HashSet<>()).add(s.getDeal().getId());
        }
        List<PublicProfile.HistoryLine> out = new ArrayList<>();
        cats.forEach((cid, c) -> out.add(new PublicProfile.HistoryLine(CategoryResponse.from(c),
                dealIds.get(cid).size(), kg.get(cid))));
        out.sort(Comparator.comparing(PublicProfile.HistoryLine::totalKg).reversed());
        return out;
    }

    private static String nameOf(User u) {
        return u.getRole() == Role.MERCHANT && u.getBusinessName() != null ? u.getBusinessName() : u.getName();
    }
    private static boolean blank(String s) { return s == null || s.isBlank(); }
}

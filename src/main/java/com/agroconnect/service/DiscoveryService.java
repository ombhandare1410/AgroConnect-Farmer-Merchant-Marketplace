package com.agroconnect.service;

import com.agroconnect.dto.*;
import com.agroconnect.entity.MerchantRequirement;
import com.agroconnect.entity.ProduceListing;
import com.agroconnect.entity.User;
import com.agroconnect.enums.RequirementStatus;
import com.agroconnect.repository.MerchantRequirementRepository;
import com.agroconnect.repository.ProduceListingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.*;

/**
 * Finds and ranks matches. Order of rules:
 * 1) fruit category + ACTIVE status are hard filters (done in the database query),
 * 2) the user's filters, 3) closeness of location, 4) price, 5) rating.
 * A nearby user who does not buy the fruit is never even in the list.
 */
@Service
public class DiscoveryService {

    private final MerchantRequirementRepository requirements;
    private final ProduceListingRepository listings;
    private final StatsService statsService;

    public DiscoveryService(MerchantRequirementRepository requirements, ProduceListingRepository listings,
                            StatsService statsService) {
        this.requirements = requirements;
        this.listings = listings;
        this.statsService = statsService;
    }

    /** Farmer looks for merchants who actively want a fruit. */
    @Transactional(readOnly = true)
    public List<MerchantCard> merchantsForFarmer(User farmer, Long categoryId, String location,
                                                 BigDecimal minQty, BigDecimal minPrice, Double minRating) {
        Map<Long, UserStats> cache = new HashMap<>();
        List<MerchantCard> cards = new ArrayList<>();
        for (MerchantRequirement r : requirements.findDiscoverable(categoryId)) {
            User m = r.getMerchant();
            if (!LocationMatcher.textMatches(location, m.getVillageOrCity(), m.getTaluka(), m.getDistrict())) continue;
            if (minQty != null && r.getQuantityKg().compareTo(minQty) < 0) continue;       // merchant needs at least this much
            if (minPrice != null && r.getTargetPriceMax().compareTo(minPrice) < 0) continue; // merchant can pay at least this
            UserStats s = cache.computeIfAbsent(m.getId(), id -> statsService.forUser(m));
            if (minRating != null && (s.avgRating() == null || s.avgRating() < minRating)) continue;

            int rank = LocationMatcher.rank(farmer.getState(), farmer.getDistrict(), farmer.getTaluka(),
                    farmer.getVillageOrCity(), farmer.getPinCode(),
                    m.getState(), m.getDistrict(), m.getTaluka(), m.getVillageOrCity(), m.getPinCode());
            cards.add(new MerchantCard(r.getId(), m.getId(), m.getBusinessName(), place(m),
                    CategoryResponse.from(r.getCategory()), r.getVariety(), r.getQuantityKg(),
                    r.getTargetPriceMin(), r.getTargetPriceMax(), r.getQualityGrade(),
                    s.avgRating(), s.reviewCount(), s.completedDeals(), s.totalKg(),
                    rank, LocationMatcher.code(rank)));
        }
        Comparator<MerchantCard> order = Comparator.comparingInt(MerchantCard::matchRank);
        order = order.thenComparing(MerchantCard::targetPriceMax, Comparator.reverseOrder());
        order = order.thenComparingDouble(c -> -(c.avgRating() == null ? 0.0 : c.avgRating()));
        cards.sort(order);
        return cards;
    }

    /** Merchant looks for farmers who have a fruit. If no fruit is chosen, use the merchant's active requirements. */
    @Transactional(readOnly = true)
    public List<FarmerCard> farmersForMerchant(User merchant, Long categoryId, String location,
                                               BigDecimal minQty, BigDecimal maxPrice, Double minRating) {
        Set<Long> categoryIds = new HashSet<>();
        if (categoryId != null) {
            categoryIds.add(categoryId);
        } else {
            requirements.findByMerchantIdAndStatus(merchant.getId(), RequirementStatus.ACTIVE)
                    .forEach(r -> categoryIds.add(r.getCategory().getId()));
        }
        if (categoryIds.isEmpty()) return List.of();

        Map<Long, UserStats> cache = new HashMap<>();
        List<FarmerCard> cards = new ArrayList<>();
        for (ProduceListing l : listings.findDiscoverable(categoryIds)) {
            User f = l.getFarmer();
            if (!LocationMatcher.textMatches(location, l.getVillageOrCity(), l.getTaluka(), l.getDistrict())) continue;
            if (minQty != null && l.getQuantityKg().compareTo(minQty) < 0) continue;
            if (maxPrice != null && l.getExpectedPricePerKg().compareTo(maxPrice) > 0) continue;
            UserStats s = cache.computeIfAbsent(f.getId(), id -> statsService.forUser(f));
            if (minRating != null && (s.avgRating() == null || s.avgRating() < minRating)) continue;

            // We only know the farmer's PIN code if the produce is at their registered place
            boolean atHome = LocationMatcher.rank(f.getState(), f.getDistrict(), f.getTaluka(), f.getVillageOrCity(), null,
                    l.getState(), l.getDistrict(), l.getTaluka(), l.getVillageOrCity(), null) == 0;
            int rank = LocationMatcher.rank(merchant.getState(), merchant.getDistrict(), merchant.getTaluka(),
                    merchant.getVillageOrCity(), merchant.getPinCode(),
                    l.getState(), l.getDistrict(), l.getTaluka(), l.getVillageOrCity(), atHome ? f.getPinCode() : null);
            cards.add(new FarmerCard(l.getId(), f.getId(), f.getName(), f.getFarmName(),
                    l.getVillageOrCity() + ", " + l.getDistrict(), CategoryResponse.from(l.getCategory()),
                    l.getVariety(), l.getQuantityKg(), l.getExpectedPricePerKg(), l.getQualityGrade(),
                    l.getImages().stream().map(i -> new ListingResponse.ImageInfo(i.getId(), i.getImagePath())).toList(),
                    s.avgRating(), s.reviewCount(), s.completedDeals(), rank, LocationMatcher.code(rank)));
        }
        Comparator<FarmerCard> order = Comparator.comparingInt(FarmerCard::matchRank);
        order = order.thenComparing(FarmerCard::expectedPricePerKg);            // cheaper first for buyers
        order = order.thenComparingDouble(c -> -(c.avgRating() == null ? 0.0 : c.avgRating()));
        cards.sort(order);
        return cards;
    }

    private static String place(User u) { return u.getVillageOrCity() + ", " + u.getDistrict(); }
}

package com.agroconnect.service;

import com.agroconnect.dto.UserStats;
import com.agroconnect.entity.User;
import com.agroconnect.enums.DealStatus;
import com.agroconnect.enums.Role;
import com.agroconnect.repository.DealRepository;
import com.agroconnect.repository.ReviewRepository;
import com.agroconnect.repository.SupplyRecordRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;

/** Ratings and deal numbers are calculated from stored records every time. Nothing is typed in by hand. */
@Service
public class StatsService {

    private final ReviewRepository reviews;
    private final DealRepository deals;
    private final SupplyRecordRepository supplies;

    public StatsService(ReviewRepository reviews, DealRepository deals, SupplyRecordRepository supplies) {
        this.reviews = reviews;
        this.deals = deals;
        this.supplies = supplies;
    }

    @Transactional(readOnly = true)
    public UserStats forUser(User u) {
        Double avg = reviews.averageVisibleRating(u.getId());
        Double rounded = avg == null ? null : Math.round(avg * 10) / 10.0;
        long count = reviews.countVisible(u.getId());
        if (u.getRole() == Role.MERCHANT) {
            BigDecimal kg = supplies.sumKgForMerchantCompletedDeals(u.getId());
            return new UserStats(rounded, count, deals.countByMerchantIdAndStatus(u.getId(), DealStatus.COMPLETED),
                    kg == null ? BigDecimal.ZERO : kg);
        }
        BigDecimal sold = supplies.sumKgForFarmerCompletedDeals(u.getId());
        return new UserStats(rounded, count, deals.countByFarmerIdAndStatus(u.getId(), DealStatus.COMPLETED),
                sold == null ? BigDecimal.ZERO : sold);
    }
}

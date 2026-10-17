package com.agroconnect.controller;

import com.agroconnect.dto.FarmerCard;
import com.agroconnect.dto.MerchantCard;
import com.agroconnect.service.CurrentUserService;
import com.agroconnect.service.DiscoveryService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/discover")
public class DiscoveryController {

    private final DiscoveryService discovery;
    private final CurrentUserService currentUser;

    public DiscoveryController(DiscoveryService discovery, CurrentUserService currentUser) {
        this.discovery = discovery;
        this.currentUser = currentUser;
    }

    // Farmers only
    @GetMapping("/merchants")
    public List<MerchantCard> merchants(@RequestParam Long categoryId,
                                        @RequestParam(required = false) String location,
                                        @RequestParam(required = false) BigDecimal minQty,
                                        @RequestParam(required = false) BigDecimal minPrice,
                                        @RequestParam(required = false) Double minRating,
                                        Authentication a) {
        return discovery.merchantsForFarmer(currentUser.get(a), categoryId, location, minQty, minPrice, minRating);
    }

    // Merchants only
    @GetMapping("/farmers")
    public List<FarmerCard> farmers(@RequestParam(required = false) Long categoryId,
                                    @RequestParam(required = false) String location,
                                    @RequestParam(required = false) BigDecimal minQty,
                                    @RequestParam(required = false) BigDecimal maxPrice,
                                    @RequestParam(required = false) Double minRating,
                                    Authentication a) {
        return discovery.farmersForMerchant(currentUser.get(a), categoryId, location, minQty, maxPrice, minRating);
    }
}

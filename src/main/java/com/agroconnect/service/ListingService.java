package com.agroconnect.service;

import com.agroconnect.dto.ListingRequest;
import com.agroconnect.dto.ListingResponse;
import com.agroconnect.entity.FruitCategory;
import com.agroconnect.entity.ProduceImage;
import com.agroconnect.entity.ProduceListing;
import com.agroconnect.entity.User;
import com.agroconnect.enums.ListingStatus;
import com.agroconnect.exception.ApiException;
import com.agroconnect.repository.FruitCategoryRepository;
import com.agroconnect.repository.ProduceListingRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

@Service
public class ListingService {

    private static final int MAX_IMAGES = 5;
    private final ProduceListingRepository listings;
    private final FruitCategoryRepository categories;
    private final StorageService storage;

    public ListingService(ProduceListingRepository listings, FruitCategoryRepository categories, StorageService storage) {
        this.listings = listings;
        this.categories = categories;
        this.storage = storage;
    }

    @Transactional(readOnly = true)
    public List<ListingResponse> mine(User farmer) {
        return listings.findByFarmerIdOrderByCreatedAtDesc(farmer.getId()).stream().map(ListingResponse::from).toList();
    }

    @Transactional
    public ListingResponse create(User farmer, ListingRequest r) {
        ProduceListing l = new ProduceListing();
        l.setFarmer(farmer);
        apply(l, r, farmer);
        return ListingResponse.from(listings.save(l));
    }

    @Transactional
    public ListingResponse update(User farmer, Long id, ListingRequest r) {
        ProduceListing l = owned(farmer, id);
        apply(l, r, farmer);
        return ListingResponse.from(listings.save(l));
    }

    @Transactional
    public void delete(User farmer, Long id) {
        ProduceListing l = owned(farmer, id);
        List<String> paths = l.getImages().stream().map(ProduceImage::getImagePath).toList();
        try {
            listings.delete(l);
            listings.flush();
        } catch (DataIntegrityViolationException e) {
            throw new ApiException(HttpStatus.CONFLICT, "LISTING_IN_USE", "This listing is used in a meeting or deal");
        }
        paths.forEach(storage::delete);
    }

    @Transactional
    public ListingResponse addImage(User farmer, Long id, MultipartFile file) {
        ProduceListing l = owned(farmer, id);
        if (l.getImages().size() >= MAX_IMAGES) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "TOO_MANY_IMAGES", "At most 5 photos per listing");
        }
        ProduceImage img = new ProduceImage();
        img.setListing(l);
        img.setImagePath(storage.store(file));
        l.getImages().add(img);
        return ListingResponse.from(listings.save(l));
    }

    @Transactional
    public ListingResponse deleteImage(User farmer, Long id, Long imageId) {
        ProduceListing l = owned(farmer, id);
        ProduceImage img = l.getImages().stream().filter(i -> i.getId().equals(imageId)).findFirst()
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", "Photo not found"));
        l.getImages().remove(img);                    // orphanRemoval deletes the database row
        ListingResponse result = ListingResponse.from(listings.save(l));
        storage.delete(img.getImagePath());
        return result;
    }

    // ---- helpers
    /** Loads a listing and makes sure it belongs to this farmer (nobody can touch another farmer's listing). */
    private ProduceListing owned(User farmer, Long id) {
        ProduceListing l = listings.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", "Listing not found"));
        if (!l.getFarmer().getId().equals(farmer.getId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN", "This is not your listing");
        }
        return l;
    }

    private void apply(ProduceListing l, ListingRequest r, User farmer) {
        FruitCategory c = categories.findById(r.categoryId())
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION", "Unknown fruit category"));
        if (!c.isActive()) throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION", "This fruit category is not active");
        l.setCategory(c);
        l.setVariety(blankToNull(r.variety()));
        l.setQuantityKg(r.quantityKg());
        l.setExpectedPricePerKg(r.expectedPricePerKg());
        l.setQualityGrade(blankToNull(r.qualityGrade()));
        l.setAvailableFrom(r.availableFrom());
        // Location is optional: default to the farmer's registered area
        l.setState(or(r.state(), farmer.getState()));
        l.setDistrict(or(r.district(), farmer.getDistrict()));
        l.setTaluka(or(r.taluka(), farmer.getTaluka()));
        l.setVillageOrCity(or(r.villageOrCity(), farmer.getVillageOrCity()));
        l.setDescription(blankToNull(r.description()));
        l.setStatus(r.status() == null ? ListingStatus.ACTIVE : r.status());
    }

    private static String blankToNull(String s) { return s == null || s.isBlank() ? null : s.trim(); }
    private static String or(String value, String fallback) { return blankToNull(value) != null ? value.trim() : fallback; }
}

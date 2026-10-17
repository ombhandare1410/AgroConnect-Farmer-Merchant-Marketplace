package com.agroconnect.controller;

import com.agroconnect.dto.ListingRequest;
import com.agroconnect.dto.ListingResponse;
import com.agroconnect.service.CurrentUserService;
import com.agroconnect.service.ListingService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

/** Farmer-only (SecurityConfig). The service also checks each listing belongs to the logged-in farmer. */
@RestController
@RequestMapping("/api/listings")
public class ListingController {

    private final ListingService service;
    private final CurrentUserService currentUser;

    public ListingController(ListingService service, CurrentUserService currentUser) {
        this.service = service;
        this.currentUser = currentUser;
    }

    @GetMapping("/mine")
    public List<ListingResponse> mine(Authentication a) { return service.mine(currentUser.get(a)); }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ListingResponse create(@Valid @RequestBody ListingRequest r, Authentication a) {
        return service.create(currentUser.get(a), r);
    }

    @PutMapping("/{id}")
    public ListingResponse update(@PathVariable Long id, @Valid @RequestBody ListingRequest r, Authentication a) {
        return service.update(currentUser.get(a), id, r);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id, Authentication a) { service.delete(currentUser.get(a), id); }

    @PostMapping(value = "/{id}/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ListingResponse addImage(@PathVariable Long id, @RequestParam("file") MultipartFile file, Authentication a) {
        return service.addImage(currentUser.get(a), id, file);
    }

    @DeleteMapping("/{id}/images/{imageId}")
    public ListingResponse deleteImage(@PathVariable Long id, @PathVariable Long imageId, Authentication a) {
        return service.deleteImage(currentUser.get(a), id, imageId);
    }
}

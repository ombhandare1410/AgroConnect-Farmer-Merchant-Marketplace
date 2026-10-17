package com.agroconnect.controller;

import com.agroconnect.dto.PublicProfile;
import com.agroconnect.dto.UpdateProfileRequest;
import com.agroconnect.dto.UserResponse;
import com.agroconnect.service.CurrentUserService;
import com.agroconnect.service.ProfileService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/** Farmers and merchants only (SecurityConfig). */
@RestController
@RequestMapping("/api")
public class ProfileController {

    private final ProfileService service;
    private final CurrentUserService currentUser;

    public ProfileController(ProfileService service, CurrentUserService currentUser) {
        this.service = service;
        this.currentUser = currentUser;
    }

    @GetMapping("/profiles/{id}")
    public PublicProfile view(@PathVariable Long id, Authentication a) {
        return service.view(currentUser.get(a), id);
    }

    @PutMapping("/profile")
    public UserResponse update(@Valid @RequestBody UpdateProfileRequest r, Authentication a) {
        return UserResponse.from(service.update(currentUser.get(a), r));
    }
}

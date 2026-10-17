package com.agroconnect.controller;

import com.agroconnect.dto.RequirementRequest;
import com.agroconnect.dto.RequirementResponse;
import com.agroconnect.service.CurrentUserService;
import com.agroconnect.service.RequirementService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;

/** Merchant-only (SecurityConfig). Pause or close a requirement by updating its status. */
@RestController
@RequestMapping("/api/requirements")
public class RequirementController {

    private final RequirementService service;
    private final CurrentUserService currentUser;

    public RequirementController(RequirementService service, CurrentUserService currentUser) {
        this.service = service;
        this.currentUser = currentUser;
    }

    @GetMapping("/mine")
    public List<RequirementResponse> mine(Authentication a) { return service.mine(currentUser.get(a)); }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RequirementResponse create(@Valid @RequestBody RequirementRequest r, Authentication a) {
        return service.create(currentUser.get(a), r);
    }

    @PutMapping("/{id}")
    public RequirementResponse update(@PathVariable Long id, @Valid @RequestBody RequirementRequest r, Authentication a) {
        return service.update(currentUser.get(a), id, r);
    }
}

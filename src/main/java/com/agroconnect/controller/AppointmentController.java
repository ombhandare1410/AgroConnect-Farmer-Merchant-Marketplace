package com.agroconnect.controller;

import com.agroconnect.dto.AppointmentRequest;
import com.agroconnect.dto.AppointmentResponse;
import com.agroconnect.dto.RespondRequest;
import com.agroconnect.service.AppointmentService;
import com.agroconnect.service.CurrentUserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;

/** Farmers and merchants only (SecurityConfig). Every method also checks the caller is a participant. */
@RestController
@RequestMapping("/api/appointments")
public class AppointmentController {

    private final AppointmentService service;
    private final CurrentUserService currentUser;

    public AppointmentController(AppointmentService service, CurrentUserService currentUser) {
        this.service = service;
        this.currentUser = currentUser;
    }

    @GetMapping
    public List<AppointmentResponse> mine(Authentication a) { return service.mine(currentUser.get(a)); }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AppointmentResponse create(@Valid @RequestBody AppointmentRequest r, Authentication a) {
        return service.create(currentUser.get(a), r);
    }

    @PostMapping("/{id}/respond")
    public AppointmentResponse respond(@PathVariable Long id, @Valid @RequestBody RespondRequest r, Authentication a) {
        return service.respond(currentUser.get(a), id, r);
    }

    @PostMapping("/{id}/cancel")
    public AppointmentResponse cancel(@PathVariable Long id, Authentication a) {
        return service.cancel(currentUser.get(a), id);
    }

    @PostMapping("/{id}/complete")
    public AppointmentResponse complete(@PathVariable Long id, Authentication a) {
        return service.complete(currentUser.get(a), id);
    }
}

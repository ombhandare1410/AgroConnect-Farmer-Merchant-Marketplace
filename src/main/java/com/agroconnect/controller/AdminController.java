package com.agroconnect.controller;

import com.agroconnect.dto.*;
import com.agroconnect.service.AdminService;
import com.agroconnect.service.CurrentUserService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;

/** Administrator only: SecurityConfig protects every /api/admin/** URL with hasRole("ADMIN"). */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminService service;
    private final CurrentUserService currentUser;

    public AdminController(AdminService service, CurrentUserService currentUser) {
        this.service = service;
        this.currentUser = currentUser;
    }

    @GetMapping("/summary")
    public AdminSummary summary() { return service.summary(); }

    @GetMapping("/users")
    public List<AdminUserView> users() { return service.users(); }

    @PutMapping("/users/{id}/active")
    public AdminUserView setActive(@PathVariable Long id, @Valid @RequestBody SetActiveRequest r, Authentication a) {
        return service.setActive(currentUser.get(a), id, r.active());
    }

    @GetMapping("/complaints")
    public List<AdminComplaintView> complaints() { return service.complaints(); }

    @PostMapping("/complaints/{id}/resolve")
    public AdminComplaintView resolve(@PathVariable Long id, @Valid @RequestBody ResolveComplaintRequest r, Authentication a) {
        return service.resolve(currentUser.get(a), id, r);
    }

    @GetMapping("/audit")
    public List<AuditView> audit() { return service.audit(); }
}

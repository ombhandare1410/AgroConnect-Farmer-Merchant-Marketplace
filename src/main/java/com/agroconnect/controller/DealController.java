package com.agroconnect.controller;

import com.agroconnect.dto.*;
import com.agroconnect.service.CurrentUserService;
import com.agroconnect.service.DealService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;

/** Farmers and merchants only (SecurityConfig). DealService also checks the caller is a participant of the deal. */
@RestController
@RequestMapping("/api/deals")
public class DealController {

    private final DealService service;
    private final CurrentUserService currentUser;

    public DealController(DealService service, CurrentUserService currentUser) {
        this.service = service;
        this.currentUser = currentUser;
    }

    @GetMapping
    public List<DealView> mine(Authentication a) { return service.mine(currentUser.get(a)); }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DealView create(@Valid @RequestBody DealRequest r, Authentication a) { return service.create(currentUser.get(a), r); }

    @PostMapping("/{id}/confirm")
    public DealView confirm(@PathVariable Long id, Authentication a) { return service.confirm(currentUser.get(a), id); }

    @PostMapping("/{id}/cancel")
    public DealView cancel(@PathVariable Long id, Authentication a) { return service.cancel(currentUser.get(a), id); }

    // Reject the proposed deal (the other person's decision on a confirmation request)
    @PostMapping("/{id}/reject")
    public DealView reject(@PathVariable Long id, @Valid @RequestBody(required = false) DecisionRequest r, Authentication a) {
        return service.rejectDeal(currentUser.get(a), id, r == null ? null : r.reason());
    }

    // Completion is now a request that the other person must accept (there is no direct "complete" any more)
    @PostMapping("/{id}/completion/request")
    public DealView requestCompletion(@PathVariable Long id, Authentication a) {
        return service.requestCompletion(currentUser.get(a), id);
    }

    @PostMapping("/{id}/completion/accept")
    public DealView acceptCompletion(@PathVariable Long id, Authentication a) {
        return service.acceptCompletion(currentUser.get(a), id);
    }

    @PostMapping("/{id}/completion/reject")
    public DealView rejectCompletion(@PathVariable Long id, @Valid @RequestBody(required = false) DecisionRequest r, Authentication a) {
        return service.rejectCompletion(currentUser.get(a), id, r == null ? null : r.reason());
    }

    @PostMapping("/{id}/supplies")
    public DealView recordSupply(@PathVariable Long id, @Valid @RequestBody SupplyRequest r, Authentication a) {
        return service.recordSupply(currentUser.get(a), id, r);
    }

    @PostMapping("/{id}/supplies/{supplyId}/confirm")
    public DealView confirmSupply(@PathVariable Long id, @PathVariable Long supplyId, Authentication a) {
        return service.respondSupply(currentUser.get(a), id, supplyId, true);
    }

    @PostMapping("/{id}/supplies/{supplyId}/reject")
    public DealView rejectSupply(@PathVariable Long id, @PathVariable Long supplyId, Authentication a) {
        return service.respondSupply(currentUser.get(a), id, supplyId, false);
    }

    @PostMapping("/{id}/payments")
    public DealView recordPayment(@PathVariable Long id, @Valid @RequestBody PaymentRequest r, Authentication a) {
        return service.recordPayment(currentUser.get(a), id, r);
    }

    @PostMapping("/{id}/review")
    public DealView review(@PathVariable Long id, @Valid @RequestBody ReviewRequest r, Authentication a) {
        return service.review(currentUser.get(a), id, r);
    }
}

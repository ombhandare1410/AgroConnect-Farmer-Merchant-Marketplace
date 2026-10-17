package com.agroconnect.controller;

import com.agroconnect.dto.ReportRequest;
import com.agroconnect.service.ComplaintService;
import com.agroconnect.service.CurrentUserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reviews")
public class ReviewController {

    private final ComplaintService complaints;
    private final CurrentUserService currentUser;

    public ReviewController(ComplaintService complaints, CurrentUserService currentUser) {
        this.complaints = complaints;
        this.currentUser = currentUser;
    }

    @PostMapping("/{id}/report")
    @ResponseStatus(HttpStatus.CREATED)
    public void report(@PathVariable Long id, @Valid @RequestBody ReportRequest r, Authentication a) {
        complaints.reportReview(currentUser.get(a), id, r.reason());
    }
}

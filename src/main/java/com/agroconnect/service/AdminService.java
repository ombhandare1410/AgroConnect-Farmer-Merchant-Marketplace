package com.agroconnect.service;

import com.agroconnect.dto.*;
import com.agroconnect.entity.Complaint;
import com.agroconnect.entity.Review;
import com.agroconnect.entity.User;
import com.agroconnect.enums.ComplaintStatus;
import com.agroconnect.enums.DealStatus;
import com.agroconnect.enums.Role;
import com.agroconnect.exception.ApiException;
import com.agroconnect.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Administrator tools. The /api/admin/** URLs are already ADMIN-only (SecurityConfig); the rules below are extra:
 * admin accounts cannot be changed here, closed reports cannot be actioned twice, and every sensitive action
 * is written to the audit log. The admin never edits a deal, supply or payment record.
 */
@Service
public class AdminService {

    private final UserRepository users;
    private final ComplaintRepository complaints;
    private final ReviewRepository reviews;
    private final DealRepository deals;
    private final AuditLogRepository auditLogs;
    private final AuditService audit;

    public AdminService(UserRepository users, ComplaintRepository complaints, ReviewRepository reviews,
                        DealRepository deals, AuditLogRepository auditLogs, AuditService audit) {
        this.users = users;
        this.complaints = complaints;
        this.reviews = reviews;
        this.deals = deals;
        this.auditLogs = auditLogs;
        this.audit = audit;
    }

    @Transactional(readOnly = true)
    public AdminSummary summary() {
        return new AdminSummary(users.countByRole(Role.FARMER), users.countByRole(Role.MERCHANT),
                deals.countByStatus(DealStatus.ACTIVE), deals.countByStatus(DealStatus.COMPLETED),
                complaints.countByStatus(ComplaintStatus.OPEN));
    }

    // ---------------------------------------------------------------- accounts
    @Transactional(readOnly = true)
    public List<AdminUserView> users() {
        return users.findByRoleInOrderByCreatedAtDesc(List.of(Role.FARMER, Role.MERCHANT))
                .stream().map(AdminUserView::from).toList();
    }

    @Transactional
    public AdminUserView setActive(User admin, Long id, boolean active) {
        User u = users.findById(id).orElseThrow(AdminService::notFound);
        if (u.getRole() == Role.ADMIN) {
            throw new ApiException(HttpStatus.FORBIDDEN, "ADMIN_PROTECTED", "Administrator accounts cannot be changed here");
        }
        if (u.isActive() != active) {
            u.setActive(active);
            users.save(u);
            audit.log(admin, active ? "USER_ACTIVATED" : "USER_DEACTIVATED", "USER", id, u.getEmail());
        }
        return AdminUserView.from(u);
    }

    // ---------------------------------------------------------------- reports on reviews
    @Transactional(readOnly = true)
    public List<AdminComplaintView> complaints() {
        return complaints.findAllByOrderByCreatedAtDesc().stream().map(AdminService::view).toList();
    }

    @Transactional
    public AdminComplaintView resolve(User admin, Long id, ResolveComplaintRequest r) {
        Complaint c = complaints.findById(id).orElseThrow(AdminService::notFound);
        Review review = c.getReview();
        String note = r.adminNote();

        switch (r.action()) {
            case HIDE_REVIEW -> {
                requireOpen(c);
                if (review == null) throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION", "This report has no review");
                review.setHidden(true);                 // hidden reviews are excluded from ratings and profiles
                reviews.save(review);
                close(c, ComplaintStatus.RESOLVED, admin, note);
                audit.log(admin, "REVIEW_HIDDEN", "REVIEW", review.getId(), note);
            }
            case RESTORE_REVIEW -> {
                if (review == null || !review.isHidden()) {
                    throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION", "There is no hidden review to restore");
                }
                review.setHidden(false);
                reviews.save(review);
                if (c.getStatus() == ComplaintStatus.OPEN) close(c, ComplaintStatus.DISMISSED, admin, note);
                audit.log(admin, "REVIEW_RESTORED", "REVIEW", review.getId(), note);
            }
            case DISMISS -> {
                requireOpen(c);
                close(c, ComplaintStatus.DISMISSED, admin, note);
                audit.log(admin, "REPORT_DISMISSED", "COMPLAINT", id, note);
            }
            case RESOLVE -> {
                requireOpen(c);
                close(c, ComplaintStatus.RESOLVED, admin, note);
                audit.log(admin, "REPORT_RESOLVED", "COMPLAINT", id, note);
            }
        }
        return view(c);
    }

    // ---------------------------------------------------------------- audit log
    @Transactional(readOnly = true)
    public List<AuditView> audit() {
        return auditLogs.findTop50ByOrderByCreatedAtDesc().stream()
                .map(a -> new AuditView(a.getActor().getName(), a.getAction(), a.getTargetType(), a.getTargetId(),
                        a.getDetails(), a.getCreatedAt())).toList();
    }

    // ---------------------------------------------------------------- helpers
    private void requireOpen(Complaint c) {
        if (c.getStatus() != ComplaintStatus.OPEN) {
            throw new ApiException(HttpStatus.CONFLICT, "COMPLAINT_CLOSED", "This report is already closed");
        }
    }

    private void close(Complaint c, ComplaintStatus status, User admin, String note) {
        c.setStatus(status);
        c.setAdminNote(note == null || note.isBlank() ? null : note.trim());
        c.setResolvedBy(admin);
        c.setResolvedAt(LocalDateTime.now());
        complaints.save(c);
    }

    private static AdminComplaintView view(Complaint c) {
        Review rv = c.getReview();
        return new AdminComplaintView(c.getId(), c.getStatus(), c.getReason(), c.getAdminNote(),
                c.getReporter().getName(), c.getReportedUser() == null ? null : c.getReportedUser().getName(),
                rv == null ? null : rv.getId(), rv == null ? null : rv.getRating(),
                rv == null ? null : rv.getComment(), rv == null ? null : rv.isHidden(),
                c.getCreatedAt(), c.getResolvedAt());
    }

    private static ApiException notFound() { return new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", "Not found"); }
}

package com.agroconnect.service;

import com.agroconnect.dto.ResolveComplaintRequest;
import com.agroconnect.entity.Complaint;
import com.agroconnect.entity.Review;
import com.agroconnect.entity.User;
import com.agroconnect.enums.ComplaintStatus;
import com.agroconnect.enums.Role;
import com.agroconnect.exception.ApiException;
import com.agroconnect.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import java.util.Optional;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AdminServiceTest {

    @Mock UserRepository users;
    @Mock ComplaintRepository complaints;
    @Mock ReviewRepository reviews;
    @Mock DealRepository deals;
    @Mock AuditLogRepository auditLogs;
    @Mock AuditService audit;
    @InjectMocks AdminService service;

    User admin, farmer, otherAdmin, reporter;
    Review review;
    Complaint complaint;

    @BeforeEach
    void setUp() {
        admin = user(1L, Role.ADMIN, "Admin");
        otherAdmin = user(2L, Role.ADMIN, "Admin 2");
        farmer = user(3L, Role.FARMER, "Farmer");
        reporter = user(4L, Role.MERCHANT, "Reporter");

        review = new Review();
        review.setId(50L);
        review.setRating(1);
        review.setComment("rude");
        review.setHidden(false);

        complaint = new Complaint();
        complaint.setId(70L);
        complaint.setReporter(reporter);
        complaint.setReview(review);
        complaint.setReason("abusive");
        complaint.setStatus(ComplaintStatus.OPEN);

        when(users.findById(3L)).thenReturn(Optional.of(farmer));
        when(users.findById(2L)).thenReturn(Optional.of(otherAdmin));
        when(complaints.findById(70L)).thenReturn(Optional.of(complaint));
    }

    @Test
    void administratorAccountsCannotBeDeactivated() {
        assertThatThrownBy(() -> service.setActive(admin, 2L, false))
                .isInstanceOf(ApiException.class).hasFieldOrPropertyWithValue("code", "ADMIN_PROTECTED");
        assertThat(otherAdmin.isActive()).isTrue();
        verify(users, never()).save(any());
    }

    @Test
    void deactivatingAUserIsSavedAndWrittenToTheAuditLog() {
        service.setActive(admin, 3L, false);
        assertThat(farmer.isActive()).isFalse();
        verify(users).save(farmer);
        verify(audit).log(eq(admin), eq("USER_DEACTIVATED"), eq("USER"), eq(3L), any());
    }

    @Test
    void activatingAnAlreadyActiveUserDoesNothing() {
        service.setActive(admin, 3L, true);
        verify(users, never()).save(any());
        verify(audit, never()).log(any(), any(), any(), any(), any());
    }

    @Test
    void hidingAReviewHidesItAndClosesTheReportWithAdminAndTime() {
        service.resolve(admin, 70L, new ResolveComplaintRequest(ResolveComplaintRequest.Action.HIDE_REVIEW, "abusive"));
        assertThat(review.isHidden()).isTrue();
        assertThat(complaint.getStatus()).isEqualTo(ComplaintStatus.RESOLVED);
        assertThat(complaint.getResolvedBy()).isSameAs(admin);
        assertThat(complaint.getResolvedAt()).isNotNull();
        assertThat(complaint.getAdminNote()).isEqualTo("abusive");
        verify(audit).log(eq(admin), eq("REVIEW_HIDDEN"), eq("REVIEW"), eq(50L), any());
    }

    @Test
    void dismissingLeavesTheReviewVisible() {
        service.resolve(admin, 70L, new ResolveComplaintRequest(ResolveComplaintRequest.Action.DISMISS, null));
        assertThat(review.isHidden()).isFalse();
        assertThat(complaint.getStatus()).isEqualTo(ComplaintStatus.DISMISSED);
    }

    @Test
    void aClosedReportCannotBeActionedAgain() {
        complaint.setStatus(ComplaintStatus.RESOLVED);
        assertThatThrownBy(() -> service.resolve(admin, 70L, new ResolveComplaintRequest(ResolveComplaintRequest.Action.HIDE_REVIEW, null)))
                .isInstanceOf(ApiException.class).hasFieldOrPropertyWithValue("code", "COMPLAINT_CLOSED");
    }

    @Test
    void aHiddenReviewCanBeRestored() {
        review.setHidden(true);
        complaint.setStatus(ComplaintStatus.RESOLVED);
        service.resolve(admin, 70L, new ResolveComplaintRequest(ResolveComplaintRequest.Action.RESTORE_REVIEW, "mistake"));
        assertThat(review.isHidden()).isFalse();
        verify(audit).log(eq(admin), eq("REVIEW_RESTORED"), eq("REVIEW"), eq(50L), any());
    }

    @Test
    void restoringAReviewThatIsNotHiddenIsRefused() {
        assertThatThrownBy(() -> service.resolve(admin, 70L, new ResolveComplaintRequest(ResolveComplaintRequest.Action.RESTORE_REVIEW, null)))
                .isInstanceOf(ApiException.class).hasFieldOrPropertyWithValue("code", "VALIDATION");
    }

    private User user(Long id, Role role, String name) {
        User u = new User();
        u.setId(id);
        u.setRole(role);
        u.setName(name);
        u.setEmail(name.toLowerCase().replace(" ", "") + "@example.com");
        return u;
    }
}

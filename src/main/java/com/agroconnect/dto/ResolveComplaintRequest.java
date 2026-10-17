package com.agroconnect.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * HIDE_REVIEW: hide the reported review (it stops counting in ratings) and close the report.
 * DISMISS: the report is not justified. RESOLVE: handled in another way (e.g. user was contacted).
 * RESTORE_REVIEW: undo an earlier hide.
 */
public record ResolveComplaintRequest(@NotNull Action action, @Size(max = 500) String adminNote) {
    public enum Action { HIDE_REVIEW, RESTORE_REVIEW, DISMISS, RESOLVE }
}

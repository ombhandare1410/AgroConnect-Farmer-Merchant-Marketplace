package com.agroconnect.dto;

import com.agroconnect.enums.AppointmentStatus;
import com.agroconnect.enums.Role;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * otherPhone is filled in ONLY when the backend says this viewer may see it.
 * The can... flags tell the page which buttons to show; the backend re-checks every action anyway.
 */
public record AppointmentResponse(Long id, AppointmentStatus status, OtherParty other, String otherPhone,
                                  CategoryResponse category, LocalDate date, LocalTime time,
                                  String meetingLocation, String note, boolean proposedByMe,
                                  boolean canRespond, boolean canCancel, boolean canComplete) {

    public record OtherParty(Long id, Role role, String displayName, String location) { }
}

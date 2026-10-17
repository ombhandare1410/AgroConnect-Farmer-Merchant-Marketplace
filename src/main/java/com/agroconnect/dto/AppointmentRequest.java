package com.agroconnect.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.LocalTime;

/** otherUserId = the person you want to meet. listingId / requirementId are optional "what it is about". */
public record AppointmentRequest(@NotNull Long otherUserId, Long listingId, Long requirementId,
                                 @NotNull LocalDate date, @NotNull LocalTime time,
                                 @NotBlank @Size(max = 200) String meetingLocation,
                                 @Size(max = 300) String note) { }

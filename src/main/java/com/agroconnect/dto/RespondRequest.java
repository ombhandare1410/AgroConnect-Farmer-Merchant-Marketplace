package com.agroconnect.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.LocalTime;

/** ACCEPT, REJECT, or RESCHEDULE (propose another date/time, and optionally another place). */
public record RespondRequest(@NotNull Action action, LocalDate date, LocalTime time,
                             @Size(max = 200) String meetingLocation, @Size(max = 300) String note) {
    public enum Action { ACCEPT, REJECT, RESCHEDULE }
}

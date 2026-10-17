package com.agroconnect.service;

import com.agroconnect.dto.*;
import com.agroconnect.entity.*;
import com.agroconnect.enums.AppointmentStatus;
import com.agroconnect.enums.Role;
import com.agroconnect.exception.ApiException;
import com.agroconnect.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.*;

/**
 * Allowed status changes (everything else is refused with INVALID_TRANSITION):
 *   PENDING / RESCHEDULE_REQUESTED --other person: ACCEPT--> CONFIRMED
 *   PENDING / RESCHEDULE_REQUESTED --other person: REJECT--> REJECTED
 *   PENDING / RESCHEDULE_REQUESTED --other person: RESCHEDULE--> RESCHEDULE_REQUESTED (they become the proposer)
 *   PENDING / RESCHEDULE_REQUESTED / CONFIRMED --either person: cancel--> CANCELLED
 *   CONFIRMED --either person, on or after the meeting date--> COMPLETED
 * requestedBy always means "who made the latest proposal", so the OTHER person is the one who must answer.
 */
@Service
public class AppointmentService {

    private static final ZoneId IST = ZoneId.of("Asia/Kolkata");
    private static final Set<AppointmentStatus> OPEN =
            EnumSet.of(AppointmentStatus.PENDING, AppointmentStatus.RESCHEDULE_REQUESTED);

    private final AppointmentRepository appointments;
    private final UserRepository users;
    private final ProduceListingRepository listings;
    private final MerchantRequirementRepository requirements;
    private final ContactAccessService contact;

    public AppointmentService(AppointmentRepository appointments, UserRepository users,
                              ProduceListingRepository listings, MerchantRequirementRepository requirements,
                              ContactAccessService contact) {
        this.appointments = appointments;
        this.users = users;
        this.listings = listings;
        this.requirements = requirements;
        this.contact = contact;
    }

    @Transactional(readOnly = true)
    public List<AppointmentResponse> mine(User me) {
        Map<Long, Boolean> phoneCache = new HashMap<>();
        return appointments.findForUser(me.getId()).stream().map(a -> toResponse(me, a, phoneCache)).toList();
    }

    @Transactional
    public AppointmentResponse create(User me, AppointmentRequest r) {
        User other = users.findById(r.otherUserId()).filter(User::isActive)
                .orElseThrow(() -> notFound());
        if (other.getRole() == Role.ADMIN || other.getRole() == me.getRole()) throw notFound();
        User farmer = me.getRole() == Role.FARMER ? me : other;
        User merchant = me.getRole() == Role.MERCHANT ? me : other;

        ProduceListing listing = null;
        if (r.listingId() != null) {
            listing = listings.findById(r.listingId()).orElseThrow(() -> validation("Unknown listing"));
            if (!listing.getFarmer().getId().equals(farmer.getId())) throw validation("Listing does not belong to this farmer");
        }
        MerchantRequirement requirement = null;
        if (r.requirementId() != null) {
            requirement = requirements.findById(r.requirementId()).orElseThrow(() -> validation("Unknown requirement"));
            if (!requirement.getMerchant().getId().equals(merchant.getId())) throw validation("Requirement does not belong to this merchant");
        }
        if (appointments.existsByFarmerIdAndMerchantIdAndStatusIn(farmer.getId(), merchant.getId(), OPEN)) {
            throw new ApiException(HttpStatus.CONFLICT, "APPOINTMENT_OPEN", "A meeting request with this person is already open");
        }
        requireFuture(r.date(), r.time());

        Appointment a = new Appointment();
        a.setFarmer(farmer);
        a.setMerchant(merchant);
        a.setListing(listing);
        a.setRequirement(requirement);
        a.setRequestedBy(me);
        a.setAppointmentDate(r.date());
        a.setAppointmentTime(r.time());
        a.setMeetingLocation(r.meetingLocation().trim());
        a.setNote(blankToNull(r.note()));
        a.setStatus(AppointmentStatus.PENDING);
        return toResponse(me, appointments.save(a), new HashMap<>());
    }

    @Transactional
    public AppointmentResponse respond(User me, Long id, RespondRequest r) {
        Appointment a = participantOnly(me, id);
        boolean mineProposal = a.getRequestedBy().getId().equals(me.getId());
        if (!OPEN.contains(a.getStatus()) || mineProposal) throw invalidTransition();   // only the OTHER person answers

        switch (r.action()) {
            case ACCEPT -> {
                requireFuture(a.getAppointmentDate(), a.getAppointmentTime());
                a.setStatus(AppointmentStatus.CONFIRMED);
            }
            case REJECT -> a.setStatus(AppointmentStatus.REJECTED);
            case RESCHEDULE -> {
                if (r.date() == null || r.time() == null) throw validation("Date and time are required");
                requireFuture(r.date(), r.time());
                a.setAppointmentDate(r.date());
                a.setAppointmentTime(r.time());
                if (blankToNull(r.meetingLocation()) != null) a.setMeetingLocation(r.meetingLocation().trim());
                if (blankToNull(r.note()) != null) a.setNote(r.note().trim());
                a.setRequestedBy(me);                                   // now the other person must answer
                a.setStatus(AppointmentStatus.RESCHEDULE_REQUESTED);
            }
        }
        return toResponse(me, appointments.save(a), new HashMap<>());
    }

    @Transactional
    public AppointmentResponse cancel(User me, Long id) {
        Appointment a = participantOnly(me, id);
        if (!OPEN.contains(a.getStatus()) && a.getStatus() != AppointmentStatus.CONFIRMED) throw invalidTransition();
        a.setStatus(AppointmentStatus.CANCELLED);     // phone access from this appointment ends automatically (see ContactAccessService)
        return toResponse(me, appointments.save(a), new HashMap<>());
    }

    @Transactional
    public AppointmentResponse complete(User me, Long id) {
        Appointment a = participantOnly(me, id);
        if (a.getStatus() != AppointmentStatus.CONFIRMED) throw invalidTransition();
        if (a.getAppointmentDate().isAfter(LocalDate.now(IST))) {
            throw new ApiException(HttpStatus.CONFLICT, "TOO_EARLY", "A meeting can be completed only on or after its date");
        }
        a.setStatus(AppointmentStatus.COMPLETED);
        return toResponse(me, appointments.save(a), new HashMap<>());
    }

    // ---------- helpers
    private Appointment participantOnly(User me, Long id) {
        Appointment a = appointments.findById(id).orElseThrow(() -> notFound());
        if (!a.getFarmer().getId().equals(me.getId()) && !a.getMerchant().getId().equals(me.getId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN", "This is not your meeting");
        }
        return a;
    }

    private AppointmentResponse toResponse(User me, Appointment a, Map<Long, Boolean> phoneCache) {
        boolean iAmFarmer = a.getFarmer().getId().equals(me.getId());
        User other = iAmFarmer ? a.getMerchant() : a.getFarmer();
        boolean showPhone = phoneCache.computeIfAbsent(other.getId(), k -> contact.canSeePhone(me, other));
        boolean proposedByMe = a.getRequestedBy().getId().equals(me.getId());
        boolean open = OPEN.contains(a.getStatus());
        CategoryResponse category = a.getListing() != null ? CategoryResponse.from(a.getListing().getCategory())
                : a.getRequirement() != null ? CategoryResponse.from(a.getRequirement().getCategory()) : null;
        String name = other.getRole() == Role.MERCHANT && other.getBusinessName() != null
                ? other.getBusinessName() : other.getName();
        return new AppointmentResponse(a.getId(), a.getStatus(),
                new AppointmentResponse.OtherParty(other.getId(), other.getRole(), name,
                        other.getVillageOrCity() + ", " + other.getDistrict()),
                showPhone ? other.getPhone() : null,
                category, a.getAppointmentDate(), a.getAppointmentTime(), a.getMeetingLocation(), a.getNote(),
                proposedByMe,
                open && !proposedByMe,
                open || a.getStatus() == AppointmentStatus.CONFIRMED,
                a.getStatus() == AppointmentStatus.CONFIRMED && !a.getAppointmentDate().isAfter(LocalDate.now(IST)));
    }

    private void requireFuture(LocalDate date, LocalTime time) {
        if (LocalDateTime.of(date, time).isBefore(LocalDateTime.now(IST))) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "PAST_DATE", "Choose a date and time in the future");
        }
    }

    private static ApiException notFound() { return new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", "Not found"); }
    private static ApiException validation(String m) { return new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION", m); }
    private static ApiException invalidTransition() {
        return new ApiException(HttpStatus.CONFLICT, "INVALID_TRANSITION", "This meeting cannot be changed like that now");
    }
    private static String blankToNull(String s) { return s == null || s.isBlank() ? null : s.trim(); }
}

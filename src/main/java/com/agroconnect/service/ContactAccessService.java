package com.agroconnect.service;

import com.agroconnect.entity.User;
import com.agroconnect.enums.AppointmentStatus;
import com.agroconnect.enums.DealStatus;
import com.agroconnect.enums.Role;
import com.agroconnect.repository.AppointmentRepository;
import com.agroconnect.repository.DealRepository;
import org.springframework.stereotype.Service;
import java.util.List;

/**
 * THE one place that decides who may see whose phone number.
 * Allowed only between a farmer and a merchant, and only while either
 *   - they have a CONFIRMED appointment, or
 *   - they have a deal that is pending confirmation or active.
 * Cancelling an appointment removes only its own permission: another confirmed
 * appointment or an open deal keeps access, otherwise access ends.
 */
@Service
public class ContactAccessService {

    private final AppointmentRepository appointments;
    private final DealRepository deals;

    public ContactAccessService(AppointmentRepository appointments, DealRepository deals) {
        this.appointments = appointments;
        this.deals = deals;
    }

    public boolean canSeePhone(User viewer, User other) {
        boolean pair = (viewer.getRole() == Role.FARMER && other.getRole() == Role.MERCHANT)
                || (viewer.getRole() == Role.MERCHANT && other.getRole() == Role.FARMER);
        if (!pair) return false;
        Long farmerId = viewer.getRole() == Role.FARMER ? viewer.getId() : other.getId();
        Long merchantId = viewer.getRole() == Role.MERCHANT ? viewer.getId() : other.getId();
        return appointments.existsByFarmerIdAndMerchantIdAndStatus(farmerId, merchantId, AppointmentStatus.CONFIRMED)
                || deals.existsByFarmerIdAndMerchantIdAndStatusIn(farmerId, merchantId,
                        List.of(DealStatus.PENDING_CONFIRMATION, DealStatus.ACTIVE));
    }
}

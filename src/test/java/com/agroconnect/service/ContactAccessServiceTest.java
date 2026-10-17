package com.agroconnect.service;

import com.agroconnect.entity.User;
import com.agroconnect.enums.AppointmentStatus;
import com.agroconnect.enums.Role;
import com.agroconnect.repository.AppointmentRepository;
import com.agroconnect.repository.DealRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** The privacy rule for phone numbers: only a farmer/merchant pair with a CONFIRMED meeting or an open deal. */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ContactAccessServiceTest {

    @Mock AppointmentRepository appointments;
    @Mock DealRepository deals;
    ContactAccessService service;
    User farmer, merchant, otherFarmer, admin;

    @BeforeEach
    void setUp() {
        service = new ContactAccessService(appointments, deals);
        farmer = user(1L, Role.FARMER);
        merchant = user(2L, Role.MERCHANT);
        otherFarmer = user(3L, Role.FARMER);
        admin = user(4L, Role.ADMIN);
    }

    @Test
    void hiddenWithoutAnyMeetingOrDeal() {
        assertThat(service.canSeePhone(farmer, merchant)).isFalse();
        assertThat(service.canSeePhone(merchant, farmer)).isFalse();
    }

    @Test
    void visibleToBothSidesWhileAMeetingIsConfirmed() {
        when(appointments.existsByFarmerIdAndMerchantIdAndStatus(1L, 2L, AppointmentStatus.CONFIRMED)).thenReturn(true);
        assertThat(service.canSeePhone(farmer, merchant)).isTrue();
        assertThat(service.canSeePhone(merchant, farmer)).isTrue();
    }

    @Test
    void confirmedMeetingWithSomeoneElseDoesNotUnlockThisPerson() {
        when(appointments.existsByFarmerIdAndMerchantIdAndStatus(1L, 2L, AppointmentStatus.CONFIRMED)).thenReturn(true);
        assertThat(service.canSeePhone(otherFarmer, merchant)).isFalse();
    }

    @Test
    void anOpenDealAloneAlsoAllowsAccess() {
        when(deals.existsByFarmerIdAndMerchantIdAndStatusIn(anyLong(), anyLong(), any())).thenReturn(true);
        assertThat(service.canSeePhone(farmer, merchant)).isTrue();
    }

    @Test
    void neverBetweenTheSameRoleOrWithAnAdmin() {
        when(appointments.existsByFarmerIdAndMerchantIdAndStatus(anyLong(), anyLong(), any())).thenReturn(true);
        when(deals.existsByFarmerIdAndMerchantIdAndStatusIn(anyLong(), anyLong(), any())).thenReturn(true);
        assertThat(service.canSeePhone(farmer, otherFarmer)).isFalse();
        assertThat(service.canSeePhone(farmer, admin)).isFalse();
        assertThat(service.canSeePhone(admin, merchant)).isFalse();
    }

    private User user(Long id, Role role) {
        User u = new User();
        u.setId(id);
        u.setRole(role);
        return u;
    }
}

package com.agroconnect.service;

import com.agroconnect.entity.*;
import com.agroconnect.enums.*;
import com.agroconnect.exception.ApiException;
import com.agroconnect.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests (no database needed) for the two Phase 6 rules:
 *  1) a proposed deal becomes ACTIVE only when the OTHER person accepts it;
 *  2) a deal becomes COMPLETED only when the OTHER person accepts a completion request.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DealApprovalTest {

    @Mock DealRepository deals;
    @Mock UserRepository users;
    @Mock FruitCategoryRepository categories;
    @Mock ProduceListingRepository listings;
    @Mock MerchantRequirementRepository requirementRepo;
    @Mock AppointmentRepository appointments;
    @Mock SupplyRecordRepository supplies;
    @Mock PaymentRecordRepository payments;
    @Mock ReviewRepository reviews;
    @Mock ContactAccessService contact;
    @Mock DealApprovalRepository requests;
    @InjectMocks DealService service;

    User farmer, merchant, stranger;
    FruitCategory grapes;
    Deal deal;

    @BeforeEach
    void setUp() {
        farmer = user(1L, Role.FARMER);
        merchant = user(2L, Role.MERCHANT);
        stranger = user(3L, Role.FARMER);
        grapes = new FruitCategory();
        grapes.setId(5L);
        grapes.setName("Grapes");
        grapes.setActive(true);

        deal = new Deal();
        deal.setId(10L);
        deal.setFarmer(farmer);
        deal.setMerchant(merchant);
        deal.setCategory(grapes);
        deal.setCreatedBy(farmer);                       // the farmer proposed the deal
        deal.setAgreedQuantityKg(new BigDecimal("10000.00"));
        deal.setAgreedPricePerKg(new BigDecimal("40.00"));
        deal.setTotalValue(new BigDecimal("400000.00"));
        deal.setAgreementDate(LocalDate.now());
        deal.setStatus(DealStatus.PENDING_CONFIRMATION);

        when(deals.lockById(10L)).thenReturn(Optional.of(deal));
        when(deals.save(any(Deal.class))).thenAnswer(i -> i.getArgument(0));
        when(requests.save(any(DealApproval.class))).thenAnswer(i -> i.getArgument(0));
    }

    // ---------------------------------------------------------------- 1) deal confirmation
    @Test
    void proposedDealIsStoredAsPendingWithARequestToTheOtherPerson() {
        when(users.findById(2L)).thenReturn(Optional.of(merchant));
        when(appointments.existsByFarmerIdAndMerchantIdAndStatusIn(anyLong(), anyLong(), any())).thenReturn(true);
        when(categories.findById(5L)).thenReturn(Optional.of(grapes));

        service.create(farmer, new com.agroconnect.dto.DealRequest(2L, 5L, null, null,
                new BigDecimal("10000"), new BigDecimal("40"), null, null));

        ArgumentCaptor<Deal> savedDeal = ArgumentCaptor.forClass(Deal.class);
        verify(deals).save(savedDeal.capture());
        assertThat(savedDeal.getValue().getStatus()).isEqualTo(DealStatus.PENDING_CONFIRMATION);

        ArgumentCaptor<DealApproval> req = ArgumentCaptor.forClass(DealApproval.class);
        verify(requests).save(req.capture());
        assertThat(req.getValue().getType()).isEqualTo(DealRequestType.CONFIRMATION);
        assertThat(req.getValue().getStatus()).isEqualTo(DealRequestStatus.PENDING);
        assertThat(req.getValue().getRequestedBy()).isSameAs(farmer);
    }

    @Test
    void requesterCannotAcceptOwnDealProposal() {
        DealApproval r = pending(DealRequestType.CONFIRMATION, farmer);
        assertThatThrownBy(() -> service.confirm(farmer, 10L))
                .isInstanceOf(ApiException.class).hasFieldOrPropertyWithValue("code", "OWN_REQUEST");
        assertThat(deal.getStatus()).isEqualTo(DealStatus.PENDING_CONFIRMATION);
        assertThat(r.getStatus()).isEqualTo(DealRequestStatus.PENDING);
    }

    @Test
    void requesterCannotRejectOwnDealProposalEither() {
        pending(DealRequestType.CONFIRMATION, farmer);
        assertThatThrownBy(() -> service.rejectDeal(farmer, 10L, null))
                .isInstanceOf(ApiException.class).hasFieldOrPropertyWithValue("code", "OWN_REQUEST");
        assertThat(deal.getStatus()).isEqualTo(DealStatus.PENDING_CONFIRMATION);
    }

    @Test
    void otherPersonAcceptsAndDealBecomesActiveWithDecisionStored() {
        DealApproval r = pending(DealRequestType.CONFIRMATION, farmer);
        service.confirm(merchant, 10L);
        assertThat(deal.getStatus()).isEqualTo(DealStatus.ACTIVE);
        assertThat(r.getStatus()).isEqualTo(DealRequestStatus.ACCEPTED);
        assertThat(r.getDecidedBy()).isSameAs(merchant);
        assertThat(r.getDecidedAt()).isNotNull();
    }

    @Test
    void otherPersonRejectsAndDealDoesNotBecomeActive() {
        DealApproval r = pending(DealRequestType.CONFIRMATION, farmer);
        service.rejectDeal(merchant, 10L, "Price too low");
        assertThat(deal.getStatus()).isEqualTo(DealStatus.CANCELLED);
        assertThat(r.getStatus()).isEqualTo(DealRequestStatus.REJECTED);
        assertThat(r.getReason()).isEqualTo("Price too low");
        assertThat(r.getDecidedBy()).isSameAs(merchant);
    }

    @Test
    void phase5DealWithoutStoredRequestStillWorks() {      // old data: PENDING deal, no deal_requests row
        service.confirm(merchant, 10L);
        assertThat(deal.getStatus()).isEqualTo(DealStatus.ACTIVE);
    }

    @Test
    void phase5PendingDealCannotBeAcceptedByItsProposerEvenWithoutStoredRequest() {
        assertThatThrownBy(() -> service.confirm(farmer, 10L))
                .isInstanceOf(ApiException.class).hasFieldOrPropertyWithValue("code", "OWN_REQUEST");
        assertThat(deal.getStatus()).isEqualTo(DealStatus.PENDING_CONFIRMATION);
    }

    @Test
    void nonParticipantCannotTouchTheDeal() {
        assertThatThrownBy(() -> service.confirm(stranger, 10L))
                .isInstanceOf(ApiException.class).hasFieldOrPropertyWithValue("code", "FORBIDDEN");
        assertThatThrownBy(() -> service.requestCompletion(stranger, 10L))
                .isInstanceOf(ApiException.class).hasFieldOrPropertyWithValue("code", "FORBIDDEN");
    }

    @Test
    void acceptedDealCannotBeAcceptedTwice() {
        deal.setStatus(DealStatus.ACTIVE);
        assertThatThrownBy(() -> service.confirm(merchant, 10L))
                .isInstanceOf(ApiException.class).hasFieldOrPropertyWithValue("code", "INVALID_TRANSITION");
    }

    // ---------------------------------------------------------------- 2) deal completion
    @Test
    void completionRequestDoesNotCompleteTheDeal() {
        activeDealWithConfirmedDelivery();
        service.requestCompletion(farmer, 10L);

        assertThat(deal.getStatus()).isEqualTo(DealStatus.ACTIVE);          // NOT completed
        ArgumentCaptor<DealApproval> req = ArgumentCaptor.forClass(DealApproval.class);
        verify(requests).save(req.capture());
        assertThat(req.getValue().getType()).isEqualTo(DealRequestType.COMPLETION);
        assertThat(req.getValue().getStatus()).isEqualTo(DealRequestStatus.PENDING);
        assertThat(req.getValue().getRequestedBy()).isSameAs(farmer);
        assertThat(req.getValue().getRequestedAt()).isNotNull();
    }

    @Test
    void completionNeedsAtLeastOneConfirmedDelivery() {
        deal.setStatus(DealStatus.ACTIVE);
        assertThatThrownBy(() -> service.requestCompletion(farmer, 10L))
                .isInstanceOf(ApiException.class).hasFieldOrPropertyWithValue("code", "NO_SUPPLY");
    }

    @Test
    void completionCannotBeRequestedForADealThatIsNotActive() {
        assertThatThrownBy(() -> service.requestCompletion(farmer, 10L))      // still PENDING_CONFIRMATION
                .isInstanceOf(ApiException.class).hasFieldOrPropertyWithValue("code", "INVALID_TRANSITION");
    }

    @Test
    void secondCompletionRequestWhileOneIsWaitingIsRefused() {
        activeDealWithConfirmedDelivery();
        pending(DealRequestType.COMPLETION, farmer);
        assertThatThrownBy(() -> service.requestCompletion(merchant, 10L))
                .isInstanceOf(ApiException.class).hasFieldOrPropertyWithValue("code", "COMPLETION_PENDING");
    }

    @Test
    void requesterCannotAcceptOrRejectOwnCompletionRequest() {
        activeDealWithConfirmedDelivery();
        DealApproval r = pending(DealRequestType.COMPLETION, farmer);
        assertThatThrownBy(() -> service.acceptCompletion(farmer, 10L))
                .isInstanceOf(ApiException.class).hasFieldOrPropertyWithValue("code", "OWN_REQUEST");
        assertThatThrownBy(() -> service.rejectCompletion(farmer, 10L, null))
                .isInstanceOf(ApiException.class).hasFieldOrPropertyWithValue("code", "OWN_REQUEST");
        assertThat(deal.getStatus()).isEqualTo(DealStatus.ACTIVE);
        assertThat(r.getStatus()).isEqualTo(DealRequestStatus.PENDING);
    }

    @Test
    void otherPersonAcceptsAndDealBecomesCompleted() {
        activeDealWithConfirmedDelivery();
        DealApproval r = pending(DealRequestType.COMPLETION, farmer);
        service.acceptCompletion(merchant, 10L);
        assertThat(deal.getStatus()).isEqualTo(DealStatus.COMPLETED);
        assertThat(r.getStatus()).isEqualTo(DealRequestStatus.ACCEPTED);
        assertThat(r.getDecidedBy()).isSameAs(merchant);
        assertThat(r.getDecidedAt()).isNotNull();
    }

    @Test
    void rejectedCompletionLeavesTheDealActive() {
        activeDealWithConfirmedDelivery();
        DealApproval r = pending(DealRequestType.COMPLETION, farmer);
        service.rejectCompletion(merchant, 10L, "Payment still pending");
        assertThat(deal.getStatus()).isEqualTo(DealStatus.ACTIVE);
        assertThat(r.getStatus()).isEqualTo(DealRequestStatus.REJECTED);
        assertThat(r.getReason()).isEqualTo("Payment still pending");
    }

    @Test
    void acceptingWithoutAWaitingRequestIsRefused() {
        activeDealWithConfirmedDelivery();
        assertThatThrownBy(() -> service.acceptCompletion(merchant, 10L))
                .isInstanceOf(ApiException.class).hasFieldOrPropertyWithValue("code", "REQUEST_NOT_PENDING");
        assertThat(deal.getStatus()).isEqualTo(DealStatus.ACTIVE);
    }

    // ---------------------------------------------------------------- helpers
    private User user(Long id, Role role) {
        User u = new User();
        u.setId(id);
        u.setRole(role);
        u.setName("User " + id);
        return u;
    }

    /** Makes the repository return a PENDING request of this type, sent by 'requester'. */
    private DealApproval pending(DealRequestType type, User requester) {
        DealApproval r = new DealApproval();
        r.setDeal(deal);
        r.setType(type);
        r.setStatus(DealRequestStatus.PENDING);
        r.setRequestedBy(requester);
        when(requests.findFirstByDealIdAndTypeAndStatus(10L, type, DealRequestStatus.PENDING)).thenReturn(Optional.of(r));
        return r;
    }

    private void activeDealWithConfirmedDelivery() {
        deal.setStatus(DealStatus.ACTIVE);
        SupplyRecord s = new SupplyRecord();
        s.setDeal(deal);
        s.setQuantityKg(new BigDecimal("1500.00"));
        s.setStatus(SupplyStatus.CONFIRMED);
        s.setRecordedBy(farmer);
        when(supplies.findByDealIdOrderByDeliveryDateAscCreatedAtAsc(10L)).thenReturn(List.of(s));
    }
}

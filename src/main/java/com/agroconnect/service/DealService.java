package com.agroconnect.service;

import com.agroconnect.dto.*;
import com.agroconnect.entity.*;
import com.agroconnect.enums.*;
import com.agroconnect.exception.ApiException;
import com.agroconnect.repository.*;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

/**
 * Deals, deliveries and payments. Money rules (all calculated HERE, never trusted from the browser):
 *   total value   = agreed kg x agreed price per kg
 *   supplied kg   = sum of CONFIRMED deliveries          remaining kg   = agreed kg - supplied kg
 *   payments      = sum of payment records               outstanding    = total value - payments
 * Remaining quantity and outstanding payment are separate: a buyer may pay before or after delivery.
 * Every method that changes a deal first LOCKS the deal row, so simultaneous requests cannot break the totals.
 */
@Service
public class DealService {

    private static final ZoneId IST = ZoneId.of("Asia/Kolkata");
    private static final BigDecimal MAX_TOTAL = new BigDecimal("999999999999.99");

    private final DealRepository deals;
    private final UserRepository users;
    private final FruitCategoryRepository categories;
    private final ProduceListingRepository listings;
    private final MerchantRequirementRepository requirements;
    private final AppointmentRepository appointments;
    private final SupplyRecordRepository supplies;
    private final PaymentRecordRepository payments;
    private final ReviewRepository reviews;
    private final ContactAccessService contact;
    private final DealApprovalRepository requests;

    public DealService(DealRepository deals, UserRepository users, FruitCategoryRepository categories,
                       ProduceListingRepository listings, MerchantRequirementRepository requirements,
                       AppointmentRepository appointments, SupplyRecordRepository supplies,
                       PaymentRecordRepository payments, ReviewRepository reviews, ContactAccessService contact,
                       DealApprovalRepository requests) {
        this.deals = deals;
        this.users = users;
        this.categories = categories;
        this.listings = listings;
        this.requirements = requirements;
        this.appointments = appointments;
        this.supplies = supplies;
        this.payments = payments;
        this.reviews = reviews;
        this.contact = contact;
        this.requests = requests;
    }

    @Transactional(readOnly = true)
    public List<DealView> mine(User me) {
        Map<Long, Boolean> phoneCache = new HashMap<>();
        return deals.findForUser(me.getId()).stream().map(d -> view(me, d, phoneCache)).toList();
    }

    // ---------------------------------------------------------------- deal life cycle
    @Transactional
    public DealView create(User me, DealRequest r) {
        User other = users.findById(r.otherUserId()).filter(User::isActive).orElseThrow(DealService::notFound);
        if (other.getRole() == Role.ADMIN || other.getRole() == me.getRole()) throw notFound();
        User farmer = me.getRole() == Role.FARMER ? me : other;
        User merchant = me.getRole() == Role.MERCHANT ? me : other;

        // A deal also unlocks phone numbers, so it needs a confirmed (or completed) meeting first.
        if (!appointments.existsByFarmerIdAndMerchantIdAndStatusIn(farmer.getId(), merchant.getId(),
                EnumSet.of(AppointmentStatus.CONFIRMED, AppointmentStatus.COMPLETED))) {
            throw new ApiException(HttpStatus.CONFLICT, "MEETING_REQUIRED", "A confirmed meeting is needed first");
        }
        FruitCategory category = categories.findById(r.categoryId()).filter(FruitCategory::isActive)
                .orElseThrow(() -> validation("Unknown or inactive fruit category"));
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
        BigDecimal qty = money(r.agreedQuantityKg());
        BigDecimal price = money(r.agreedPricePerKg());
        requirePositive(qty);
        requirePositive(price);
        BigDecimal total = qty.multiply(price).setScale(2, RoundingMode.HALF_UP);      // calculated on the server
        if (total.compareTo(MAX_TOTAL) > 0) throw validation("Deal value is too large");
        LocalDate date = r.agreementDate() == null ? today() : r.agreementDate();
        notFuture(date);

        Deal d = new Deal();
        d.setFarmer(farmer);
        d.setMerchant(merchant);
        d.setCategory(category);
        d.setListing(listing);
        d.setRequirement(requirement);
        d.setCreatedBy(me);
        d.setAgreedQuantityKg(qty);
        d.setAgreedPricePerKg(price);
        d.setTotalValue(total);
        d.setAgreementDate(date);
        d.setTerms(blankToNull(r.terms()));
        d.setStatus(DealStatus.PENDING_CONFIRMATION);
        Deal saved = deals.save(d);
        openRequest(saved, DealRequestType.CONFIRMATION, me);     // the OTHER person must accept or reject it
        return view(me, saved, new HashMap<>());
    }

    /** The OTHER person (never the one who proposed) ACCEPTS the deal: PENDING_CONFIRMATION -> ACTIVE. */
    @Transactional
    public DealView confirm(User me, Long id) {
        Deal d = locked(me, id);
        if (d.getStatus() != DealStatus.PENDING_CONFIRMATION) throw invalid();
        DealApproval req = pendingRequest(d, DealRequestType.CONFIRMATION);
        requireNotRequester(req, me);
        decide(req, DealRequestStatus.ACCEPTED, me, null);
        d.setStatus(DealStatus.ACTIVE);
        return view(me, deals.save(d), new HashMap<>());
    }

    /** The OTHER person REJECTS the proposed deal: PENDING_CONFIRMATION -> CANCELLED. The decision is stored. */
    @Transactional
    public DealView rejectDeal(User me, Long id, String reason) {
        Deal d = locked(me, id);
        if (d.getStatus() != DealStatus.PENDING_CONFIRMATION) throw invalid();
        DealApproval req = pendingRequest(d, DealRequestType.CONFIRMATION);
        requireNotRequester(req, me);
        decide(req, DealRequestStatus.REJECTED, me, reason);
        d.setStatus(DealStatus.CANCELLED);
        return view(me, deals.save(d), new HashMap<>());
    }

    /** Either person may cancel a pending deal, or an active deal that has no deliveries and no payments yet. */
    @Transactional
    public DealView cancel(User me, Long id) {
        Deal d = locked(me, id);
        boolean untouched = supplies.findByDealIdOrderByDeliveryDateAscCreatedAtAsc(id).isEmpty()
                && payments.findByDealIdOrderByPaymentDateAscCreatedAtAsc(id).isEmpty();
        boolean wasPending = d.getStatus() == DealStatus.PENDING_CONFIRMATION;
        if (!(wasPending || (d.getStatus() == DealStatus.ACTIVE && untouched))) throw invalid();
        d.setStatus(DealStatus.CANCELLED);
        if (wasPending) {          // close the open request: the sender withdraws it, the receiver rejects it
            requests.findFirstByDealIdAndTypeAndStatus(id, DealRequestType.CONFIRMATION, DealRequestStatus.PENDING)
                    .ifPresent(r -> decide(r, r.getRequestedBy().getId().equals(me.getId())
                            ? DealRequestStatus.WITHDRAWN : DealRequestStatus.REJECTED, me, null));
        }
        return view(me, deals.save(d), new HashMap<>());
    }

    /**
     * Either person may ASK for the deal to be completed. This does NOT complete the deal: it stores a PENDING
     * completion request, and the deal stays ACTIVE until the other person accepts. Needs one CONFIRMED delivery.
     */
    @Transactional
    public DealView requestCompletion(User me, Long id) {
        Deal d = locked(me, id);
        if (d.getStatus() != DealStatus.ACTIVE) throw invalid();
        requireConfirmedSupply(id);
        if (requests.findFirstByDealIdAndTypeAndStatus(id, DealRequestType.COMPLETION, DealRequestStatus.PENDING).isPresent()) {
            throw new ApiException(HttpStatus.CONFLICT, "COMPLETION_PENDING", "A completion request is already waiting");
        }
        openRequest(d, DealRequestType.COMPLETION, me);
        return view(me, d, new HashMap<>());
    }

    /** Only the OTHER person can accept. ACTIVE -> COMPLETED. Completion unlocks reviews. */
    @Transactional
    public DealView acceptCompletion(User me, Long id) {
        Deal d = locked(me, id);
        if (d.getStatus() != DealStatus.ACTIVE) throw invalid();
        DealApproval req = pendingRequest(d, DealRequestType.COMPLETION);
        requireNotRequester(req, me);
        requireConfirmedSupply(id);
        decide(req, DealRequestStatus.ACCEPTED, me, null);
        d.setStatus(DealStatus.COMPLETED);
        return view(me, deals.save(d), new HashMap<>());
    }

    /** Rejecting leaves the deal ACTIVE; the parties can sort out what is missing and ask again. */
    @Transactional
    public DealView rejectCompletion(User me, Long id, String reason) {
        Deal d = locked(me, id);
        if (d.getStatus() != DealStatus.ACTIVE) throw invalid();
        DealApproval req = pendingRequest(d, DealRequestType.COMPLETION);
        requireNotRequester(req, me);
        decide(req, DealRequestStatus.REJECTED, me, reason);
        return view(me, d, new HashMap<>());
    }

    // ---------------------------------------------------------------- deliveries
    @Transactional
    public DealView recordSupply(User me, Long id, SupplyRequest r) {
        Deal d = locked(me, id);
        if (d.getStatus() != DealStatus.ACTIVE) throw invalid();
        notFuture(r.deliveryDate());
        BigDecimal qty = money(r.quantityKg());
        requirePositive(qty);
        List<SupplyRecord> existing = supplies.findByDealIdOrderByDeliveryDateAscCreatedAtAsc(id);
        BigDecimal counted = sum(existing, SupplyStatus.CONFIRMED).add(sum(existing, SupplyStatus.RECORDED));
        if (counted.add(qty).compareTo(d.getAgreedQuantityKg()) > 0) {
            throw new ApiException(HttpStatus.CONFLICT, "SUPPLY_EXCEEDS", "More than the agreed quantity");
        }
        SupplyRecord s = new SupplyRecord();
        s.setDeal(d);
        s.setDeliveryDate(r.deliveryDate());
        s.setQuantityKg(qty);
        s.setNotes(blankToNull(r.notes()));
        s.setStatus(SupplyStatus.RECORDED);
        s.setRecordedBy(me);
        supplies.save(s);
        return view(me, d, new HashMap<>());
    }

    /** The person who did NOT record a delivery confirms or rejects it. Rejected rows stay in the history. */
    @Transactional
    public DealView respondSupply(User me, Long dealId, Long supplyId, boolean confirm) {
        Deal d = locked(me, dealId);
        SupplyRecord s = supplies.findById(supplyId).orElseThrow(DealService::notFound);
        if (!s.getDeal().getId().equals(dealId)) throw notFound();
        if (d.getStatus() != DealStatus.ACTIVE || s.getStatus() != SupplyStatus.RECORDED
                || s.getRecordedBy().getId().equals(me.getId())) throw invalid();
        s.setStatus(confirm ? SupplyStatus.CONFIRMED : SupplyStatus.REJECTED);
        supplies.save(s);
        return view(me, d, new HashMap<>());
    }

    // ---------------------------------------------------------------- payments
    /** A payment record is NOT proof that money moved, so it is stored as RECORDED. Nothing here sets VERIFIED. */
    @Transactional
    public DealView recordPayment(User me, Long id, PaymentRequest r) {
        Deal d = locked(me, id);
        if (d.getStatus() != DealStatus.ACTIVE && d.getStatus() != DealStatus.COMPLETED) throw invalid();
        notFuture(r.paymentDate());
        BigDecimal amount = money(r.amount());
        requirePositive(amount);
        BigDecimal paid = paidTotal(payments.findByDealIdOrderByPaymentDateAscCreatedAtAsc(id));
        if (paid.add(amount).compareTo(d.getTotalValue()) > 0) {
            throw new ApiException(HttpStatus.CONFLICT, "PAYMENT_EXCEEDS", "More than the outstanding amount");
        }
        PaymentRecord p = new PaymentRecord();
        p.setDeal(d);
        p.setPaymentDate(r.paymentDate());
        p.setAmount(amount);
        p.setMethod(r.method());
        p.setReferenceNo(blankToNull(r.referenceNo()));
        p.setNotes(blankToNull(r.notes()));
        p.setStatus(PaymentStatus.RECORDED);
        p.setRecordedBy(me);
        payments.save(p);
        return view(me, d, new HashMap<>());
    }

    // ---------------------------------------------------------------- reviews
    @Transactional
    public DealView review(User me, Long id, ReviewRequest r) {
        Deal d = locked(me, id);
        if (d.getStatus() != DealStatus.COMPLETED) throw invalid();
        if (reviews.existsByDealIdAndReviewerId(id, me.getId())) {
            throw new ApiException(HttpStatus.CONFLICT, "REVIEW_EXISTS", "You already reviewed this deal");
        }
        Review rv = new Review();
        rv.setDeal(d);
        rv.setReviewer(me);
        rv.setReviewee(d.getFarmer().getId().equals(me.getId()) ? d.getMerchant() : d.getFarmer());
        rv.setRating(r.rating());
        rv.setComment(blankToNull(r.comment()));
        try {
            reviews.saveAndFlush(rv);           // the database also has a unique key (deal, reviewer)
        } catch (DataIntegrityViolationException e) {
            throw new ApiException(HttpStatus.CONFLICT, "REVIEW_EXISTS", "You already reviewed this deal");
        }
        return view(me, d, new HashMap<>());
    }

    // ---------------------------------------------------------------- building the response
    private DealView view(User me, Deal d, Map<Long, Boolean> phoneCache) {
        List<SupplyRecord> sups = supplies.findByDealIdOrderByDeliveryDateAscCreatedAtAsc(d.getId());
        List<PaymentRecord> pays = payments.findByDealIdOrderByPaymentDateAscCreatedAtAsc(d.getId());

        BigDecimal confirmed = sum(sups, SupplyStatus.CONFIRMED);
        BigDecimal pending = sum(sups, SupplyStatus.RECORDED);
        BigDecimal remaining = d.getAgreedQuantityKg().subtract(confirmed).max(BigDecimal.ZERO);
        BigDecimal valueSupplied = confirmed.multiply(d.getAgreedPricePerKg()).setScale(2, RoundingMode.HALF_UP);
        BigDecimal paid = paidTotal(pays);
        BigDecimal outstanding = d.getTotalValue().subtract(paid).max(BigDecimal.ZERO);

        boolean iAmFarmer = d.getFarmer().getId().equals(me.getId());
        User other = iAmFarmer ? d.getMerchant() : d.getFarmer();
        boolean showPhone = phoneCache.computeIfAbsent(other.getId(), k -> contact.canSeePhone(me, other));
        boolean createdByMe = d.getCreatedBy().getId().equals(me.getId());
        DealStatus st = d.getStatus();
        boolean untouched = sups.isEmpty() && pays.isEmpty();
        boolean reviewed = reviews.existsByDealIdAndReviewerId(d.getId(), me.getId());
        List<DealApproval> reqs = requests.findByDealIdOrderByRequestedAtAsc(d.getId());
        boolean completionPending = reqs.stream().anyMatch(r -> r.getType() == DealRequestType.COMPLETION
                && r.getStatus() == DealRequestStatus.PENDING);
        boolean canDecideCompletion = st == DealStatus.ACTIVE && reqs.stream().anyMatch(r ->
                r.getType() == DealRequestType.COMPLETION && r.getStatus() == DealRequestStatus.PENDING
                        && !r.getRequestedBy().getId().equals(me.getId()));

        DealView.Flags flags = new DealView.Flags(
                st == DealStatus.PENDING_CONFIRMATION && !createdByMe,
                st == DealStatus.PENDING_CONFIRMATION || (st == DealStatus.ACTIVE && untouched),
                st == DealStatus.ACTIVE && confirmed.signum() > 0 && !completionPending,
                st == DealStatus.ACTIVE && d.getAgreedQuantityKg().subtract(confirmed).subtract(pending).signum() > 0,
                (st == DealStatus.ACTIVE || st == DealStatus.COMPLETED) && outstanding.signum() > 0,
                st == DealStatus.COMPLETED && !reviewed,
                reviewed, canDecideCompletion, completionPending);
        List<DealView.RequestInfo> requestInfos = reqs.stream().map(r -> new DealView.RequestInfo(r.getId(), r.getType(),
                r.getStatus(), displayName(r.getRequestedBy()), r.getRequestedBy().getId().equals(me.getId()),
                r.getRequestedAt(), r.getDecidedBy() == null ? null : displayName(r.getDecidedBy()), r.getDecidedAt(),
                r.getReason())).toList();

        List<DealView.SupplyView> supplyViews = sups.stream().map(s -> {
            boolean byMe = s.getRecordedBy().getId().equals(me.getId());
            return new DealView.SupplyView(s.getId(), s.getDeliveryDate(), s.getQuantityKg(), s.getNotes(), s.getStatus(),
                    byMe, st == DealStatus.ACTIVE && s.getStatus() == SupplyStatus.RECORDED && !byMe, s.getCreatedAt());
        }).toList();
        List<DealView.PaymentView> paymentViews = pays.stream().map(p -> new DealView.PaymentView(p.getId(),
                p.getPaymentDate(), p.getAmount(), p.getMethod(), p.getReferenceNo(), p.getNotes(), p.getStatus(),
                p.getRecordedBy().getId().equals(me.getId()), p.getCreatedAt())).toList();

        String otherName = other.getRole() == Role.MERCHANT && other.getBusinessName() != null
                ? other.getBusinessName() : other.getName();
        return new DealView(d.getId(), st,
                new AppointmentResponse.OtherParty(other.getId(), other.getRole(), otherName,
                        other.getVillageOrCity() + ", " + other.getDistrict()),
                showPhone ? other.getPhone() : null, CategoryResponse.from(d.getCategory()),
                d.getAgreedQuantityKg(), d.getAgreedPricePerKg(), d.getTotalValue(), d.getAgreementDate(), d.getTerms(),
                createdByMe, new DealView.Summary(confirmed, pending, remaining, valueSupplied, paid, outstanding),
                supplyViews, paymentViews, flags, requestInfos);
    }

    // ---------------------------------------------------------------- helpers
    /** Locks the deal and checks the caller is one of its two participants. */
    private Deal locked(User me, Long id) {
        Deal d = deals.lockById(id).orElseThrow(DealService::notFound);
        if (!d.getFarmer().getId().equals(me.getId()) && !d.getMerchant().getId().equals(me.getId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN", "This is not your deal");
        }
        return d;
    }

    /** The pending request of this type. A deal proposed in Phase 5 has no stored request yet, so it is rebuilt from the deal. */
    private DealApproval pendingRequest(Deal d, DealRequestType type) {
        return requests.findFirstByDealIdAndTypeAndStatus(d.getId(), type, DealRequestStatus.PENDING).orElseGet(() -> {
            if (type == DealRequestType.CONFIRMATION && d.getStatus() == DealStatus.PENDING_CONFIRMATION) {
                DealApproval legacy = newRequest(d, type, d.getCreatedBy());
                legacy.setRequestedAt(d.getCreatedAt());
                return requests.save(legacy);
            }
            throw new ApiException(HttpStatus.CONFLICT, "REQUEST_NOT_PENDING", "There is no waiting request");
        });
    }

    private DealApproval newRequest(Deal d, DealRequestType type, User requester) {
        DealApproval r = new DealApproval();
        r.setDeal(d);
        r.setType(type);
        r.setStatus(DealRequestStatus.PENDING);
        r.setRequestedBy(requester);
        r.setRequestedAt(LocalDateTime.now());
        return r;
    }

    private DealApproval openRequest(Deal d, DealRequestType type, User requester) {
        return requests.save(newRequest(d, type, requester));
    }

    /** Nobody can approve their own request. */
    private static void requireNotRequester(DealApproval req, User me) {
        if (req.getRequestedBy().getId().equals(me.getId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "OWN_REQUEST", "You cannot approve your own request");
        }
    }

    private void decide(DealApproval req, DealRequestStatus status, User me, String reason) {
        req.setStatus(status);
        req.setDecidedBy(me);
        req.setDecidedAt(LocalDateTime.now());
        req.setReason(blankToNull(reason));
        requests.save(req);
    }

    private void requireConfirmedSupply(Long dealId) {
        if (sum(supplies.findByDealIdOrderByDeliveryDateAscCreatedAtAsc(dealId), SupplyStatus.CONFIRMED).signum() <= 0) {
            throw new ApiException(HttpStatus.CONFLICT, "NO_SUPPLY", "Record and confirm at least one delivery first");
        }
    }

    private static String displayName(User u) {
        return u.getRole() == Role.MERCHANT && u.getBusinessName() != null ? u.getBusinessName() : u.getName();
    }

    private static BigDecimal sum(List<SupplyRecord> list, SupplyStatus status) {
        return list.stream().filter(s -> s.getStatus() == status).map(SupplyRecord::getQuantityKg)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
    private static BigDecimal paidTotal(List<PaymentRecord> list) {
        return list.stream().map(PaymentRecord::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
    private static BigDecimal money(BigDecimal v) { return v.setScale(2, RoundingMode.HALF_UP); }
    private static void requirePositive(BigDecimal v) { if (v.signum() <= 0) throw validation("Value must be greater than zero"); }
    private static LocalDate today() { return LocalDate.now(IST); }
    private static void notFuture(LocalDate date) {
        if (date.isAfter(today())) throw new ApiException(HttpStatus.BAD_REQUEST, "FUTURE_DATE", "The date cannot be in the future");
    }
    private static ApiException notFound() { return new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", "Not found"); }
    private static ApiException validation(String m) { return new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION", m); }
    private static ApiException invalid() { return new ApiException(HttpStatus.CONFLICT, "INVALID_TRANSITION", "This cannot be done now"); }
    private static String blankToNull(String s) { return s == null || s.isBlank() ? null : s.trim(); }
}

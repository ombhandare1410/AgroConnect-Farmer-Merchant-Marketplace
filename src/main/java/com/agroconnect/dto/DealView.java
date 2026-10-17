package com.agroconnect.dto;

import com.agroconnect.enums.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Everything about one deal, as seen by ONE participant. Both people see the same numbers;
 * only the flags (what buttons to show) and "...ByMe" fields differ.
 */
public record DealView(Long id, DealStatus status, AppointmentResponse.OtherParty other, String otherPhone,
                       CategoryResponse category, BigDecimal agreedQuantityKg, BigDecimal agreedPricePerKg,
                       BigDecimal totalValue, LocalDate agreementDate, String terms, boolean createdByMe,
                       Summary summary, List<SupplyView> supplies, List<PaymentView> payments, Flags flags,
                       List<RequestInfo> requests) {

    /** All calculated by the backend from stored records. Quantity (kg) and money (INR) are kept separate. */
    public record Summary(BigDecimal suppliedKg, BigDecimal pendingKg, BigDecimal remainingKg,
                          BigDecimal valueSupplied, BigDecimal paymentsTotal, BigDecimal outstanding) { }

    public record SupplyView(Long id, LocalDate deliveryDate, BigDecimal quantityKg, String notes,
                             SupplyStatus status, boolean recordedByMe, boolean canRespond, LocalDateTime createdAt) { }

    public record PaymentView(Long id, LocalDate paymentDate, BigDecimal amount, PaymentMethod method,
                              String referenceNo, String notes, PaymentStatus status, boolean recordedByMe,
                              LocalDateTime createdAt) { }

    /**
     * canConfirm = this person may ACCEPT or REJECT the proposed deal.
     * canComplete = this person may SEND a completion request (it no longer completes the deal by itself).
     * canDecideCompletion = a completion request from the other person is waiting for this person.
     */
    public record Flags(boolean canConfirm, boolean canCancel, boolean canComplete, boolean canRecordSupply,
                        boolean canRecordPayment, boolean canReview, boolean reviewed,
                        boolean canDecideCompletion, boolean completionPending) { }

    /** History of confirmation / completion requests: who asked, who decided, what, and when. */
    public record RequestInfo(Long id, DealRequestType type, DealRequestStatus status, String requestedByName,
                              boolean requestedByMe, LocalDateTime requestedAt, String decidedByName,
                              LocalDateTime decidedAt, String reason) { }
}

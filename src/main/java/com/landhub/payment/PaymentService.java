package com.landhub.payment;

import com.landhub.auth.User;
import com.landhub.auth.UserService;
import com.landhub.booking.Booking;
import com.landhub.booking.BookingService;
import com.landhub.booking.BookingStatus;
import com.landhub.land.LandStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.Year;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final BookingService bookingService;
    private final UserService userService;

    public PaymentService(PaymentRepository paymentRepository,
                          BookingService bookingService,
                          UserService userService) {
        this.paymentRepository = paymentRepository;
        this.bookingService = bookingService;
        this.userService = userService;
    }

    @Transactional(readOnly = true)
    public List<Payment> findCustomerPayments(String email) {
        return paymentRepository.findByCustomerEmailIgnoreCaseOrderByCreatedAtDesc(email);
    }

    @Transactional(readOnly = true)
    public Optional<Payment> findCustomerPayment(Long id, String email) {
        return paymentRepository.findByIdAndCustomerEmailIgnoreCase(id, email);
    }

    @Transactional(readOnly = true)
    public List<Payment> findCustomerBookingPayments(Long bookingId, String email) {
        return paymentRepository.findByBookingIdAndCustomerEmailIgnoreCaseOrderByCreatedAtDesc(bookingId, email);
    }

    @Transactional(readOnly = true)
    public List<Payment> findAll(Optional<PaymentStatus> status) {
        return status.map(paymentRepository::findByStatusOrderByCreatedAtDesc)
                .orElseGet(paymentRepository::findAllByOrderByCreatedAtDesc);
    }

    @Transactional(readOnly = true)
    public Optional<Payment> findById(Long id) {
        return paymentRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public PaymentSummary getCustomerBookingSummary(Long bookingId, String email) {
        Booking booking = bookingService.findCustomerBooking(bookingId, email)
                .orElseThrow(() -> new IllegalArgumentException("Booking was not found."));
        return summaryFor(booking);
    }

    @Transactional(readOnly = true)
    public PaymentSummary getSummary(Booking booking) {
        return summaryFor(booking);
    }

    public Payment createCustomerPayment(String customerEmail,
                                         Long bookingId,
                                         PaymentType paymentType,
                                         PaymentMethod paymentMethod,
                                         BigDecimal amount,
                                         String bankReference,
                                         String chequeNumber,
                                         String note) {
        Booking booking = bookingService.findCustomerBookingForPayment(bookingId, customerEmail)
                .orElseThrow(() -> new IllegalArgumentException("Booking was not found."));
        validateNewPayment(booking, amount);
        validateMethodDetails(paymentMethod, bankReference, chequeNumber);

        User customer = userService.findByEmail(customerEmail)
                .orElseThrow(() -> new IllegalArgumentException("Customer account was not found."));

        Payment payment = new Payment();
        payment.setBooking(booking);
        payment.setCustomer(customer);
        payment.setPaymentType(requiredPaymentType(paymentType));
        payment.setPaymentMethod(requiredPaymentMethod(paymentMethod));
        payment.setStatus(PaymentStatus.PENDING);
        payment.setAmount(amount);
        payment.setBankReference(cleanOptional(bankReference));
        payment.setChequeNumber(cleanOptional(chequeNumber));
        payment.setNote(cleanOptional(note));
        payment.setTransactionReference(generateTransactionReference());
        return paymentRepository.save(payment);
    }

    public Payment simulateDemoSuccess(Long paymentId, String customerEmail) {
        Payment payment = getCustomerPayment(paymentId, customerEmail);
        if (payment.getPaymentMethod() != PaymentMethod.DEMO_CARD_GATEWAY) {
            throw new IllegalArgumentException("This payment is not a demo card gateway payment.");
        }
        return markPaid(payment, payment.getCustomer());
    }

    public Payment simulateDemoFailure(Long paymentId, String customerEmail) {
        Payment payment = getCustomerPayment(paymentId, customerEmail);
        if (payment.getPaymentMethod() != PaymentMethod.DEMO_CARD_GATEWAY) {
            throw new IllegalArgumentException("This payment is not a demo card gateway payment.");
        }
        return markFailed(payment, "Demo gateway failure selected by customer.", payment.getCustomer());
    }

    public Payment markPaidByAdmin(Long id, User processedBy) {
        return markPaid(getPayment(id), processedBy);
    }

    public Payment markFailedByAdmin(Long id, String failureReason, User processedBy) {
        return markFailed(getPayment(id), requiredText(failureReason, "Failure reason is required."), processedBy);
    }

    public Payment cancel(Long id, User processedBy) {
        Payment payment = getPayment(id);
        requireStatus(payment, PaymentStatus.PENDING, "Only pending payments can be cancelled.");
        payment.setStatus(PaymentStatus.CANCELLED);
        payment.setProcessedBy(processedBy);
        return paymentRepository.save(payment);
    }

    public Payment refund(Long id, String refundReason, User processedBy) {
        Payment payment = getPayment(id);
        requireStatus(payment, PaymentStatus.PAID, "Only paid payments can be refunded.");
        String reason = requiredText(refundReason, "Refund reason is required.");
        Booking booking = bookingService.findForPaymentUpdate(payment.getBooking().getId());
        payment.setBooking(booking);
        payment.setStatus(PaymentStatus.REFUNDED);
        payment.setRefundReason(reason);
        payment.setProcessedBy(processedBy);
        return paymentRepository.save(payment);
    }

    @Transactional(readOnly = true)
    public long countPending() {
        return paymentRepository.countByStatus(PaymentStatus.PENDING);
    }

    @Transactional(readOnly = true)
    public long countCustomerPayments(String email) {
        return findCustomerPayments(email).size();
    }

    @Transactional(readOnly = true)
    public BigDecimal customerTotalPaid(String email) {
        return findCustomerPayments(email).stream()
                .filter(payment -> payment.getStatus() == PaymentStatus.PAID)
                .map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Transactional(readOnly = true)
    public BigDecimal paidRevenue() {
        return paymentRepository.findByStatusOrderByCreatedAtDesc(PaymentStatus.PAID).stream()
                .map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private Payment markPaid(Payment payment, User processedBy) {
        requireStatus(payment, PaymentStatus.PENDING, "Only pending payments can be marked paid.");
        Booking booking = bookingService.findForPaymentUpdate(payment.getBooking().getId());
        payment.setBooking(booking);
        validatePaymentBooking(booking);
        PaymentSummary summary = summaryFor(booking);
        if (payment.getAmount().compareTo(summary.remainingBalance()) > 0) {
            throw new IllegalArgumentException("Payment amount exceeds remaining booking balance.");
        }

        payment.setStatus(PaymentStatus.PAID);
        payment.setPaidAt(LocalDateTime.now());
        payment.setProcessedBy(processedBy);
        Payment saved = paymentRepository.save(payment);
        completeBookingIfFullyPaid(saved.getBooking());
        return saved;
    }

    private Payment markFailed(Payment payment, String failureReason, User processedBy) {
        requireStatus(payment, PaymentStatus.PENDING, "Only pending payments can be marked failed.");
        payment.setStatus(PaymentStatus.FAILED);
        payment.setFailureReason(requiredText(failureReason, "Failure reason is required."));
        payment.setProcessedBy(processedBy);
        return paymentRepository.save(payment);
    }

    private void completeBookingIfFullyPaid(Booking booking) {
        PaymentSummary summary = summaryFor(booking);
        if (summary.bookingPrice().compareTo(BigDecimal.ZERO) > 0
                && summary.remainingBalance().compareTo(BigDecimal.ZERO) <= 0) {
            bookingService.completeAfterFullPayment(booking.getId());
        }
    }

    private PaymentSummary summaryFor(Booking booking) {
        BigDecimal bookingPrice = booking.getAgreedPrice() == null ? BigDecimal.ZERO : booking.getAgreedPrice();
        BigDecimal totalPaid = paymentRepository.findByBookingIdOrderByCreatedAtDesc(booking.getId()).stream()
                .filter(payment -> payment.getStatus() == PaymentStatus.PAID)
                .map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal remaining = bookingPrice.subtract(totalPaid);
        if (remaining.compareTo(BigDecimal.ZERO) < 0) {
            remaining = BigDecimal.ZERO;
        }
        return new PaymentSummary(booking, bookingPrice, totalPaid, remaining);
    }

    private void validateNewPayment(Booking booking, BigDecimal amount) {
        validatePaymentBooking(booking);
        PaymentSummary summary = summaryFor(booking);
        if (summary.remainingBalance().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("This booking has no remaining balance.");
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Payment amount must be greater than zero.");
        }
        if (amount.compareTo(summary.remainingBalance()) > 0) {
            throw new IllegalArgumentException("Payment amount exceeds remaining booking balance.");
        }
    }

    private void validatePaymentBooking(Booking booking) {
        if (booking.getStatus() != BookingStatus.APPROVED) {
            throw new IllegalArgumentException("Only approved bookings can receive payments.");
        }
        if (booking.getLand() == null || booking.getLand().getStatus() != LandStatus.RESERVED) {
            throw new IllegalArgumentException("The booking land must still be reserved for payment.");
        }
        if (booking.getAgreedPrice() == null || booking.getAgreedPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Booking agreed price is required before payment.");
        }
    }

    private void validateMethodDetails(PaymentMethod paymentMethod, String bankReference, String chequeNumber) {
        PaymentMethod method = requiredPaymentMethod(paymentMethod);
        if (method == PaymentMethod.BANK_TRANSFER && !hasText(bankReference)) {
            throw new IllegalArgumentException("Bank transfer reference is required.");
        }
        if (method == PaymentMethod.CHEQUE && !hasText(chequeNumber)) {
            throw new IllegalArgumentException("Cheque number is required.");
        }
    }

    private Payment getPayment(Long id) {
        return paymentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Payment was not found."));
    }

    private Payment getCustomerPayment(Long id, String email) {
        return paymentRepository.findByIdAndCustomerEmailIgnoreCase(id, email)
                .orElseThrow(() -> new IllegalArgumentException("Payment was not found."));
    }

    private void requireStatus(Payment payment, PaymentStatus status, String message) {
        if (payment.getStatus() != status) {
            throw new IllegalArgumentException(message);
        }
    }

    private PaymentType requiredPaymentType(PaymentType paymentType) {
        if (paymentType == null) {
            throw new IllegalArgumentException("Payment type is required.");
        }
        return paymentType;
    }

    private PaymentMethod requiredPaymentMethod(PaymentMethod paymentMethod) {
        if (paymentMethod == null) {
            throw new IllegalArgumentException("Payment method is required.");
        }
        return paymentMethod;
    }

    private String generateTransactionReference() {
        String reference;
        do {
            reference = "LH-" + Year.now().getValue() + "-"
                    + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase(Locale.ROOT);
        } while (paymentRepository.existsByTransactionReference(reference));
        return reference;
    }

    private String requiredText(String value, String message) {
        if (!hasText(value)) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }

    private String cleanOptional(String value) {
        return hasText(value) ? value.trim() : null;
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}

package com.landhub.payment;

import com.landhub.auth.User;
import com.landhub.booking.Booking;
import com.landhub.booking.BookingService;
import com.landhub.booking.BookingStatus;
import com.landhub.land.Land;
import com.landhub.land.LandStatus;
import com.landhub.auth.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private BookingService bookingService;

    @Mock
    private UserService userService;

    private PaymentService paymentService;
    private Booking booking;
    private User customer;
    private User processor;

    @BeforeEach
    void setUp() {
        paymentService = new PaymentService(paymentRepository, bookingService, userService);
        customer = new User();
        customer.setEmail("customer@example.com");
        processor = new User();
        processor.setEmail("staff@example.com");
        booking = booking(BookingStatus.APPROVED, LandStatus.RESERVED, "100.00");
    }

    @Test
    void createsPendingPaymentOnlyForApprovedReservedBooking() {
        when(bookingService.findCustomerBookingForPayment(1L, customer.getEmail())).thenReturn(Optional.of(booking));
        when(userService.findByEmail(customer.getEmail())).thenReturn(Optional.of(customer));
        when(paymentRepository.existsByTransactionReference(any(String.class))).thenReturn(false);
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Payment payment = paymentService.createCustomerPayment(customer.getEmail(), 1L,
                PaymentType.BOOKING_FEE, PaymentMethod.BANK_TRANSFER, money("25.00"),
                "BANK-1", null, null);

        assertEquals(PaymentStatus.PENDING, payment.getStatus());
        assertEquals("BANK-1", payment.getBankReference());
        assertNotNull(payment.getTransactionReference());
    }

    @Test
    void rejectsNonApprovedBooking() {
        booking.setStatus(BookingStatus.PENDING);
        when(bookingService.findCustomerBookingForPayment(1L, customer.getEmail())).thenReturn(Optional.of(booking));

        assertThrows(IllegalArgumentException.class, () -> createPayment("25.00", PaymentMethod.CASH, null, null));
    }

    @Test
    void rejectsBookingWhoseLandIsNotReserved() {
        booking.getLand().setStatus(LandStatus.AVAILABLE);
        when(bookingService.findCustomerBookingForPayment(1L, customer.getEmail())).thenReturn(Optional.of(booking));

        assertThrows(IllegalArgumentException.class, () -> createPayment("25.00", PaymentMethod.CASH, null, null));
    }

    @Test
    void rejectsNonPositiveAmount() {
        when(bookingService.findCustomerBookingForPayment(1L, customer.getEmail())).thenReturn(Optional.of(booking));

        assertThrows(IllegalArgumentException.class, () -> createPayment("0.00", PaymentMethod.CASH, null, null));
    }

    @Test
    void rejectsOverpayment() {
        when(bookingService.findCustomerBookingForPayment(1L, customer.getEmail())).thenReturn(Optional.of(booking));

        assertThrows(IllegalArgumentException.class, () -> createPayment("100.01", PaymentMethod.CASH, null, null));
    }

    @Test
    void demoSuccessMarksPaidAndCompletesFullyPaidBooking() {
        Payment payment = pendingPayment(PaymentMethod.DEMO_CARD_GATEWAY, "100.00");
        when(paymentRepository.findByIdAndCustomerEmailIgnoreCase(1L, customer.getEmail())).thenReturn(Optional.of(payment));
        when(bookingService.findForPaymentUpdate(1L)).thenReturn(booking);
        when(paymentRepository.findByBookingIdOrderByCreatedAtDesc(1L)).thenReturn(List.of(payment));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Payment result = paymentService.simulateDemoSuccess(1L, customer.getEmail());

        assertEquals(PaymentStatus.PAID, result.getStatus());
        assertNotNull(result.getPaidAt());
        verify(bookingService).completeAfterFullPayment(1L);
    }

    @Test
    void demoFailureMarksFailedWithReason() {
        Payment payment = pendingPayment(PaymentMethod.DEMO_CARD_GATEWAY, "25.00");
        when(paymentRepository.findByIdAndCustomerEmailIgnoreCase(1L, customer.getEmail())).thenReturn(Optional.of(payment));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Payment result = paymentService.simulateDemoFailure(1L, customer.getEmail());

        assertEquals(PaymentStatus.FAILED, result.getStatus());
        assertEquals("Demo gateway failure selected by customer.", result.getFailureReason());
        verify(bookingService, never()).completeAfterFullPayment(anyLong());
    }

    @Test
    void bankTransferRemainsPending() {
        Payment payment = createPayment("25.00", PaymentMethod.BANK_TRANSFER, "BANK-1", null);

        assertEquals(PaymentStatus.PENDING, payment.getStatus());
    }

    @Test
    void chequeNumberIsRequired() {
        when(bookingService.findCustomerBookingForPayment(1L, customer.getEmail())).thenReturn(Optional.of(booking));

        assertThrows(IllegalArgumentException.class, () -> createPayment("25.00", PaymentMethod.CHEQUE, null, null));
    }

    @Test
    void cashRemainsPending() {
        Payment payment = createPayment("25.00", PaymentMethod.CASH, null, null);

        assertEquals(PaymentStatus.PENDING, payment.getStatus());
    }

    @Test
    void partialPaymentKeepsBookingApprovedAndDoesNotComplete() {
        Payment payment = pendingPayment(PaymentMethod.CASH, "25.00");
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(payment));
        when(bookingService.findForPaymentUpdate(1L)).thenReturn(booking);
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        paymentService.markPaidByAdmin(1L, processor);

        assertEquals(BookingStatus.APPROVED, booking.getStatus());
        assertEquals(LandStatus.RESERVED, booking.getLand().getStatus());
        verify(bookingService, never()).completeAfterFullPayment(anyLong());
    }

    @Test
    void failedCancelledAndRefundedPaymentsDoNotCount() {
        Payment paid = payment(PaymentStatus.PAID, "40.00");
        Payment failed = payment(PaymentStatus.FAILED, "20.00");
        Payment cancelled = payment(PaymentStatus.CANCELLED, "15.00");
        Payment refunded = payment(PaymentStatus.REFUNDED, "10.00");
        when(paymentRepository.findByBookingIdOrderByCreatedAtDesc(1L))
                .thenReturn(List.of(paid, failed, cancelled, refunded));

        PaymentSummary summary = paymentService.getSummary(booking);

        assertEquals(money("40.00"), summary.totalPaid());
        assertEquals(money("60.00"), summary.remainingBalance());
    }

    @Test
    void refundRequiresReasonAndReducesEffectiveTotal() {
        Payment paid = payment(PaymentStatus.PAID, "40.00");
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(paid));
        when(bookingService.findForPaymentUpdate(1L)).thenReturn(booking);
        when(paymentRepository.findByBookingIdOrderByCreatedAtDesc(1L)).thenReturn(List.of(paid));

        assertThrows(IllegalArgumentException.class, () -> paymentService.refund(1L, " ", processor));

        paymentService.refund(1L, "Customer refund", processor);
        assertEquals(PaymentStatus.REFUNDED, paid.getStatus());
        assertEquals(0, paymentService.getSummary(booking).totalPaid().compareTo(money("0.00")));
        assertEquals(BookingStatus.APPROVED, booking.getStatus());
    }

    @Test
    void invalidStatusTransitionsAreBlocked() {
        Payment paid = payment(PaymentStatus.PAID, "25.00");
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(paid));
        assertThrows(IllegalArgumentException.class, () -> paymentService.markPaidByAdmin(1L, processor));
        assertThrows(IllegalArgumentException.class, () -> paymentService.cancel(1L, processor));

        Payment pending = pendingPayment(PaymentMethod.CASH, "25.00");
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(pending));
        assertThrows(IllegalArgumentException.class, () -> paymentService.refund(1L, "Refund", processor));
    }

    @Test
    void customerCannotReadAnotherCustomersPayment() {
        when(paymentRepository.findByIdAndCustomerEmailIgnoreCase(1L, customer.getEmail())).thenReturn(Optional.empty());

        assertEquals(Optional.empty(), paymentService.findCustomerPayment(1L, customer.getEmail()));
    }

    private Payment createPayment(String amount, PaymentMethod method, String bankReference, String chequeNumber) {
        lenient().when(bookingService.findCustomerBookingForPayment(1L, customer.getEmail())).thenReturn(Optional.of(booking));
        lenient().when(userService.findByEmail(customer.getEmail())).thenReturn(Optional.of(customer));
        lenient().when(paymentRepository.existsByTransactionReference(any(String.class))).thenReturn(false);
        lenient().when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));
        return paymentService.createCustomerPayment(customer.getEmail(), 1L, PaymentType.INSTALLMENT, method,
                money(amount), bankReference, chequeNumber, null);
    }

    private Payment pendingPayment(PaymentMethod method, String amount) {
        Payment payment = payment(PaymentStatus.PENDING, amount);
        payment.setPaymentMethod(method);
        payment.setBooking(booking);
        payment.setCustomer(customer);
        return payment;
    }

    private Payment payment(PaymentStatus status, String amount) {
        Payment payment = new Payment();
        payment.setId(1L);
        payment.setStatus(status);
        payment.setAmount(money(amount));
        payment.setBooking(booking);
        return payment;
    }

    private Booking booking(BookingStatus status, LandStatus landStatus, String price) {
        Land land = new Land();
        land.setId(1L);
        land.setStatus(landStatus);
        Booking result = new Booking();
        result.setId(1L);
        result.setStatus(status);
        result.setAgreedPrice(money(price));
        result.setLand(land);
        result.setCustomer(customer);
        return result;
    }

    private BigDecimal money(String value) {
        return new BigDecimal(value);
    }
}

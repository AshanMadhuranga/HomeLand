package com.landhub.booking;

import com.landhub.auth.User;
import com.landhub.auth.UserService;
import com.landhub.land.Land;
import com.landhub.land.LandService;
import com.landhub.land.LandStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@Transactional
public class BookingService {

    private static final Set<BookingStatus> ACTIVE_BOOKING_STATUSES = Set.of(BookingStatus.PENDING, BookingStatus.APPROVED);

    private final BookingRepository bookingRepository;
    private final LandService landService;
    private final UserService userService;

    public BookingService(BookingRepository bookingRepository, LandService landService, UserService userService) {
        this.bookingRepository = bookingRepository;
        this.landService = landService;
        this.userService = userService;
    }

    @Transactional(readOnly = true)
    public List<Booking> findCustomerBookings(String email) {
        return bookingRepository.findByCustomerEmailIgnoreCaseOrderByCreatedAtDesc(email);
    }

    @Transactional(readOnly = true)
    public Optional<Booking> findCustomerBooking(Long id, String email) {
        return bookingRepository.findByIdAndCustomerEmailIgnoreCase(id, email);
    }

    @Transactional(readOnly = true)
    public List<Booking> findAll() {
        return bookingRepository.findAllByOrderByCreatedAtDesc();
    }

    @Transactional(readOnly = true)
    public Optional<Booking> findById(Long id) {
        return bookingRepository.findById(id);
    }

    public Booking create(String customerEmail, Long landId, String customerNote) {
        User customer = userService.findByEmail(customerEmail)
                .orElseThrow(() -> new IllegalArgumentException("Customer account was not found."));
        Land land = landService.findPublicLandById(landId)
                .orElseThrow(() -> new IllegalArgumentException("Land listing is not available for booking."));

        if (land.getStatus() != LandStatus.AVAILABLE) {
            throw new IllegalArgumentException("Only available land can receive booking requests.");
        }

        if (bookingRepository.existsByCustomerEmailIgnoreCaseAndLandIdAndStatusIn(customerEmail, landId, ACTIVE_BOOKING_STATUSES)) {
            throw new IllegalArgumentException("You already have an active booking request for this land.");
        }

        if (bookingRepository.existsByLandIdAndStatusIn(landId, Set.of(BookingStatus.APPROVED))) {
            throw new IllegalArgumentException("This land already has an approved booking.");
        }

        Booking booking = new Booking();
        booking.setCustomer(customer);
        booking.setLand(land);
        booking.setCustomerNote(cleanOptional(customerNote));
        booking.setStatus(BookingStatus.PENDING);
        return bookingRepository.save(booking);
    }

    public Booking approve(Long id, BigDecimal agreedPrice, String adminNote, User reviewer) {
        Booking booking = getBooking(id);

        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new IllegalArgumentException("Only pending bookings can be approved.");
        }

        if (booking.getLand().getStatus() != LandStatus.AVAILABLE) {
            throw new IllegalArgumentException("This land is no longer available for booking approval.");
        }

        booking.setStatus(BookingStatus.APPROVED);
        booking.setAgreedPrice(agreedPrice == null ? booking.getLand().getPrice() : agreedPrice);
        booking.setAdminNote(cleanOptional(adminNote));
        booking.setReviewedAt(LocalDateTime.now());
        booking.setReviewedBy(reviewer);
        Booking saved = bookingRepository.save(booking);
        landService.markReserved(saved.getLand().getId());
        return saved;
    }

    public Booking reject(Long id, String rejectionReason, User reviewer) {
        Booking booking = getBooking(id);
        booking.setStatus(BookingStatus.REJECTED);
        booking.setRejectionReason(requiredText(rejectionReason, "Rejection reason is required."));
        booking.setReviewedAt(LocalDateTime.now());
        booking.setReviewedBy(reviewer);
        return bookingRepository.save(booking);
    }

    public Booking cancelByAdmin(Long id, String adminNote, User reviewer) {
        Booking booking = getBooking(id);
        BookingStatus previousStatus = booking.getStatus();
        booking.setStatus(BookingStatus.CANCELLED);
        booking.setAdminNote(cleanOptional(adminNote));
        booking.setReviewedAt(LocalDateTime.now());
        booking.setReviewedBy(reviewer);
        Booking saved = bookingRepository.save(booking);

        if (previousStatus == BookingStatus.APPROVED) {
            landService.markAvailable(saved.getLand().getId());
        }

        return saved;
    }

    public Booking cancelCustomerBooking(Long id, String customerEmail) {
        Booking booking = bookingRepository.findByIdAndCustomerEmailIgnoreCase(id, customerEmail)
                .orElseThrow(() -> new IllegalArgumentException("Booking request was not found."));

        if (!booking.isCancellableByCustomer()) {
            throw new IllegalArgumentException("This booking request cannot be cancelled.");
        }

        BookingStatus previousStatus = booking.getStatus();
        booking.setStatus(BookingStatus.CANCELLED);
        Booking saved = bookingRepository.save(booking);

        if (previousStatus == BookingStatus.APPROVED) {
            landService.markAvailable(saved.getLand().getId());
        }

        return saved;
    }

    @Transactional(readOnly = true)
    public long countPending() {
        return bookingRepository.countByStatus(BookingStatus.PENDING);
    }

    private Booking getBooking(Long id) {
        return bookingRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Booking request was not found."));
    }

    private String requiredText(String value, String message) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }

    private String cleanOptional(String value) {
        return value == null || value.trim().isEmpty() ? null : value.trim();
    }
}

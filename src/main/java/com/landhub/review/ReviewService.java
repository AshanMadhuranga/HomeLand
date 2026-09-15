package com.landhub.review;

import com.landhub.auth.User;
import com.landhub.auth.UserService;
import com.landhub.booking.Booking;
import com.landhub.booking.BookingService;
import com.landhub.booking.BookingStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final BookingService bookingService;
    private final UserService userService;

    public ReviewService(ReviewRepository reviewRepository, BookingService bookingService, UserService userService) {
        this.reviewRepository = reviewRepository;
        this.bookingService = bookingService;
        this.userService = userService;
    }

    @Transactional(readOnly = true)
    public List<Review> findCustomerReviews(String email) {
        return reviewRepository.findByCustomerEmailIgnoreCaseOrderByCreatedAtDesc(email);
    }

    @Transactional(readOnly = true)
    public Optional<Review> findCustomerReview(Long id, String email) {
        return reviewRepository.findByIdAndCustomerEmailIgnoreCase(id, email);
    }

    @Transactional(readOnly = true)
    public Optional<Review> findCustomerReviewForBooking(Long bookingId, String email) {
        return reviewRepository.findByBookingIdAndCustomerEmailIgnoreCaseAndActiveTrue(bookingId, email);
    }

    @Transactional(readOnly = true)
    public List<Review> findApprovedReviewsForLand(Long landId) {
        return reviewRepository.findByLandIdAndStatusAndActiveTrueOrderByCreatedAtDesc(landId, ReviewStatus.APPROVED);
    }

    @Transactional(readOnly = true)
    public double averageRatingForLand(Long landId) {
        return findApprovedReviewsForLand(landId).stream().mapToInt(Review::getRating).average().orElse(0);
    }

    @Transactional(readOnly = true)
    public List<Review> findAll() {
        return reviewRepository.findAllByOrderByCreatedAtDesc();
    }

    @Transactional(readOnly = true)
    public Optional<Review> findById(Long id) {
        return reviewRepository.findById(id);
    }

    public Review create(String customerEmail, Long bookingId, Integer rating, String title, String comment) {
        Booking booking = eligibleBooking(customerEmail, bookingId);
        if (reviewRepository.existsByBookingIdAndCustomerEmailIgnoreCaseAndActiveTrue(bookingId, customerEmail)) {
            throw new IllegalArgumentException("You already submitted a review for this completed booking.");
        }
        User customer = userService.findByEmail(customerEmail)
                .orElseThrow(() -> new IllegalArgumentException("Customer account was not found."));
        Review review = new Review();
        review.setCustomer(customer);
        review.setBooking(booking);
        review.setLand(booking.getLand());
        applyReviewFields(review, rating, title, comment);
        review.setStatus(ReviewStatus.PENDING);
        review.setActive(true);
        return reviewRepository.save(review);
    }

    public Review update(Long id, String customerEmail, Integer rating, String title, String comment) {
        Review review = reviewRepository.findByIdAndCustomerEmailIgnoreCase(id, customerEmail)
                .orElseThrow(() -> new IllegalArgumentException("Review was not found."));
        if (!review.isActive()) {
            throw new IllegalArgumentException("Hidden reviews cannot be edited.");
        }
        eligibleBooking(customerEmail, review.getBooking().getId());
        applyReviewFields(review, rating, title, comment);
        review.setStatus(ReviewStatus.PENDING);
        review.setModerationReason(null);
        return reviewRepository.save(review);
    }

    public Review hideByCustomer(Long id, String customerEmail) {
        Review review = reviewRepository.findByIdAndCustomerEmailIgnoreCase(id, customerEmail)
                .orElseThrow(() -> new IllegalArgumentException("Review was not found."));
        review.setStatus(ReviewStatus.HIDDEN);
        review.setActive(false);
        return reviewRepository.save(review);
    }

    public Review approve(Long id) {
        Review review = getReview(id);
        review.setStatus(ReviewStatus.APPROVED);
        review.setActive(true);
        review.setModerationReason(null);
        return reviewRepository.save(review);
    }

    public Review reject(Long id, String reason) {
        Review review = getReview(id);
        review.setStatus(ReviewStatus.REJECTED);
        review.setModerationReason(cleanOptional(reason));
        return reviewRepository.save(review);
    }

    public Review hide(Long id, String reason) {
        Review review = getReview(id);
        review.setStatus(ReviewStatus.HIDDEN);
        review.setActive(false);
        review.setModerationReason(cleanOptional(reason));
        return reviewRepository.save(review);
    }

    @Transactional(readOnly = true)
    public long countCustomerReviews(String email) {
        return reviewRepository.countByCustomerEmailIgnoreCase(email);
    }

    @Transactional(readOnly = true)
    public long countPending() {
        return reviewRepository.countByStatus(ReviewStatus.PENDING);
    }

    private Booking eligibleBooking(String email, Long bookingId) {
        Booking booking = bookingService.findCustomerBooking(bookingId, email)
                .orElseThrow(() -> new IllegalArgumentException("Completed booking was not found."));
        if (booking.getStatus() != BookingStatus.COMPLETED) {
            throw new IllegalArgumentException("Only completed bookings can be reviewed.");
        }
        return booking;
    }

    private Review getReview(Long id) {
        return reviewRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Review was not found."));
    }

    private void applyReviewFields(Review review, Integer rating, String title, String comment) {
        if (rating == null || rating < 1 || rating > 5) {
            throw new IllegalArgumentException("Rating must be between 1 and 5.");
        }
        review.setRating(rating);
        review.setTitle(requiredText(title, "Review title is required."));
        review.setComment(requiredText(comment, "Review comment is required."));
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

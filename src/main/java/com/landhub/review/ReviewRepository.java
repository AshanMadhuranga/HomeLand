package com.landhub.review;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    List<Review> findByCustomerEmailIgnoreCaseOrderByCreatedAtDesc(String email);

    Optional<Review> findByIdAndCustomerEmailIgnoreCase(Long id, String email);

    Optional<Review> findByBookingIdAndCustomerEmailIgnoreCaseAndActiveTrue(Long bookingId, String email);

    boolean existsByBookingIdAndCustomerEmailIgnoreCaseAndActiveTrue(Long bookingId, String email);

    List<Review> findByLandIdAndStatusAndActiveTrueOrderByCreatedAtDesc(Long landId, ReviewStatus status);

    List<Review> findAllByOrderByCreatedAtDesc();

    long countByCustomerEmailIgnoreCase(String email);

    long countByStatus(ReviewStatus status);
}

package com.landhub.payment;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    List<Payment> findByCustomerEmailIgnoreCaseOrderByCreatedAtDesc(String email);

    Optional<Payment> findByIdAndCustomerEmailIgnoreCase(Long id, String email);

    List<Payment> findByBookingIdAndCustomerEmailIgnoreCaseOrderByCreatedAtDesc(Long bookingId, String email);

    List<Payment> findByBookingIdOrderByCreatedAtDesc(Long bookingId);

    List<Payment> findAllByOrderByCreatedAtDesc();

    List<Payment> findByStatusOrderByCreatedAtDesc(PaymentStatus status);

    long countByStatus(PaymentStatus status);

    boolean existsByTransactionReference(String transactionReference);
}

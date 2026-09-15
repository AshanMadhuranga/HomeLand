package com.landhub.feedback;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FeedbackRepository extends JpaRepository<Feedback, Long> {

    List<Feedback> findByCustomerEmailIgnoreCaseOrderByCreatedAtDesc(String email);

    Optional<Feedback> findByIdAndCustomerEmailIgnoreCase(Long id, String email);

    List<Feedback> findAllByOrderByCreatedAtDesc();

    long countByCustomerEmailIgnoreCase(String email);

    long countByCustomerEmailIgnoreCaseAndStatus(String email, FeedbackStatus status);

    long countByStatus(FeedbackStatus status);
}

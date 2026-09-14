package com.landhub.inquiry;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InquiryRepository extends JpaRepository<Inquiry, Long> {

    List<Inquiry> findByCustomerEmailIgnoreCaseOrderByCreatedAtDesc(String email);

    Optional<Inquiry> findByIdAndCustomerEmailIgnoreCase(Long id, String email);

    List<Inquiry> findAllByOrderByCreatedAtDesc();

    long countByStatus(InquiryStatus status);
}

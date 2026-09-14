package com.landhub.sitevisit;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SiteVisitRepository extends JpaRepository<SiteVisit, Long> {

    List<SiteVisit> findByCustomerEmailIgnoreCaseOrderByCreatedAtDesc(String email);

    Optional<SiteVisit> findByIdAndCustomerEmailIgnoreCase(Long id, String email);

    List<SiteVisit> findAllByOrderByCreatedAtDesc();

    long countByStatus(SiteVisitStatus status);
}

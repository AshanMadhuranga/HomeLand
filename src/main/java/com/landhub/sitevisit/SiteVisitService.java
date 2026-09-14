package com.landhub.sitevisit;

import com.landhub.auth.User;
import com.landhub.auth.UserService;
import com.landhub.land.Land;
import com.landhub.land.LandService;
import com.landhub.land.LandStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class SiteVisitService {

    private final SiteVisitRepository siteVisitRepository;
    private final LandService landService;
    private final UserService userService;

    public SiteVisitService(SiteVisitRepository siteVisitRepository, LandService landService, UserService userService) {
        this.siteVisitRepository = siteVisitRepository;
        this.landService = landService;
        this.userService = userService;
    }

    @Transactional(readOnly = true)
    public List<SiteVisit> findCustomerSiteVisits(String email) {
        return siteVisitRepository.findByCustomerEmailIgnoreCaseOrderByCreatedAtDesc(email);
    }

    @Transactional(readOnly = true)
    public Optional<SiteVisit> findCustomerSiteVisit(Long id, String email) {
        return siteVisitRepository.findByIdAndCustomerEmailIgnoreCase(id, email);
    }

    @Transactional(readOnly = true)
    public List<SiteVisit> findAll() {
        return siteVisitRepository.findAllByOrderByCreatedAtDesc();
    }

    @Transactional(readOnly = true)
    public Optional<SiteVisit> findById(Long id) {
        return siteVisitRepository.findById(id);
    }

    public SiteVisit create(String customerEmail, Long landId, LocalDate preferredDate, LocalTime preferredTime, String note) {
        User customer = userService.findByEmail(customerEmail)
                .orElseThrow(() -> new IllegalArgumentException("Customer account was not found."));
        Land land = landService.findPublicLandById(landId)
                .orElseThrow(() -> new IllegalArgumentException("Land listing is not available for site visits."));

        if (land.getStatus() != LandStatus.AVAILABLE) {
            throw new IllegalArgumentException("Only available land can receive site visit requests.");
        }

        validateFutureDate(preferredDate, preferredTime);

        SiteVisit visit = new SiteVisit();
        visit.setCustomer(customer);
        visit.setLand(land);
        visit.setPreferredDate(preferredDate);
        visit.setPreferredTime(preferredTime);
        visit.setNote(cleanOptional(note));
        visit.setStatus(SiteVisitStatus.PENDING);
        return siteVisitRepository.save(visit);
    }

    public SiteVisit approve(Long id, String adminNote, User reviewer) {
        SiteVisit visit = getVisit(id);
        visit.setStatus(SiteVisitStatus.APPROVED);
        visit.setAdminNote(cleanOptional(adminNote));
        stampReview(visit, reviewer);
        return siteVisitRepository.save(visit);
    }

    public SiteVisit reject(Long id, String adminNote, User reviewer) {
        SiteVisit visit = getVisit(id);
        visit.setStatus(SiteVisitStatus.REJECTED);
        visit.setAdminNote(requiredText(adminNote, "Rejection note is required."));
        stampReview(visit, reviewer);
        return siteVisitRepository.save(visit);
    }

    public SiteVisit reschedule(Long id, LocalDate preferredDate, LocalTime preferredTime, String adminNote, User reviewer) {
        validateFutureDate(preferredDate, preferredTime);
        SiteVisit visit = getVisit(id);
        visit.setPreferredDate(preferredDate);
        visit.setPreferredTime(preferredTime);
        visit.setStatus(SiteVisitStatus.RESCHEDULED);
        visit.setAdminNote(requiredText(adminNote, "Reschedule note is required."));
        stampReview(visit, reviewer);
        return siteVisitRepository.save(visit);
    }

    public SiteVisit complete(Long id, User reviewer) {
        SiteVisit visit = getVisit(id);
        visit.setStatus(SiteVisitStatus.COMPLETED);
        stampReview(visit, reviewer);
        return siteVisitRepository.save(visit);
    }

    public SiteVisit cancelCustomerSiteVisit(Long id, String customerEmail) {
        SiteVisit visit = siteVisitRepository.findByIdAndCustomerEmailIgnoreCase(id, customerEmail)
                .orElseThrow(() -> new IllegalArgumentException("Site visit request was not found."));

        if (!visit.isCancellableByCustomer()) {
            throw new IllegalArgumentException("This site visit request cannot be cancelled.");
        }

        visit.setStatus(SiteVisitStatus.CANCELLED);
        return siteVisitRepository.save(visit);
    }

    @Transactional(readOnly = true)
    public long countPending() {
        return siteVisitRepository.countByStatus(SiteVisitStatus.PENDING);
    }

    private SiteVisit getVisit(Long id) {
        return siteVisitRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Site visit request was not found."));
    }

    private void stampReview(SiteVisit visit, User reviewer) {
        visit.setReviewedAt(LocalDateTime.now());
        visit.setReviewedBy(reviewer);
    }

    private void validateFutureDate(LocalDate date, LocalTime time) {
        if (date == null) {
            throw new IllegalArgumentException("Preferred date is required.");
        }
        if (time == null) {
            throw new IllegalArgumentException("Preferred time is required.");
        }
        if (date.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Preferred date cannot be in the past.");
        }
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

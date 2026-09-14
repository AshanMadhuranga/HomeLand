package com.landhub.inquiry;

import com.landhub.auth.User;
import com.landhub.auth.UserService;
import com.landhub.land.Land;
import com.landhub.land.LandService;
import com.landhub.land.LandStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class InquiryService {

    private final InquiryRepository inquiryRepository;
    private final LandService landService;
    private final UserService userService;

    public InquiryService(InquiryRepository inquiryRepository, LandService landService, UserService userService) {
        this.inquiryRepository = inquiryRepository;
        this.landService = landService;
        this.userService = userService;
    }

    @Transactional(readOnly = true)
    public List<Inquiry> findCustomerInquiries(String email) {
        return inquiryRepository.findByCustomerEmailIgnoreCaseOrderByCreatedAtDesc(email);
    }

    @Transactional(readOnly = true)
    public Optional<Inquiry> findCustomerInquiry(Long id, String email) {
        return inquiryRepository.findByIdAndCustomerEmailIgnoreCase(id, email);
    }

    @Transactional(readOnly = true)
    public List<Inquiry> findAll() {
        return inquiryRepository.findAllByOrderByCreatedAtDesc();
    }

    @Transactional(readOnly = true)
    public Optional<Inquiry> findById(Long id) {
        return inquiryRepository.findById(id);
    }

    public Inquiry create(String customerEmail, Long landId, String subject, String message) {
        User customer = userService.findByEmail(customerEmail)
                .orElseThrow(() -> new IllegalArgumentException("Customer account was not found."));
        Land land = landService.findPublicLandById(landId)
                .orElseThrow(() -> new IllegalArgumentException("Land listing is not available for inquiries."));

        if (land.getStatus() == LandStatus.SOLD || land.getStatus() == LandStatus.INACTIVE || land.getStatus() == LandStatus.PENDING) {
            throw new IllegalArgumentException("This land is not available for new inquiries.");
        }

        Inquiry inquiry = new Inquiry();
        inquiry.setCustomer(customer);
        inquiry.setLand(land);
        inquiry.setSubject(requiredText(subject, "Subject is required."));
        inquiry.setMessage(requiredText(message, "Message is required."));
        inquiry.setStatus(InquiryStatus.OPEN);
        inquiry.setActive(true);
        return inquiryRepository.save(inquiry);
    }

    public Inquiry respond(Long id, String response, User responder) {
        Inquiry inquiry = getInquiry(id);
        inquiry.setResponse(requiredText(response, "Response is required."));
        inquiry.setStatus(InquiryStatus.RESPONDED);
        inquiry.setRespondedAt(LocalDateTime.now());
        inquiry.setRespondedBy(responder);
        return inquiryRepository.save(inquiry);
    }

    public Inquiry resolve(Long id, User responder) {
        Inquiry inquiry = getInquiry(id);
        inquiry.setStatus(InquiryStatus.RESOLVED);
        inquiry.setRespondedAt(LocalDateTime.now());
        inquiry.setRespondedBy(responder);
        return inquiryRepository.save(inquiry);
    }

    public Inquiry cancelCustomerInquiry(Long id, String customerEmail) {
        Inquiry inquiry = inquiryRepository.findByIdAndCustomerEmailIgnoreCase(id, customerEmail)
                .orElseThrow(() -> new IllegalArgumentException("Inquiry was not found."));

        if (inquiry.getStatus() != InquiryStatus.OPEN) {
            throw new IllegalArgumentException("Only open inquiries can be cancelled.");
        }

        inquiry.setStatus(InquiryStatus.CANCELLED);
        inquiry.setActive(false);
        return inquiryRepository.save(inquiry);
    }

    @Transactional(readOnly = true)
    public long countOpen() {
        return inquiryRepository.countByStatus(InquiryStatus.OPEN);
    }

    private Inquiry getInquiry(Long id) {
        return inquiryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Inquiry was not found."));
    }

    private String requiredText(String value, String message) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }
}

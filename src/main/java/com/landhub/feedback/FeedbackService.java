package com.landhub.feedback;

import com.landhub.auth.User;
import com.landhub.auth.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class FeedbackService {

    private final FeedbackRepository feedbackRepository;
    private final UserService userService;

    public FeedbackService(FeedbackRepository feedbackRepository, UserService userService) {
        this.feedbackRepository = feedbackRepository;
        this.userService = userService;
    }

    @Transactional(readOnly = true)
    public List<Feedback> findCustomerFeedback(String email) {
        return feedbackRepository.findByCustomerEmailIgnoreCaseOrderByCreatedAtDesc(email);
    }

    @Transactional(readOnly = true)
    public Optional<Feedback> findCustomerFeedback(Long id, String email) {
        return feedbackRepository.findByIdAndCustomerEmailIgnoreCase(id, email);
    }

    @Transactional(readOnly = true)
    public List<Feedback> findAll() {
        return feedbackRepository.findAllByOrderByCreatedAtDesc();
    }

    @Transactional(readOnly = true)
    public Optional<Feedback> findById(Long id) {
        return feedbackRepository.findById(id);
    }

    public Feedback create(String customerEmail, FeedbackType type, String subject, String message) {
        User customer = userService.findByEmail(customerEmail)
                .orElseThrow(() -> new IllegalArgumentException("Customer account was not found."));
        Feedback feedback = new Feedback();
        feedback.setCustomer(customer);
        feedback.setType(requiredType(type));
        feedback.setSubject(requiredText(subject, "Subject is required."));
        feedback.setMessage(requiredText(message, "Message is required."));
        feedback.setStatus(FeedbackStatus.OPEN);
        feedback.setActive(true);
        return feedbackRepository.save(feedback);
    }

    public Feedback cancel(Long id, String customerEmail) {
        Feedback feedback = feedbackRepository.findByIdAndCustomerEmailIgnoreCase(id, customerEmail)
                .orElseThrow(() -> new IllegalArgumentException("Feedback was not found."));
        if (feedback.getStatus() != FeedbackStatus.OPEN) {
            throw new IllegalArgumentException("Only open feedback can be cancelled.");
        }
        feedback.setStatus(FeedbackStatus.CANCELLED);
        feedback.setActive(false);
        return feedbackRepository.save(feedback);
    }

    public Feedback markInReview(Long id) {
        Feedback feedback = getFeedback(id);
        if (feedback.getStatus() == FeedbackStatus.CANCELLED || feedback.getStatus() == FeedbackStatus.RESOLVED) {
            throw new IllegalArgumentException("This feedback cannot be marked in review.");
        }
        feedback.setStatus(FeedbackStatus.IN_REVIEW);
        return feedbackRepository.save(feedback);
    }

    public Feedback respond(Long id, String response, User responder) {
        Feedback feedback = getFeedback(id);
        if (feedback.getStatus() == FeedbackStatus.CANCELLED) {
            throw new IllegalArgumentException("Cancelled feedback cannot be responded to.");
        }
        feedback.setAdminResponse(requiredText(response, "Response is required."));
        feedback.setRespondedBy(responder);
        feedback.setRespondedAt(LocalDateTime.now());
        feedback.setStatus(FeedbackStatus.RESPONDED);
        return feedbackRepository.save(feedback);
    }

    public Feedback resolve(Long id) {
        Feedback feedback = getFeedback(id);
        if (feedback.getStatus() == FeedbackStatus.CANCELLED) {
            throw new IllegalArgumentException("Cancelled feedback cannot be resolved.");
        }
        feedback.setStatus(FeedbackStatus.RESOLVED);
        return feedbackRepository.save(feedback);
    }

    @Transactional(readOnly = true)
    public long countCustomerFeedback(String email) {
        return feedbackRepository.countByCustomerEmailIgnoreCase(email);
    }

    @Transactional(readOnly = true)
    public long countCustomerOpenFeedback(String email) {
        return feedbackRepository.countByCustomerEmailIgnoreCaseAndStatus(email, FeedbackStatus.OPEN);
    }

    @Transactional(readOnly = true)
    public long countOpen() {
        return feedbackRepository.countByStatus(FeedbackStatus.OPEN);
    }

    private Feedback getFeedback(Long id) {
        return feedbackRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Feedback was not found."));
    }

    private FeedbackType requiredType(FeedbackType type) {
        if (type == null) {
            throw new IllegalArgumentException("Feedback type is required.");
        }
        return type;
    }

    private String requiredText(String value, String message) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }
}

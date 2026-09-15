package com.landhub.customer;

import com.landhub.auth.User;
import com.landhub.auth.UserService;
import com.landhub.booking.Booking;
import com.landhub.booking.BookingService;
import com.landhub.booking.BookingStatus;
import com.landhub.feedback.Feedback;
import com.landhub.feedback.FeedbackService;
import com.landhub.inquiry.Inquiry;
import com.landhub.inquiry.InquiryService;
import com.landhub.payment.Payment;
import com.landhub.payment.PaymentService;
import com.landhub.review.Review;
import com.landhub.review.ReviewService;
import com.landhub.sitevisit.SiteVisit;
import com.landhub.sitevisit.SiteVisitService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class CustomerManagementService {

    private final UserService userService;
    private final InquiryService inquiryService;
    private final SiteVisitService siteVisitService;
    private final BookingService bookingService;
    private final PaymentService paymentService;
    private final ReviewService reviewService;
    private final FeedbackService feedbackService;

    public CustomerManagementService(UserService userService,
                                     InquiryService inquiryService,
                                     SiteVisitService siteVisitService,
                                     BookingService bookingService,
                                     PaymentService paymentService,
                                     ReviewService reviewService,
                                     FeedbackService feedbackService) {
        this.userService = userService;
        this.inquiryService = inquiryService;
        this.siteVisitService = siteVisitService;
        this.bookingService = bookingService;
        this.paymentService = paymentService;
        this.reviewService = reviewService;
        this.feedbackService = feedbackService;
    }

    public List<User> findCustomers() {
        return userService.findCustomers();
    }

    public CustomerCrmSummary summary(User customer) {
        String email = customer.getEmail();
        List<Booking> bookings = bookingService.findCustomerBookings(email);
        return new CustomerCrmSummary(
                customer,
                inquiryService.findCustomerInquiries(email).size(),
                siteVisitService.findCustomerSiteVisits(email).size(),
                bookings.size(),
                bookings.stream().filter(booking -> booking.getStatus() == BookingStatus.COMPLETED).count(),
                paymentService.customerTotalPaid(email),
                reviewService.countCustomerReviews(email),
                feedbackService.countCustomerFeedback(email)
        );
    }

    public List<CustomerInteractionItem> timeline(String email) {
        List<CustomerInteractionItem> items = new ArrayList<>();
        for (Inquiry inquiry : inquiryService.findCustomerInquiries(email)) {
            items.add(new CustomerInteractionItem(inquiry.getCreatedAt(), "Inquiry", inquiry.getSubject(), inquiry.getStatus().name(), inquiry.getId()));
        }
        for (SiteVisit visit : siteVisitService.findCustomerSiteVisits(email)) {
            items.add(new CustomerInteractionItem(visit.getCreatedAt(), "Site Visit", visit.getLand().getTitle(), visit.getStatus().name(), visit.getId()));
        }
        for (Booking booking : bookingService.findCustomerBookings(email)) {
            items.add(new CustomerInteractionItem(booking.getCreatedAt(), "Booking", booking.getLand().getTitle(), booking.getStatus().name(), booking.getId()));
        }
        for (Payment payment : paymentService.findCustomerPayments(email)) {
            items.add(new CustomerInteractionItem(payment.getCreatedAt(), "Payment", payment.getTransactionReference(), payment.getStatus().name(), payment.getId()));
        }
        for (Review review : reviewService.findCustomerReviews(email)) {
            items.add(new CustomerInteractionItem(review.getCreatedAt(), "Review", review.getTitle(), review.getStatus().name(), review.getId()));
        }
        for (Feedback feedback : feedbackService.findCustomerFeedback(email)) {
            items.add(new CustomerInteractionItem(feedback.getCreatedAt(), "Feedback", feedback.getSubject(), feedback.getStatus().name(), feedback.getId()));
        }
        return items.stream()
                .filter(item -> item.dateTime() != null)
                .sorted(Comparator.comparing(CustomerInteractionItem::dateTime).reversed())
                .toList();
    }
}

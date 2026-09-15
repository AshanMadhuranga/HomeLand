package com.landhub.customer;

import com.landhub.auth.UserService;
import com.landhub.feedback.FeedbackService;
import com.landhub.inquiry.InquiryService;
import com.landhub.payment.PaymentService;
import com.landhub.review.ReviewService;
import com.landhub.booking.BookingService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/customer/profile")
public class CustomerProfileController {

    private final UserService userService;
    private final InquiryService inquiryService;
    private final BookingService bookingService;
    private final PaymentService paymentService;
    private final ReviewService reviewService;
    private final FeedbackService feedbackService;

    public CustomerProfileController(UserService userService, InquiryService inquiryService, BookingService bookingService,
                                     PaymentService paymentService, ReviewService reviewService, FeedbackService feedbackService) {
        this.userService = userService;
        this.inquiryService = inquiryService;
        this.bookingService = bookingService;
        this.paymentService = paymentService;
        this.reviewService = reviewService;
        this.feedbackService = feedbackService;
    }

    @GetMapping
    public String details(Authentication authentication, Model model) {
        String email = authentication.getName();
        model.addAttribute("customer", userService.findByEmail(email).orElseThrow());
        model.addAttribute("inquiryCount", inquiryService.findCustomerInquiries(email).size());
        model.addAttribute("bookingCount", bookingService.findCustomerBookings(email).size());
        model.addAttribute("paymentCount", paymentService.countCustomerPayments(email));
        model.addAttribute("reviewCount", reviewService.countCustomerReviews(email));
        model.addAttribute("feedbackCount", feedbackService.countCustomerFeedback(email));
        return "customer/profile/details";
    }

    @GetMapping("/edit")
    public String edit(Authentication authentication, Model model) {
        model.addAttribute("customer", userService.findByEmail(authentication.getName()).orElseThrow());
        return "customer/profile/edit";
    }

    @PostMapping("/update")
    public String update(@RequestParam String firstName,
                         @RequestParam String lastName,
                         @RequestParam String phone,
                         Authentication authentication,
                         RedirectAttributes redirectAttributes,
                         Model model) {
        try {
            userService.updateCustomerProfile(authentication.getName(), firstName, lastName, phone);
            redirectAttributes.addFlashAttribute("successMessage", "Profile updated.");
            return "redirect:/customer/profile";
        } catch (IllegalArgumentException exception) {
            model.addAttribute("errorMessage", exception.getMessage());
            model.addAttribute("customer", userService.findByEmail(authentication.getName()).orElseThrow());
            return "customer/profile/edit";
        }
    }
}

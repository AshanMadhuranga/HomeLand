package com.landhub.customer;

import com.landhub.auth.Role;
import com.landhub.auth.User;
import com.landhub.auth.UserService;
import com.landhub.booking.BookingService;
import com.landhub.feedback.FeedbackService;
import com.landhub.inquiry.InquiryService;
import com.landhub.payment.PaymentService;
import com.landhub.review.ReviewService;
import com.landhub.sitevisit.SiteVisitService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/customers")
public class AdminCustomerController {

    private final UserService userService;
    private final CustomerManagementService customerManagementService;
    private final InquiryService inquiryService;
    private final SiteVisitService siteVisitService;
    private final BookingService bookingService;
    private final PaymentService paymentService;
    private final ReviewService reviewService;
    private final FeedbackService feedbackService;

    public AdminCustomerController(UserService userService,
                                   CustomerManagementService customerManagementService,
                                   InquiryService inquiryService,
                                   SiteVisitService siteVisitService,
                                   BookingService bookingService,
                                   PaymentService paymentService,
                                   ReviewService reviewService,
                                   FeedbackService feedbackService) {
        this.userService = userService;
        this.customerManagementService = customerManagementService;
        this.inquiryService = inquiryService;
        this.siteVisitService = siteVisitService;
        this.bookingService = bookingService;
        this.paymentService = paymentService;
        this.reviewService = reviewService;
        this.feedbackService = feedbackService;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("customers", customerManagementService.findCustomers());
        model.addAttribute("crmService", customerManagementService);
        return "admin/customers/list";
    }

    @GetMapping("/{id}")
    public String details(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        return userService.findById(id)
                .filter(user -> user.getRole() == Role.CUSTOMER)
                .map(customer -> {
                    String email = customer.getEmail();
                    model.addAttribute("customer", customer);
                    model.addAttribute("summary", customerManagementService.summary(customer));
                    model.addAttribute("inquiries", inquiryService.findCustomerInquiries(email));
                    model.addAttribute("siteVisits", siteVisitService.findCustomerSiteVisits(email));
                    model.addAttribute("bookings", bookingService.findCustomerBookings(email));
                    model.addAttribute("payments", paymentService.findCustomerPayments(email));
                    model.addAttribute("reviews", reviewService.findCustomerReviews(email));
                    model.addAttribute("feedbackItems", feedbackService.findCustomerFeedback(email));
                    model.addAttribute("timeline", customerManagementService.timeline(email));
                    return "admin/customers/details";
                })
                .orElseGet(() -> {
                    redirectAttributes.addFlashAttribute("errorMessage", "Customer was not found.");
                    return "redirect:/admin/customers";
                });
    }

    @PostMapping("/{id}/enable")
    public String enable(@PathVariable Long id, Authentication authentication, RedirectAttributes redirectAttributes) {
        return setEnabled(id, true, authentication, redirectAttributes);
    }

    @PostMapping("/{id}/disable")
    public String disable(@PathVariable Long id, Authentication authentication, RedirectAttributes redirectAttributes) {
        return setEnabled(id, false, authentication, redirectAttributes);
    }

    private String setEnabled(Long id, boolean enabled, Authentication authentication, RedirectAttributes redirectAttributes) {
        if (!hasAdminRole(authentication)) {
            redirectAttributes.addFlashAttribute("errorMessage", "Only admins can change customer account status.");
            return "redirect:/admin/customers/" + id;
        }
        try {
            userService.setCustomerEnabled(id, enabled);
            redirectAttributes.addFlashAttribute("successMessage", enabled ? "Customer enabled." : "Customer disabled.");
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return "redirect:/admin/customers/" + id;
    }

    private boolean hasAdminRole(Authentication authentication) {
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
    }
}

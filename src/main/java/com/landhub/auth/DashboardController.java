package com.landhub.auth;

import com.landhub.land.LandService;
import com.landhub.land.LandStatus;
import com.landhub.booking.BookingService;
import com.landhub.inquiry.InquiryService;
import com.landhub.sitevisit.SiteVisitService;
import com.landhub.verification.VerificationService;
import com.landhub.verification.VerificationStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DashboardController {

    private final LandService landService;
    private final VerificationService verificationService;
    private final InquiryService inquiryService;
    private final SiteVisitService siteVisitService;
    private final BookingService bookingService;

    public DashboardController(LandService landService,
                               VerificationService verificationService,
                               InquiryService inquiryService,
                               SiteVisitService siteVisitService,
                               BookingService bookingService) {
        this.landService = landService;
        this.verificationService = verificationService;
        this.inquiryService = inquiryService;
        this.siteVisitService = siteVisitService;
        this.bookingService = bookingService;
    }

    @GetMapping("/customer/dashboard")
    public String customerDashboard(Authentication authentication, Model model) {
        String email = authentication.getName();
        model.addAttribute("customerInquiryCount", inquiryService.findCustomerInquiries(email).size());
        model.addAttribute("customerSiteVisitCount", siteVisitService.findCustomerSiteVisits(email).size());
        model.addAttribute("customerBookingCount", bookingService.findCustomerBookings(email).size());
        return "customer/dashboard";
    }

    @GetMapping("/admin/dashboard")
    public String adminDashboard(Model model) {
        model.addAttribute("totalLands", landService.countAll());
        model.addAttribute("availableLands", landService.countByPublicStatus(LandStatus.AVAILABLE));
        model.addAttribute("pendingVerificationCount", verificationService.countByStatus(VerificationStatus.PENDING));
        model.addAttribute("openInquiryCount", inquiryService.countOpen());
        model.addAttribute("pendingSiteVisitCount", siteVisitService.countPending());
        model.addAttribute("pendingBookingCount", bookingService.countPending());
        return "admin/dashboard";
    }

    @GetMapping("/403")
    public String forbidden() {
        return "error/403";
    }
}

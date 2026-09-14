package com.landhub.auth;

import com.landhub.land.LandService;
import com.landhub.land.LandStatus;
import com.landhub.verification.VerificationService;
import com.landhub.verification.VerificationStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DashboardController {

    private final LandService landService;
    private final VerificationService verificationService;

    public DashboardController(LandService landService, VerificationService verificationService) {
        this.landService = landService;
        this.verificationService = verificationService;
    }

    @GetMapping("/customer/dashboard")
    public String customerDashboard() {
        return "customer/dashboard";
    }

    @GetMapping("/admin/dashboard")
    public String adminDashboard(Model model) {
        model.addAttribute("totalLands", landService.countAll());
        model.addAttribute("availableLands", landService.countByPublicStatus(LandStatus.AVAILABLE));
        model.addAttribute("pendingVerificationCount", verificationService.countByStatus(VerificationStatus.PENDING));
        return "admin/dashboard";
    }

    @GetMapping("/403")
    public String forbidden() {
        return "error/403";
    }
}
